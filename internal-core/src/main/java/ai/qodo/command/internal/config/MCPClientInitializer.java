/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.config;

import ai.qodo.command.internal.config.client.McpClientManager;
import ai.qodo.command.internal.config.client.ToolRegistry;
import ai.qodo.command.internal.mcp.*;
import ai.qodo.command.internal.service.AgentConfigManager;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * MCP Client Initializer that coordinates the loading of MCP configurations
 * and initialization of clients. This class now delegates specific responsibilities
 * to focused components:
 * - TransportFactory: Creates appropriate transports
 * - McpClientManager: Manages client lifecycle
 * - ToolRegistry: Manages tool mapping and filtering
 */
@Component
public class MCPClientInitializer {
    private static final Logger logger = LoggerFactory.getLogger(MCPClientInitializer.class);
    private final McpClientManager clientManager;
    private final ToolRegistry toolRegistry;
    private final AgentConfigManager agentConfigManager;

    @Autowired
    public MCPClientInitializer(McpClientManager clientManager,
                                ToolRegistry toolRegistry,
                                AgentConfigManager agentConfigManager) {
        this.clientManager = clientManager;
        this.toolRegistry = toolRegistry;
        this.agentConfigManager = agentConfigManager;
    }

    // Delegate methods to maintain backward compatibility

    public Map<ServerTool, McpSchema.Tool> getToolMap() {
        return toolRegistry.getToolMap();
    }

    public Map<String, McpServerInitialized> getClientsByName() {
        return clientManager.getClientsByName();
    }

    public Map<String, McpServer> getNoToolsClient() {
        return clientManager.getNoToolsClient();
    }

    public AgentMcpServers getAgentMcpServers(String commandName) {
        return clientManager.getAgentMcpServers(commandName);
    }

    public Map<String, AgentMcpServers> getAllAgentMcpServers() {
        return clientManager.getAllAgentMcpServers();
    }

    public List<String> getServerNames() {
        return clientManager.getServerNames();
    }

    public McpSyncClient createClient(String serverName, McpServer serverConfig, String fullyQualifiedName) {
        return clientManager.createClient(serverName, serverConfig, fullyQualifiedName);
    }

    public Map<String, McpSyncClient> loadSyncClients(McpConfig mcpConfig, String fullyQualifiedName) {
        Map<String, McpSyncClient> map = new HashMap<>();
        for(String key: mcpConfig.mcpServers().keySet()) {
            mcpConfig.mcpServers().get(key);
            map.put(key, createClient(key,mcpConfig.mcpServers().get(key),fullyQualifiedName));
        }
        return map;
    }

    /**
     * Filters the tools based on blocked tools configuration.
     *
     * @return filtered map of tools
     */
    public Map<ServerTool, McpSchema.Tool> filterTools() {
        return toolRegistry.filterTools();
    }

    /**
     * Initializes MCP clients from mcp.json file.
     * This method is used by tests and can be called manually.
     */
    public void initializeClients() {
        initializeClientsFromMcpFile();
    }

    /**
     * Initializes MCP clients after the Spring application is fully initialized.
     * Loads configuration from mcp.json and creates appropriate transport for each server.
     */
    //@EventListener(ApplicationReadyEvent.class)
    public void initializeClientsFromMcpFile() {
        logger.info("Initializing MCP clients from configuration...");
        try {
            // Load configuration using the factory
            ConfigInitializer<McpConfig> initializer = McpConfigFactory.createInitializer();
            McpConfig config = initializer.init(ConfigSources.fromPath(Paths.get("mcp.json")));

            if (config == null || config.mcpServers() == null || config.mcpServers().isEmpty()) {
                logger.warn("No MCP servers configured in mcp.json");
                return;
            }

            String userHomeDir = System.getProperty("user.home");

            // Iterate over each server configuration
            for (Map.Entry<String, McpServer> entry : config.mcpServers().entrySet()) {
                String serverName = entry.getKey();
                McpServer serverConfig = entry.getValue();
                clientManager.loadMcpClients(serverName, serverConfig, userHomeDir);
            }
            
            toolRegistry.refreshToolMap(clientManager.getClientsByName());
            
            logger.info("MCP client initialization from mcp.json complete. Loaded {} clients. Not loaded {} clients " +
                    "TotalTools {}", clientManager.getClientsByName().size(), 
                    clientManager.getNoToolsClient().size(), toolRegistry.getToolMap().size());
        } catch (IOException e) {
            logger.error("Failed to load MCP configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * Initializes MCP clients from AgentConfig by processing mcpServers from each command.
     * This method goes through each command in the AgentConfig, extracts the mcpServers JSON string,
     * and uses ObjectMapper to parse it into McpServer configurations.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeClientsFromAgentConfig() {
        logger.info("Initializing MCP clients from AgentConfig...");
        var agentConfig = agentConfigManager.getAgentConfig();
        if (agentConfig == null || agentConfig.commands() == null || agentConfig.commands().isEmpty()) {
            logger.warn("No commands configured in AgentConfig");
            return;
        }
        String userHomeDir = System.getProperty("user.home");

        // Go through each command and extract MCP servers
        for (Map.Entry<String, AgentCommand> commandEntry : agentConfig.commands().entrySet()) {
            String commandName = commandEntry.getKey();
            AgentCommand command = commandEntry.getValue();

            if (command.mcpConfig() != null) {
                try {
                    McpConfig substitutedMcpConfig = EnvSubstitution.substitute(command.mcpConfig());
                    clientManager.loadAgentMcpClients(commandName, substitutedMcpConfig.mcpServers().entrySet(), 
                            userHomeDir);
                } catch (Exception e) {
                    logger.error("Failed to parse MCP server from Command Name '{}' from command '{}': {}", command,
                            commandName, e.getMessage(), e);
                }
            }
        }
        logger.info("MCP client initialization from agent.yml file complete. Loaded {} Agent(s) ",
                clientManager.getAllAgentMcpServers().size());
    }
}
