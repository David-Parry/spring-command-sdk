/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.integration;

import ai.qodo.command.internal.api.*;
import ai.qodo.command.internal.config.BlockedToolsConfiguration;
import ai.qodo.command.internal.config.MCPClientInitializer;
import ai.qodo.command.internal.config.QodoProperties;
import ai.qodo.command.internal.config.client.McpClientManager;
import ai.qodo.command.internal.config.client.ToolRegistry;
import ai.qodo.command.internal.config.transport.DefaultTransportFactory;
import ai.qodo.command.internal.mcp.*;
import ai.qodo.command.internal.metrics.McpMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;


public class ToolsListPassingInvestigation {
    final static String MCP_JSON_FILE_NAME = "mcp.json";
    final static String token = System
            .getenv()
            .getOrDefault("QODO_API_KEY", "sk-4k9bgFXmGQ1h7j9DuS4jADiolvPan-R8CHRAbLMIu8o-wZgCzEh" +
                    "-QOK04s0LisrpwSCBp5SFavUaO_zrIWvOAQk");
    final static String baseUrl = System.getenv().getOrDefault("QODO_API_BASE_URL", "https://api.command.qodo.ai");
    final static String question = "Tell me the path for the FigureOut class.";
    public static ObjectMapper MAPPER = new ObjectMapper();
    public static MCPClientInitializer mcpClientInitializer;

