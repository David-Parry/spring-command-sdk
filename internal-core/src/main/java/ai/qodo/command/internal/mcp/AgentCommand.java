/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.mcp;

import ai.qodo.command.internal.api.CommandArgument;
import ai.qodo.command.internal.api.OutputSchema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a command configuration in the agent.yml file
 */
public record AgentCommand(
    String description,
    String instructions,
    String model,
    List<CommandArgument> arguments,
    @JsonProperty("mcpServers") String mcpServers,
    List<String> tools,
    @JsonProperty("execution_strategy") String executionStrategy,
    @JsonProperty("output_schema") String outputSchemaString,
    @JsonProperty("exit_expression") String exitExpression,
    OutputSchema outputSchema,
    String systemPrompt,
    String version,
    String name,
    McpConfig mcpConfig

) {
    /**
     * Constructor overload that takes an AgentCommand and an OutputSchema and copies all their fields
     */
    public AgentCommand(AgentCommand agentCommand, OutputSchema outputSchema, String systemPrompt, String version, String name, McpConfig mcpConfig) {
        this(
            agentCommand.description(),
            agentCommand.instructions(),
            agentCommand.model(),
            agentCommand.arguments(),
            agentCommand.mcpServers(),
            agentCommand.tools(),
            agentCommand.executionStrategy(),
            agentCommand.outputSchemaString(),
            agentCommand.exitExpression(),
            outputSchema, systemPrompt, version, name, mcpConfig
        );
    }
}