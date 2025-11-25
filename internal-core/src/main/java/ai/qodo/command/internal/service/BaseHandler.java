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
import ai.qodo.command.internal.pojo.ServerRawResponses;
import ai.qodo.command.internal.util.DirectoryPrinter;
import ai.qodo.command.internal.util.WebSocketUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static ai.qodo.command.internal.service.WebSocketNotificationService.TYPE_STRUCTURED_OUTPUT;

public abstract class BaseHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(BaseHandler.class);
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;
    private final int MAX_MSG_SIZE_BYTES = 104857600;

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
        String requestId = commandSession.requestId();
        String checkPointId = commandSession.checkPointId();
        Map<String, Object> map = new HashMap<>();
        map.put(StringConstants.EVENT_KEY.getValue(), eventKey);
        map.put(StringConstants.SESSION_ID.getValue(), sessionId);
        map.put(StringConstants.PROJECT_STRUCTURE.getValue(), stringSessionDirectory(sessionId));
        map.put(StringConstants.REQUEST_ID.getValue(), requestId);
        map.put(StringConstants.CHECKPOINT_ID.getValue(), checkPointId);
        map.put(StringConstants.MESSAGE_TYP.getValue(), commandSession.messageType());

        ServerRawResponses serverRawResponses = WebSocketUtil.parseTaskResponses(allTaskResponses);

        String msg = serverRawResponses.structuredJson();
        // now remove the directory next agent will have its own session and new directory
        removeSessionDirectory(sessionId);
        try {
            logger.debug("Message to publish marking with either JIRA_BUG_ACTIONABLE or TYPE_STRUCTURED_OUTPUT: {}",
                         msg);
            map.putAll(objectMapper.readValue(msg, Map.class));
            // ask the handler to give the type also has the way to alter the Map for success in the handle too or
            // add to it before getting put on to the Queue
            if (map.containsKey(StringConstants.SUCCESS.getValue()) && (map.get(StringConstants.SUCCESS.getValue()) instanceof Boolean) && (Boolean) map.get(StringConstants.SUCCESS.getValue())) {
                map.put(StringConstants.TYPE.getValue(), type());
            } else {
                map.put(StringConstants.TYPE.getValue(), EndFlowCleanup.TYPE);
                if (logger.isDebugEnabled()) {
                    logger.debug("Response from server session {} reporting success as {} if you want to see the LLM " +
                                         "conversation and response turn trace on for BaseHandler class", sessionId,
                                 map.get(StringConstants.SUCCESS.getValue()));
                } else if (logger.isTraceEnabled()) {
                    logger.trace("""
                                         Response from server session {} did not report success was {}\s
                                          structured \
                                         response: {}\s
                                          unstructured conversation: {}""", sessionId,
                                 map.get(StringConstants.SUCCESS.getValue()), serverRawResponses.structuredJson(),
                                 serverRawResponses.unstructuredJson());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse structuredJson placing back on Queue to retry to Map from string value {}",
                         msg, e);
            logger.debug("Conversation from LLM before that did not give us a {} variable. {}",
                         TYPE_STRUCTURED_OUTPUT, serverRawResponses.unstructuredJson());
            // there was a failure to read the contract of the output schema that the LLM was suppose to return no
            // way to determine if it was success so go to a incomplete service if developer wants too
            map.put(StringConstants.TYPE.getValue(), MessageService.INCOMPLETE_NODE_SERVICE);
            map.put(StringConstants.LLM_CONVERSATION.getValue(), serverRawResponses.unstructuredJson());
        }
        this.messagePublisher.publishResponse(serializeMapForQueue(handle(map)));

    }

    /**
     * no matter what we will get a next step and a service to call if developer wants always
     *
     * @param map
     * @return - at least a string representation of a map simplest with failures will be one value
     */
    protected String serializeMapForQueue(Map map) {
        String msg;
        try {
            msg = objectMapper.writeValueAsString(map);
            if (msg.length() > MAX_MSG_SIZE_BYTES) {
                map.remove(StringConstants.LLM_CONVERSATION.getValue());
                msg = objectMapper.writeValueAsString(map);
            }
        } catch (Exception er) {
            logger.error("Something went very wrong with the map to message for QUEUE {}", map, er);
            msg = "{\"" + StringConstants.TYPE.getValue() + "\":\"" + MessageService.INCOMPLETE_NODE_SERVICE + "\"}";
        }
        return msg;
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
