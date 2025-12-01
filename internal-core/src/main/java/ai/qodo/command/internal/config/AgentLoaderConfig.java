/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "qodo.agent")
public class AgentLoaderConfig {
    
    private String configFile = "classpath:agent.yml";
    
    public String getConfigFile() {
        return configFile;
    }
    
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }
}
