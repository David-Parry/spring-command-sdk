/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SnykWebhookPayload {
    private SnykProject project;
    private SnykOrganization org;
    private SnykGroup group;
    @JsonProperty("newIssues")
    private List<SnykIssue> newIssues;
    @JsonProperty("removedIssues")
    private List<SnykIssue> removedIssues;
    private String timestamp;
    private String eventType;

    public SnykWebhookPayload() {}

    public SnykProject getProject() {
        return project;
    }

    public void setProject(SnykProject project) {
        this.project = project;
    }

    public SnykOrganization getOrg() {
        return org;
    }

    public void setOrg(SnykOrganization org) {
        this.org = org;
    }

    public SnykGroup getGroup() {
        return group;
    }

    public void setGroup(SnykGroup group) {
        this.group = group;
    }

    public List<SnykIssue> getNewIssues() {
        return newIssues;
    }

    public void setNewIssues(List<SnykIssue> newIssues) {
        this.newIssues = newIssues;
    }

    public List<SnykIssue> getRemovedIssues() {
        return removedIssues;
    }

    public void setRemovedIssues(List<SnykIssue> removedIssues) {
        this.removedIssues = removedIssues;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String toString() {
        return "SnykWebhookPayload{" +
                "project=" + project +
                ", org=" + org +
                ", group=" + group +
                ", newIssues=" + (newIssues != null ? newIssues.size() : 0) + " issues" +
                ", removedIssues=" + (removedIssues != null ? removedIssues.size() : 0) + " issues" +
                ", timestamp='" + timestamp + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}