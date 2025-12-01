/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Record representing an HTTP-based MCP server configuration.
 * This type of server is accessed via HTTP URL.
 */
public record HttpServer(
    @JsonProperty("type")
    String type,
    
    @JsonProperty("url")
    String url,
    
    @JsonProperty("headers")
    Map<String, String> headers,
    
    @JsonProperty("env")
    Map<String, String> env
) implements McpServer {
    
    /**
     * Constructor for HttpServer when only URL is provided (type field is optional).
     */
    public HttpServer(String url) {
        this("http", url, null, null);
    }
    
    /**
     * Constructor for HttpServer with URL and headers.
     */
    public HttpServer(String url, Map<String, String> headers) {
        this("http", url, headers, null);
    }
    
    /**
     * Constructor for HttpServer with URL and environment variables.
     */
    public HttpServer(String url, Map<String, String> headers, Map<String, String> env) {
        this("http", url, headers, env);
    }
}