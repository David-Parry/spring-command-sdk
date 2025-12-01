/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record AIAssistantConfig(
    @JsonProperty("version") String version,
    @JsonProperty("system_prompt") String systemPrompt,
    @JsonProperty("instructions") String instructions,
    @JsonProperty("commands") Map<String, CommandConfig> commands,
    @JsonProperty("imports") List<String> imports,
    @JsonProperty("mcpServers") Object mcpServers,
    @JsonProperty("model") String model,
    @JsonProperty("available_tools") List<String> availableTools,
    @JsonProperty("output_schema") Object outputSchema,
    @JsonProperty("exit_expression") String exitExpression,
    @JsonProperty("execution_strategy") String executionStrategy
) {}
