/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class JiraWebhookValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(JiraWebhookValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256_PREFIX = "sha256=";
    
    @Value("${jira.webhook.secret}")
    private String webhookSecret;

    @Value("${jira.webhook.validate-signature}")
    private boolean validateSignature;
    /**
     * Validates the Snyk webhook signature using HMAC-SHA256
     * 
     * @param payload The raw webhook payload
     * @param signature The X-Snyk-Signature header value
     * @return true if signature is valid, false otherwise
     */
    public boolean validateSignature(String payload, String signature) {
        if (!validateSignature) {
            logger.warn("Webhook signature validation is disabled");
            return true;
        }
        
        if (!StringUtils.hasText(webhookSecret)) {
            logger.error("Webhook secret is not configured");
            return false;
        }
        
        if (!StringUtils.hasText(signature)) {
            logger.error("No signature provided in webhook request");
            return false;
        }
        
        if (!StringUtils.hasText(payload)) {
            logger.error("Empty payload provided for signature validation");
            return false;
        }
        
        try {
            String expectedSignature = generateSignature(payload, webhookSecret);
            boolean isValid = MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
            
            if (!isValid) {
                logger.warn("Invalid webhook signature. Expected: {}, Received: {}", 
                    expectedSignature, signature);
            } else {
                logger.debug("Webhook signature validation successful");
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating webhook signature", e);
            return false;
        }
    }
    
    /**
     * Generates HMAC-SHA256 signature for the given payload and secret
     * 
     * @param payload The payload to sign
     * @param secret The secret key
     * @return The signature in format "sha256=<hex>"
     * @throws NoSuchAlgorithmException if HMAC-SHA256 is not available
     * @throws InvalidKeyException if the secret key is invalid
     */
    public String generateSignature(String payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), 
            HMAC_SHA256
        );
        mac.init(secretKey);
        
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return SHA256_PREFIX + bytesToHex(hash);
    }
    
    /**
     * Converts byte array to hexadecimal string
     * 
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}