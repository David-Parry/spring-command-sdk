/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.service;

import ai.qodo.command.internal.api.TaskResponse;

import java.net.http.WebSocket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Context object to track WebSocket session state and responses
 */
public class SessionContext {
    
    private final String sessionId;
    private final String requestId;
    private final Instant createdAt;
    private final CountDownLatch completionLatch;
    private final AtomicBoolean completed;
    
    private volatile WebSocket webSocket;
    private volatile TaskResponse userResponse;
    private volatile Instant lastActivityAt;
    
    private final List<TaskResponse> responses;
    private final List<String> errors;
    
    public SessionContext(String sessionId, String requestId) {
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.createdAt = Instant.now();
        this.lastActivityAt = Instant.now();
        this.completionLatch = new CountDownLatch(1);
        this.completed = new AtomicBoolean(false);
        this.responses = Collections.synchronizedList(new ArrayList<>());
        this.errors = Collections.synchronizedList(new ArrayList<>());
    }
    
    // Getters
    public String getSessionId() {
        return sessionId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getLastActivityAt() {
        return lastActivityAt;
    }
    
    public WebSocket getWebSocket() {
        return webSocket;
    }
    
    public TaskResponse getUserResponse() {
        return userResponse;
    }
    
    public CountDownLatch getCompletionLatch() {
        return completionLatch;
    }
    
    public boolean isCompleted() {
        return completed.get();
    }
    
    public List<TaskResponse> getResponses() {
        return new ArrayList<>(responses);
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    // Setters
    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
        updateLastActivity();
    }
    
    public void setUserResponse(TaskResponse userResponse) {
        this.userResponse = userResponse;
        updateLastActivity();
    }
    
    public void setCompleted(boolean completed) {
        this.completed.set(completed);
        updateLastActivity();
    }
    
    // Operations
    public void addResponse(TaskResponse response) {
        responses.add(response);
        updateLastActivity();
    }
    
    public void addError(String error) {
        errors.add(error);
        updateLastActivity();
    }
    
    private void updateLastActivity() {
        this.lastActivityAt = Instant.now();
    }
    
    // Utility methods
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public int getResponseCount() {
        return responses.size();
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    @Override
    public String toString() {
        return "SessionContext{" +
                "sessionId='" + sessionId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", createdAt=" + createdAt +
                ", lastActivityAt=" + lastActivityAt +
                ", completed=" + completed.get() +
                ", responseCount=" + responses.size() +
                ", errorCount=" + errors.size() +
                '}';
    }
}