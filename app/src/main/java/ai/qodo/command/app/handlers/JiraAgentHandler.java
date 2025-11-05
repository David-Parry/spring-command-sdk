/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.handlers;
import ai.qodo.command.internal.api.Handler;

import ai.qodo.command.internal.service.BaseHandler;
import ai.qodo.command.internal.service.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;

import static ai.qodo.command.internal.api.Handler.HANDLER_SUFFIX;

@Service("jira_agent" + HANDLER_SUFFIX)
@Scope("prototype")
public class JiraAgentHandler extends BaseHandler {

    public static final String JIRA_BUG_ACTIONABLE = "bug_coding_agent";
    private static final Logger logger = LoggerFactory.getLogger(JiraAgentHandler.class);

    public JiraAgentHandler(MessagePublisher messagePublisher, ObjectMapper objectMapper) {
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
