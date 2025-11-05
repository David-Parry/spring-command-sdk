/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.api;

/**
 * Builder class for creating AgentRequest objects using sub-builders.
 * This builder provides a cleaner API by using dedicated builders for each data component.
 */
public class AgentRequestBuilder {

    private final BaseDataBuilder baseDataBuilder;
    private final TaskBaseDataBuilder taskBaseDataBuilder;
    private final TaskRequestDataBuilder taskRequestDataBuilder;

    public AgentRequestBuilder() {
        this.baseDataBuilder = new BaseDataBuilder();
        this.taskBaseDataBuilder = new TaskBaseDataBuilder();
        this.taskRequestDataBuilder = new TaskRequestDataBuilder();
    }


    // Direct access to sub-builders
    public BaseDataBuilder baseData() {
        return baseDataBuilder;
    }

    public TaskBaseDataBuilder taskBaseData() {
        return taskBaseDataBuilder;
    }

    public TaskRequestDataBuilder taskRequestData() {
        return taskRequestDataBuilder;
    }

    // BaseData builder methods (delegated)
    public AgentRequestBuilder sessionId(String sessionId) {
        baseDataBuilder.sessionId(sessionId);
        return this;
    }


    public AgentRequestBuilder tool(String tool) {
        taskRequestDataBuilder.tool(tool);
        return this;
    }


    /**
     * Builds the AgentRequest from the configured sub-builders.
     *
     * @return A new AgentRequest instance
     */
    public AgentRequest build() {
        BaseData baseData = baseDataBuilder.build();
        TaskBaseData taskBaseData = taskBaseDataBuilder.build();
        TaskRequestData taskRequestData = taskRequestDataBuilder.build();

        return new AgentRequest(baseData, taskBaseData, taskRequestData);
    }


}