/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Jira agent webhook blocking.
 * Used to prevent infinite webhook loops when the agent performs actions in Jira.
 */
@Component
@ConfigurationProperties(prefix = "jira.webhook.agent")
public class JiraAgentProperties {
    
    /**
     * The Atlassian account ID of the agent user.
     * This is used to identify webhooks triggered by the agent itself.
     */
    private String accountId;
    
    /**
     * Whether to block webhooks triggered by the agent.
     * When true, webhooks from the agent account will be skipped to prevent loops.
     */
    private boolean blockEnabled = true;
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public boolean isBlockEnabled() {
        return blockEnabled;
    }
    
    public void setBlockEnabled(boolean blockEnabled) {
        this.blockEnabled = blockEnabled;
    }
}
