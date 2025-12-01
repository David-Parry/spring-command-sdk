/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.handlers;

import ai.qodo.command.internal.service.BaseHandler;
import ai.qodo.command.internal.service.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;

import static ai.qodo.command.app.controllers.CloudWatchLogWebhookController.MSG_AWS_CLOUD;
import static ai.qodo.command.internal.api.Handler.HANDLER_SUFFIX;

@Service(MSG_AWS_CLOUD + HANDLER_SUFFIX)
@Scope("prototype")
public class CloudWatchAgentHandler extends BaseHandler {

    public static final String JIRA_BUG_ACTIONABLE = "coding_agent";
    private static final Logger logger = LoggerFactory.getLogger(CloudWatchAgentHandler.class);

    public CloudWatchAgentHandler(MessagePublisher messagePublisher, ObjectMapper objectMapper) {
        super(messagePublisher, objectMapper);
    }

    @Override
    public String type() {
        return JIRA_BUG_ACTIONABLE;
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> map) {
        logger.debug("Nothing to do but put on the Queue {}", map);
        return map;
    }


}
