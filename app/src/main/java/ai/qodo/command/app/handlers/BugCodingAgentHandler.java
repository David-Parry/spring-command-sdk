/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.handlers;

import ai.qodo.command.internal.service.BaseHandler;
import ai.qodo.command.internal.service.EndFlowCleanup;
import ai.qodo.command.internal.service.MessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("bug_coding_agent-handler")
@Scope("prototype")
public class BugCodingAgentHandler extends BaseHandler {

    private static final Logger logger = LoggerFactory.getLogger(BugCodingAgentHandler.class);

    public BugCodingAgentHandler(MessagePublisher messagePublisher, ObjectMapper objectMapper) {
        super(messagePublisher, objectMapper);
    }

    @Override
    public String type() {
        return EndFlowCleanup.TYPE;
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> map) {
        logger.debug("nothing to do but pass through {}", map);
        return map;
    }


}
