/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.controllers;

import ai.qodo.command.app.config.JiraAgentProperties;
import ai.qodo.command.internal.service.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ai.qodo.command.internal.service.MessagePublisher.MSG_TYPE;

/**
 * Spring Boot WebMVC Controller to handle Jira webhook calls.
 * Jira webhooks are triggered by various events like issue creation, updates, etc.
 * The issue key is extracted dynamically from the webhook payload.
 * <p>
 * Configuration required in application.properties:
 * - jira.webhook.secret: Your webhook secret for validation (optional)
 * - jira.webhook.validate-signature: Enable/disable signature validation (default: false)
 */
@RestController
@RequestMapping("/api/webhooks")
public class JiraWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(JiraWebhookController.class);

    // Jira webhook header names
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_HUB_SIGNATURE = "X-Hub-Signature";
    
    // JSON path constants - Payload structure
    private static final String ISSUE = "issue";
    private static final String ISSUE_KEY = "key";
    private static final String WEBHOOK_EVENT = "webhookEvent";
    private static final String TIMESTAMP = "timestamp";
    private static final String USER = "user";
    private static final String ACCOUNT_ID = "accountId";
    private static final String CHANGELOG = "changelog";
    
    // Message payload keys
    private static final String MSG_JIRA = "jira_agent";
    private static final String ISSUE_KEY_FIELD = "issueKey";
    private static final String TRIGGERED_BY = "triggeredBy";
    private static final String EVENT_KEY = "EventKey";
    
    // Response keys
    private static final String RESPONSE_ERROR = "error";
    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_MESSAGE = "message";
    private static final String RESPONSE_REASON = "reason";
    private static final String RESPONSE_SERVICE = "service";
    private static final String RESPONSE_SIGNATURE_VALIDATION = "signatureValidation";
    private static final String RESPONSE_TIMESTAMP = "timestamp";

    private final ObjectMapper objectMapper;
    private final MessagePublisher messagePublisher;
    private final JiraWebhookValidator jiraWebhookValidator;
    private final JiraAgentProperties agentProperties;

    @Value("${jira.webhook.max-timestamp-age-seconds:300}")
    private long maxTimestampAgeSeconds;

    @Value("${messaging.queue.event}")
    private String eventTopic;

    public JiraWebhookController(JiraWebhookValidator jiraWebhookValidator, 
                                 ObjectMapper objectMapper,
                                 MessagePublisher messagePublisher,
                                 JiraAgentProperties agentProperties) {
        this.objectMapper = objectMapper;
        this.messagePublisher = messagePublisher;
        this.jiraWebhookValidator = jiraWebhookValidator;
        this.agentProperties = agentProperties;
    }

    /**
     * Endpoint to receive Jira webhook events.
     * Extracts the issue key dynamically from the webhook payload.
     *
     * @param userAgent   User agent from User-Agent header
     * @param contentType Content type from Content-Type header
     * @param rawBody     Raw request body as string
     * @return ResponseEntity with appropriate status
     */
    @PostMapping("/jira/{key}")
    public ResponseEntity<?> handleJiraWebhook(
            @RequestHeader(value = HEADER_USER_AGENT, required = false) String userAgent,
            @RequestHeader(value = HEADER_CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = HEADER_HUB_SIGNATURE, required = false) String signature, @PathVariable String key,
            @RequestBody String rawBody) {

        logger.info("Received Jira webhook Issue Key {} - User-Agent: {}, Content-Type: {} body {}", key, userAgent,
                    contentType, rawBody);

        if (!jiraWebhookValidator.validateSignature(rawBody, signature)) {
            logger.error("Webhook signature validation failed - Jira issue key: {}", key);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(RESPONSE_ERROR, "Invalid signature"));
        }

        try {
            // Parse the JSON payload
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            
            // Extract user accountId
            String accountId = extractUserAccountId(payload);
            
            // Check if triggered by agent and blocking is enabled
            if (isAgentTriggered(accountId)) {
                logger.info("Webhook triggered by configured agent (accountId: {}), blocking to prevent loop", 
                           accountId);
                
                // Return success to prevent Jira from retrying
                return ResponseEntity.ok(Map.of(
                    RESPONSE_STATUS, "skipped",
                    RESPONSE_MESSAGE, "Agent-triggered webhook blocked",
                    RESPONSE_REASON, "loop-prevention"
                ));
            }

            // Extract issue key dynamically
            String issueKey = extractIssueKey(payload, key);
            if (issueKey == null) {
                logger.warn("Could not extract issue key from Jira webhook payload");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(RESPONSE_ERROR, "Issue key not found in payload"));
            }

            // Extract webhook event type
            String webhookEvent = (String) payload.get(WEBHOOK_EVENT);
            Long timestamp = (Long) payload.get(TIMESTAMP);

            logger.info("Processing Jira webhook - Issue Key: {}, Event: {}, User: {}", 
                       issueKey, webhookEvent, accountId);

            // Prepare message payload
            Map<String, Object> messagePayload = new HashMap<>(payload);
            messagePayload.put(ISSUE_KEY_FIELD, issueKey);
            messagePayload.put(MSG_TYPE, MSG_JIRA);
            messagePayload.put(TRIGGERED_BY, accountId);  // Include who triggered
            messagePayload.put(EVENT_KEY, String.format("%s_%s_%s", webhookEvent != null ? webhookEvent : "unknown",
                                                         issueKey, timestamp != null ? timestamp :
                                                                 System.currentTimeMillis()));

            // Publish message
            String msgQueue = objectMapper.writeValueAsString(messagePayload);
            messagePublisher.publish(eventTopic, msgQueue);

            logger.info("Successfully processed Jira webhook for issue: {}", issueKey);

        } catch (Exception e) {
            logger.error("Error processing Jira webhook", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(RESPONSE_ERROR, "Internal server error"));
        }

        return ResponseEntity.ok(Map.of(RESPONSE_STATUS, "success", RESPONSE_MESSAGE, "Webhook processed successfully"));
    }

    /**
     * Extracts user accountId from webhook payload.
     * The user field represents who triggered the webhook event.
     * 
     * @param payload The webhook payload
     * @return The accountId, or null if not found
     */
    private String extractUserAccountId(Map<String, Object> payload) {
        try {
            Object userObj = payload.get(USER);
            if (userObj instanceof Map) {
                Map<String, Object> user = (Map<String, Object>) userObj;
                return (String) user.get(ACCOUNT_ID);
            }
        } catch (Exception e) {
            logger.error("Error extracting user accountId from payload", e);
        }
        
        return null;
    }
    
    /**
     * Checks if the webhook was triggered by the configured agent.
     * Only checks accountId to identify agent-triggered webhooks.
     * 
     * @param accountId The accountId from the webhook payload
     * @return true if triggered by agent, false otherwise
     */
    private boolean isAgentTriggered(String accountId) {
        if (!agentProperties.isBlockEnabled()) {
            logger.info("Blocking is disabled {}", agentProperties);
            return false;  // Blocking disabled
        }
        
        if (accountId == null || accountId.isEmpty()) {
            logger.warn("accountId is missing from the payload {}", accountId);
            return false;  // No accountId in payload
        }
        
        // Check accountId match only
        return agentProperties.getAccountId() != null &&
               !agentProperties.getAccountId().isEmpty() &&
               agentProperties.getAccountId().equals(accountId);
    }
    
    /**
     * Extracts the issue key from the Jira webhook payload.
     * The issue key can be found at different paths depending on the webhook event type.
     *
     * @param payload The webhook payload
     * @return The issue key or null if not found
     */
    private String extractIssueKey(Map<String, Object> payload, String issueKey) {
        try {
            if (issueKey != null && !issueKey.isBlank()) {
                return issueKey;
            }
            // Try to get issue key from issue object (most common case)
            Object issueObj = payload.get(ISSUE);
            if (issueObj instanceof Map) {
                Map<String, Object> issue = (Map<String, Object>) issueObj;
                Object key = issue.get(ISSUE_KEY);
                if (key instanceof String) {
                    return (String) key;
                }
            }

            // Try to get issue key directly from root level (some webhook types)
            Object directKey = payload.get(ISSUE_KEY);
            if (directKey instanceof String) {
                return (String) directKey;
            }

            // Try to get from nested structures (for complex webhook payloads)
            if (payload.containsKey(CHANGELOG)) {
                Object changelogObj = payload.get(CHANGELOG);
                if (changelogObj instanceof Map) {
                    Map<String, Object> changelog = (Map<String, Object>) changelogObj;
                    if (changelog.containsKey(ISSUE)) {
                        Object nestedIssue = changelog.get(ISSUE);
                        if (nestedIssue instanceof Map) {
                            Map<String, Object> issue = (Map<String, Object>) nestedIssue;
                            Object key = issue.get(ISSUE_KEY);
                            if (key instanceof String) {
                                return (String) key;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error extracting issue key from payload", e);
        }

        return null;
    }

    /**
     * Health check endpoint for Jira webhook service
     */
    @GetMapping("/jira/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                RESPONSE_STATUS, "healthy", 
                RESPONSE_SERVICE, "Jira Webhook Handler", 
                RESPONSE_SIGNATURE_VALIDATION, true, 
                RESPONSE_TIMESTAMP, Instant.now().toString()));
    }

}