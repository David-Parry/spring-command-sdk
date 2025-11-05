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

public record BaseData(
    @JsonProperty("session_id") String sessionId,
    @JsonProperty("user_data") UserDataRequest userData,
    @JsonProperty("agent_type") String agentType,
    @JsonProperty("tools") Map<String, List<Map<String, Object>>> tools,
    @JsonProperty("custom_model") String customModel,
    @JsonProperty("permissions") String permissions
) {}