    public static void main(String[] args) throws Exception {
        final String sessionId = UUID.randomUUID().toString();
        final String requestId = UUID.randomUUID().toString();
        final String wsBase = baseUrl.replaceFirst("^http", "ws").replaceAll("/+$", "");
        final String wsUrl = wsBase + "/v2/agentic/ws/start-task?session_id=" + sessionId + "&request_id=" + requestId;

        ConfigInitializer<McpConfig> initializer = McpConfigFactory.createInitializer();
        McpConfig config = initializer.init(ConfigSources.fromPath(Paths.get("mcp.json")));

        config.mcpServers().values().forEach(mcpServer -> System.out.println("MCP Server:  - " + mcpServer));
        BlockedToolsConfiguration blockedToolsConfiguration = new BlockedToolsConfiguration();
        blockedToolsConfiguration.setBlockedTools("");
        blockedToolsConfiguration.initializeBlockedTools();

        // Create required dependencies for MCPClientInitializer
        DefaultTransportFactory transportFactory = new DefaultTransportFactory();
        
        // Create caches for McpMetrics
        Cache<String, Counter> invocationCounterCache = Caffeine.newBuilder().maximumSize(1000).build();
        Cache<String, Counter> successCounterCache = Caffeine.newBuilder().maximumSize(1000).build();
        Cache<String, Counter> failureCounterCache = Caffeine.newBuilder().maximumSize(1000).build();
        Cache<String, Timer> timerCache = Caffeine.newBuilder().maximumSize(1000).build();
        
        McpMetrics mcpMetrics = new McpMetrics(
            new SimpleMeterRegistry(),
            invocationCounterCache,
            successCounterCache,
            failureCounterCache,
            timerCache
        );
        
        QodoProperties qodoProperties = new QodoProperties();
        qodoProperties.setBaseUrl(baseUrl);
        // Set any other properties if needed
        McpClientManager mcpClientManager = new McpClientManager(transportFactory, mcpMetrics, qodoProperties);
        ToolRegistry toolRegistry = new ToolRegistry(blockedToolsConfiguration, mcpMetrics);
        
        // AgentConfigManager is not used in this test, so we pass null
        mcpClientInitializer = new MCPClientInitializer(mcpClientManager, toolRegistry, null);

        mcpClientInitializer.initializeClients();

        Map<String, McpServerInitialized> mcpServers = mcpClientInitializer.getClientsByName();



        AgentRequestBuilder builder = new AgentRequestBuilder();

        builder.baseData()
               .sessionId(sessionId)
               .agentType("cli")
               .userData()
               .permissions("rwx")
                .tools(mcpServers, null);

        builder.taskBaseData()
               .instructions("You are a knowledgeable time keeper. You need to use the mcp server default-server and its get_time tool to aide in answering the question.")
               .cwd("/Users/davidparry/code/github/qodo-command-java/java")
               .addProjectRootPath("/Users/davidparry/code/github/qodo-command-java/java");

        builder.taskRequestData()
               .userRequest("Tell me what the time of day is now.")
               .executionStrategy("act")
               .customModel("gpt-4.1");

        AgentRequest subBuilderRequest = builder.build();

        String payload = MAPPER.writeValueAsString(subBuilderRequest);

        System.out.println(payload);




        HttpClient client = HttpClient.newHttpClient();
        CountDownLatch done = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(1);

        List<TaskResponse> messages = new ArrayList<>();
        WebSocket ws = client
                .newWebSocketBuilder()
                .header("Authorization", "Bearer " + token)
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder partial = new StringBuilder();
                    int counter = 1;

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("Connected: " + wsUrl);
                        // Send initial request payload
                        webSocket.sendText(payload, true);
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        partial.append(data);
                        if (last) {
                            final String msg = partial.toString();
                            partial.setLength(0);

                            try {
                                TaskResponse taskResponse = MAPPER.readValue(msg, TaskResponse.class);
                                messages.add(taskResponse);
                                if(taskResponse.error() != null || taskResponse.data() == null) {
                                    // we have an error or no tool data something went wrong
                                    System.err.println("Error::"+ taskResponse.error() + "  "+ taskResponse);
                                } else {
                                    ToolData toolData = taskResponse.data();
                                    switch (toolData.tool()) {
                                        case "UserResponse":
                                            System.out.println("UserResponse ::"+taskResponse);
                                            break;
                                        case "EndNode":
                                            System.out.println("EndNode::" + taskResponse);
                                            end.countDown();
                                            break;
                                        default:
                                            webSocket.sendText(toolResponseTime(sessionId, toolData), true);
                                            break;
                                    }
                                }


                            } catch (JsonProcessingException e) {
                                System.out.println("[ERROR DAVID] : " + msg);
                                e.printStackTrace();
                            }


                            counter++;


                        }


                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        System.out.println(new String(bytes, StandardCharsets.UTF_8));
                        return WebSocket.Listener.super.onBinary(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        System.out.println("Closed from Server: " + statusCode + " reason:" + reason);
                        printoutList("ONCLOSE", messages);
                        end.countDown();
                        done.countDown();
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        System.err.println("WebSocket error: " + error.getMessage());
                        printoutList("ONERROR", messages);
                        end.countDown();
                        done.countDown();
                    }
                })
                .join();
        System.out.println("Waiting for endnode");
        end.await();
        System.out.println("Sending close");
        ws.sendClose(1000, "Normal closure");
        System.out.println("Waiting for close message or error");
        done.await();
        System.out.println("Done await has counted down");

    }


    private static void printoutList(String marker, List<TaskResponse> messages) {
        System.out.println(marker + "\n=== Message received (length: " + messages.size() + ") ===");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            TaskResponse msg = messages.get(i);
            if (msg != null) {
                ToolData trd = msg.data();
                if (trd.tool() != null && trd.tool().equals("UserResponse")) {
                    builder.append(trd.toolArgs().values());
                }
            }
        }
        System.out.println(builder);
        System.out.println("=== End of message ===");
    }


    private static String escape(String s) {
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    private static String toolResponseTime(String sessionId, ToolData toolData) {
        try {
            LocalDateTime now = LocalDateTime.now();
            ToolResponse response = new ToolResponseBuilder()
                    .tool(toolData.tool())
                    .toolId(toolData.identifier())
                    .answer(ToolResponseBuilder
                                    .answer()
                                    .isError(false)
                                    .addTextContent("The current time is " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                                    .build())
                    .build();
            String msg =  MAPPER.writeValueAsString(response);
            System.out.println("!!!!!!!!! Tool Response sending is ::" + msg);
            return msg;


        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ToolResponse", e);
        }
    }



}
