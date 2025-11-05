/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.service;
import ai.qodo.command.internal.mcp.AgentCommand;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Context object for WebSocket notification tracking
 */
public record NotificationContext(
    String sessionId,
    String eventKey,
    String requestId,
    JsonNode payload,
    AgentCommand command,
    Instant createdAt,
    int attemptCount
) {

    public static NotificationContext create(String sessionId, String eventKey, String requestId, 
                                             JsonNode payload, AgentCommand command) {
        return new NotificationContext(sessionId, eventKey, requestId, payload, command, Instant.now(), 0);
    }
    
    public NotificationContext withIncrementedAttempt() {
        return new NotificationContext(sessionId, eventKey, requestId, payload, command, createdAt, attemptCount + 1);
    }
    
    @Override
    public String toString() {
        return "NotificationContext{" +
                "sessionId='" + sessionId + '\'' +
                ", eventKey='" + eventKey + '\'' +
                ", requestId='" + requestId + '\'' +
                ", attemptCount=" + attemptCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
