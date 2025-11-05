/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for webhook settings.
 * Provides type-safe configuration for Snyk and Jira webhook settings.
 */
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {
    
    private final Snyk snyk = new Snyk();
    private final Jira jira = new Jira();
    
    public Snyk getSnyk() {
        return snyk;
    }
    
    public Jira getJira() {
        return jira;
    }
    
    public static class Snyk {
        private String secret = "";
        private boolean validateSignature = false;
        private long maxTimestampAgeSeconds = -1;
        private boolean notificationsEnabled = true;
        
        public String getSecret() {
            return secret;
        }
        
        public void setSecret(String secret) {
            this.secret = secret;
        }
        
        public boolean isValidateSignature() {
            return validateSignature;
        }
        
        public void setValidateSignature(boolean validateSignature) {
            this.validateSignature = validateSignature;
        }
        
        public long getMaxTimestampAgeSeconds() {
            return maxTimestampAgeSeconds;
        }
        
        public void setMaxTimestampAgeSeconds(long maxTimestampAgeSeconds) {
            this.maxTimestampAgeSeconds = maxTimestampAgeSeconds;
        }
        
        public boolean isNotificationsEnabled() {
            return notificationsEnabled;
        }
        
        public void setNotificationsEnabled(boolean notificationsEnabled) {
            this.notificationsEnabled = notificationsEnabled;
        }
    }
    
    public static class Jira {
        private String secret = "";
        private boolean validateSignature = false;
        private long maxTimestampAgeSeconds = -1;
        
        public String getSecret() {
            return secret;
        }
        
        public void setSecret(String secret) {
            this.secret = secret;
        }
        
        public boolean isValidateSignature() {
            return validateSignature;
        }
        
        public void setValidateSignature(boolean validateSignature) {
            this.validateSignature = validateSignature;
        }
        
        public long getMaxTimestampAgeSeconds() {
            return maxTimestampAgeSeconds;
        }
        
        public void setMaxTimestampAgeSeconds(long maxTimestampAgeSeconds) {
            this.maxTimestampAgeSeconds = maxTimestampAgeSeconds;
        }
    }
}
