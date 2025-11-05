/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.app.dto;

public class SnykProject {
    private String id;
    private String name;
    private String url;
    private String origin;
    private String type;
    private String readOnly;
    private String testFrequency;
    private String totalDependencies;
    private String issueCountsBySeverity;

    public SnykProject() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(String readOnly) {
        this.readOnly = readOnly;
    }

    public String getTestFrequency() {
        return testFrequency;
    }

    public void setTestFrequency(String testFrequency) {
        this.testFrequency = testFrequency;
    }

    public String getTotalDependencies() {
        return totalDependencies;
    }

    public void setTotalDependencies(String totalDependencies) {
        this.totalDependencies = totalDependencies;
    }

    public String getIssueCountsBySeverity() {
        return issueCountsBySeverity;
    }

    public void setIssueCountsBySeverity(String issueCountsBySeverity) {
        this.issueCountsBySeverity = issueCountsBySeverity;
    }

    @Override
    public String toString() {
        return "SnykProject{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", origin='" + origin + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}