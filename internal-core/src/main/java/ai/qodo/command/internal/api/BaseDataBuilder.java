/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.api;

import ai.qodo.command.internal.util.OSPlatformDetector;
import ai.qodo.command.internal.mcp.McpServerInitialized;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder class for creating BaseData objects.
 * Provides a fluent API for constructing BaseData instances with proper defaults.
 */
public class BaseDataBuilder {
    
    private String sessionId;
    private UserDataRequest userData;
    private String agentType;
    private Map<String, List<Map<String, Object>>> tools;
    private String customModel;
    private String permissions;
    
    public BaseDataBuilder() {
        this.tools = new HashMap<>();
    }
    
    public BaseDataBuilder sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
    
    public BaseDataBuilder userData(UserDataRequest userData) {
        this.userData = userData;
        return this;
    }
    
    public BaseDataBuilder userData(String extensionVersion, String osPlatform, String osVersion, String editorType) {
        this.userData = new UserDataRequest(extensionVersion, osPlatform, osVersion, 
                                          editorType == null || editorType.trim().isEmpty() ? "cli" : editorType);
        return this;
    }
    
    public BaseDataBuilder userData(String client, String ts) {
        this.userData = new UserDataRequest(null, OSPlatformDetector.getPlatformName(), 
                                          OSPlatformDetector.getJDKVersionIdentifier(), "cli");
        return this;
    }
    
    public BaseDataBuilder userData() {
        this.userData = new UserDataRequest(null, OSPlatformDetector.getPlatformName(), 
                                          OSPlatformDetector.getJDKVersionIdentifier(), "cli");
        return this;
    }
    
    public BaseDataBuilder agentType(String agentType) {
        this.agentType = agentType;
        return this;
    }

    public BaseDataBuilder tools(Map<String, McpServerInitialized> mcpServers, List<String> toolFilter) {
        this.tools = createTools(mcpServers, toolFilter);
        return this;
    }
    
    public BaseDataBuilder customModel(String customModel) {
        this.customModel = customModel;
        return this;
    }
    
    public BaseDataBuilder permissions(String permissions) {
        this.permissions = permissions;
        return this;
    }
    protected static Map<String, List<Map<String, Object>>> createTools(Map<String, McpServerInitialized> mcpServers, List<String> filter) {
        Map<String, List<Map<String, Object>>> tools = new HashMap<>();
        
        // If filter is null or empty, no filtering - add all tools
        boolean noFilter = filter == null || filter.isEmpty();
        
        mcpServers.forEach((serverName, mcpServer) -> {
            List<Map<String, Object>> toolList = new java.util.ArrayList<>();
            
            mcpServer.tools().forEach(tool -> {
                boolean shouldAddTool = false;
                
                if (noFilter) {
                    // No filter, add all tools
                    shouldAddTool = true;
                } else {
                    // Check filter criteria
                    for (String filterEntry : filter) {
                        if (filterEntry.contains(".")) {
                            // Format: mcpServer.toolName
                            String[] parts = filterEntry.split("\\.", 2);
                            String filterMcpServer = parts[0];
                            String filterToolName = parts[1];
                            
                            // Check if both mcpServer and tool name match
                            if (serverName.equals(filterMcpServer) && tool.name().equals(filterToolName)) {
                                shouldAddTool = true;
                                break;
                            }
                        } else {
                            // Format: just mcpServer name - include all tools from this server
                            if (serverName.equals(filterEntry)) {
                                shouldAddTool = true;
                                break;
                            }
                        }
                    }
                }
                
                if (shouldAddTool) {
                    Map<String, Object> toolMap = new HashMap<>();
                    toolMap.put("name", tool.name());
                    toolMap.put("description", tool.description());
                    toolMap.put("inputSchema", tool.inputSchema());
                    toolList.add(toolMap);
                }
            });
            
            // Only add the serverName entry if there are tools to add
            if (!toolList.isEmpty()) {
                tools.put(serverName, toolList);
            }
        });

        return tools;
    }
    /**
     * Builds the BaseData from the configured parameters.
     * 
     * @return A new BaseData instance
     */
    public BaseData build() {
        return new BaseData(
            sessionId,
            userData,
            agentType,
            tools,
            customModel,
            permissions
        );
    }
    
    /**
     * Static factory method to create a builder with default CLI configuration.
     * 
     * @return A pre-configured BaseDataBuilder
     */
    public static BaseDataBuilder defaultCli() {
        return new BaseDataBuilder()
            .agentType("cli")
            .userData()
            .permissions("rwx");
    }
    

}