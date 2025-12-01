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
public class CloudWatchLogWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(CloudWatchLogWebhookController.class);

    // Jira webhook header names
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_HUB_SIGNATURE = "X-Hub-Signature";
    private static final String RESPONSE_ERROR = "error";
    public static final String MSG_AWS_CLOUD = "cloudwatch_agent";
    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_MESSAGE = "message";
    private static final String RESPONSE_SERVICE = "service";
    private static final String RESPONSE_SIGNATURE_VALIDATION = "signatureValidation";
    private static final String RESPONSE_TIMESTAMP = "timestamp";

    private final ObjectMapper objectMapper;
    private final MessagePublisher messagePublisher;
    private final JiraWebhookValidator jiraWebhookValidator;

    @Value("${messaging.queue.event}")
    private String eventTopic;

    public CloudWatchLogWebhookController(JiraWebhookValidator jiraWebhookValidator, ObjectMapper objectMapper,
                                          MessagePublisher messagePublisher, JiraAgentProperties agentProperties) {
        this.objectMapper = objectMapper;
        this.messagePublisher = messagePublisher;
        this.jiraWebhookValidator = jiraWebhookValidator;
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
    @PostMapping("/aws/cloud")
    public ResponseEntity<?> handleJiraWebhook(
            @RequestHeader(value = HEADER_USER_AGENT, required = false) String userAgent,
            @RequestHeader(value = HEADER_CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = HEADER_HUB_SIGNATURE, required = false) String signature,
            @RequestBody String rawBody) {

        logger.info("Received Cloudwatch log - User-Agent: {}, Content-Type: {} body {}", userAgent, contentType,
                    rawBody);

        if (!jiraWebhookValidator.validateSignature(rawBody, signature)) {
            logger.error("Webhook signature validation failed - Cloudwatch log error: {}", rawBody);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(RESPONSE_ERROR, "Invalid signature"));
        }

        try {
            // Parse the JSON payload
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);

            // Prepare message payload
            Map<String, Object> messagePayload = new HashMap<>(payload);
            messagePayload.put(MSG_TYPE, MSG_AWS_CLOUD);

            // Publish message
            String msgQueue = objectMapper.writeValueAsString(messagePayload);
            messagePublisher.publish(eventTopic, msgQueue);

            logger.info("Successfully processed AWS Log webhook log size {}", msgQueue.length());

        } catch (Exception e) {
            logger.error("Error processing AWS Cloudwatch log webhook", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(RESPONSE_ERROR, "Internal server error"));
        }

        return ResponseEntity.ok(Map.of(RESPONSE_STATUS, "success", RESPONSE_MESSAGE, "Webhook processed " +
                "successfully"));
    }

    /**
     * Health check endpoint for Jira webhook service
     */
    @GetMapping("/aws/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(RESPONSE_STATUS, "healthy", RESPONSE_SERVICE, "AWS Log Webhook Handler",
                                        RESPONSE_SIGNATURE_VALIDATION, true, RESPONSE_TIMESTAMP, Instant
                .now()
                .toString()));
    }

}