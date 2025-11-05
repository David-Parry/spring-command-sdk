/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.service;

import ai.qodo.command.internal.api.Handler;
import ai.qodo.command.internal.api.StringConstants;
import ai.qodo.command.internal.api.TaskResponse;
import ai.qodo.command.internal.pojo.CommandSession;
import ai.qodo.command.internal.util.DirectoryPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static ai.qodo.command.internal.service.WebSocketNotificationService.TYPE_STRUCTURED_OUTPUT;

public abstract class BaseHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(BaseHandler.class);
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;

    public BaseHandler(MessagePublisher messagePublisher, ObjectMapper objectMapper) {
        this.messagePublisher = messagePublisher;
        this.objectMapper = objectMapper;
    }

    public abstract String type();

    public abstract Map<String, Object> handle(Map<String, Object> map);


    @Override
    public void handle(CommandSession commandSession, List<TaskResponse> allTaskResponses) {
        String eventKey = commandSession.eventKey();
        String sessionId = commandSession.sessionId();
        StringBuilder structuredJson = new StringBuilder();
        for (TaskResponse r : allTaskResponses) {
            if (r.type().equalsIgnoreCase(TYPE_STRUCTURED_OUTPUT)) {
                for (Object ok : r.data().toolArgs().values()) {
                    structuredJson.append(ok);
                }
            }
        }
        String msg = structuredJson.toString();
        try {
            logger.debug("Message to publish marking with either JIRA_BUG_ACTIONABLE or TYPE_STRUCTURED_OUTPUT: {}",
                         msg);
            Map<String, Object> map = objectMapper.readValue(msg, Map.class);
            map.put(StringConstants.EVENT_KEY.getValue(), eventKey);
            map.put(StringConstants.SESSION_ID.getValue(), sessionId);
            map.put(StringConstants.PROJECT_STRUCTURE.getValue(), stringSessionDirectory(sessionId));
            // ask the handler to give the type also has the way to alter the Map for success in the handle too or
            // add to it before getting put on to the Queue
            if (map.containsKey(StringConstants.SUCCESS.getValue()) &&
                    (map.get(StringConstants.SUCCESS.getValue()) instanceof Boolean) &&
                    (Boolean) map.get(StringConstants.SUCCESS.getValue())) {
                map.put(StringConstants.TYPE.getValue(), type());
            } else {
                map.put(StringConstants.TYPE.getValue(), EndFlowCleanup.TYPE);
            }
            this.messagePublisher.publishResponse(objectMapper.writeValueAsString(handle(map)));
        } catch (Exception e) {
            logger.error("Failed to parse structuredJson placing back on Queue to retry to Map from string value {}", msg, e);
            throw new CompletionException("The message placed retrieved did not parse to Map from string value "+  msg, e);
        } finally {
            removeSessionDirectory(sessionId);
        }

    }

    private String stringSessionDirectory(String sessionId) {
        String stringDirectory = sessionId + "/";
        try {
            Path path = Path.of(getSessionAbsolutePath(sessionId));
            stringDirectory = DirectoryPrinter.buildDirectoryTree(path);
        } catch (Exception e) {
            logger.error("failed to build directory tree for sessionId {}", sessionId, e);
        }
        return stringDirectory;
    }

    private String getSessionAbsolutePath(String sessionId) {
        return System.getProperty(StringConstants.USER_HOME.getValue()) + "/" + sessionId + "/";
    }

    public void removeSessionDirectory(String sessionId) {
        String directoryPath = getSessionAbsolutePath(sessionId);
        Path directory = Paths.get(directoryPath);
        logger.debug("Removing session base directory {}", directory);

        try {
            if (Files.exists(directory)) {
                try (Stream<Path> walk = Files.walk(directory)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.error("Failed to delete: {} ", path, e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Failed to remove directory: {} ", directoryPath, e);
        }
    }

}
