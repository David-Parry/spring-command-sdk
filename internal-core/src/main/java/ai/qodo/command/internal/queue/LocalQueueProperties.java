/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.queue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the local queue implementation.
 * These properties control the behavior of the in-memory queue system.
 */
@Component
@ConfigurationProperties(prefix = "messaging.local")
@ConditionalOnProperty(name = "messaging.provider", havingValue = "local")
public class LocalQueueProperties {
    
    /**
     * Maximum capacity for each queue.
     * Default: 1000 messages
     */
    private int queueCapacity = 1000;
    
    /**
     * Number of consumer threads per queue.
     * Default: 1 (ensures message ordering)
     */
    private int consumerThreads = 1;
    
    /**
     * Maximum number of retry attempts for failed messages.
     * Default: 3
     */
    private int retryAttempts = 3;
    
    /**
     * Initial delay in milliseconds before first retry.
     * Default: 1000ms (1 second)
     */
    private long retryDelayMs = 1000;
    
    /**
     * Maximum delay in milliseconds between retries (with exponential backoff).
     * Default: 30000ms (30 seconds)
     */
    private long maxRetryDelayMs = 30000;
    
    /**
     * Timeout in seconds for polling messages from the queue.
     * Default: 5 seconds
     */
    private long pollTimeoutSeconds = 5;
    
    /**
     * Whether to use exponential backoff for retries.
     * Default: true
     */
    private boolean exponentialBackoff = true;
    
    public int getQueueCapacity() {
        return queueCapacity;
    }
    
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
    
    public int getConsumerThreads() {
        return consumerThreads;
    }
    
    public void setConsumerThreads(int consumerThreads) {
        this.consumerThreads = consumerThreads;
    }
    
    public int getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    public long getRetryDelayMs() {
        return retryDelayMs;
    }
    
    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
    
    public long getMaxRetryDelayMs() {
        return maxRetryDelayMs;
    }
    
    public void setMaxRetryDelayMs(long maxRetryDelayMs) {
        this.maxRetryDelayMs = maxRetryDelayMs;
    }
    
    public long getPollTimeoutSeconds() {
        return pollTimeoutSeconds;
    }
    
    public void setPollTimeoutSeconds(long pollTimeoutSeconds) {
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }
    
    public boolean isExponentialBackoff() {
        return exponentialBackoff;
    }
    
    public void setExponentialBackoff(boolean exponentialBackoff) {
        this.exponentialBackoff = exponentialBackoff;
    }
}
