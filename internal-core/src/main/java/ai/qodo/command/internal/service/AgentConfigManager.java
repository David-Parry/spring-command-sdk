/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.service;

import ai.qodo.command.internal.mcp.AgentConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manager component that loads and caches the agent configuration
 */
@Component
public class AgentConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(AgentConfigManager.class);

    private final AgentConfigService agentConfigService;
    private AgentConfig agentConfig;

    @Autowired
    public AgentConfigManager(AgentConfigService agentConfigService) {
        this.agentConfigService = agentConfigService;
    }

    /**
     * Loads the agent configuration after dependency injection is complete.
     * This ensures the configuration is available for other components during startup.
     */
    @PostConstruct
    public void loadAgentConfiguration() {
        String configFilePath = agentConfigService.getConfigFilePath();
        logger.info("Loading agent configuration from: {}", configFilePath);

        try {
            this.agentConfig = agentConfigService.loadAgentConfig();
            logger.info("Successfully loaded agent configuration with version: {} and {} commands",
                        agentConfig.version(), agentConfig
                    .commands()
                    .size());
        } catch (Exception e) {
            logger.error("Failed to load agent configuration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load agent configuration, Cannot continue till agents are valid", e);
        }
    }


    /**
     * Gets the loaded agent configuration
     *
     * @return the loaded AgentConfig, or null if not yet loaded or failed to load
     */
    public AgentConfig getAgentConfig() {
        return agentConfig;
    }


}