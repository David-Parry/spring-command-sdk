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
import java.util.Map;

public class SnykIssue {
    private String id;
    private String title;
    private String severity;
    private String url;
    @JsonProperty("type")
    private String type;
    private String description;
    @JsonProperty("introducedDate")
    private String introducedDate;
    @JsonProperty("disclosureTime")
    private String disclosureTime;
    @JsonProperty("publicationTime")
    private String publicationTime;
    @JsonProperty("isUpgradable")
    private Boolean isUpgradable;
    @JsonProperty("isPatchable")
    private Boolean isPatchable;
    @JsonProperty("isPinnable")
    private Boolean isPinnable;
    @JsonProperty("identifiers")
    private Map<String, List<String>> identifiers;
    @JsonProperty("credit")
    private List<String> credit;
    @JsonProperty("CVSSv3")
    private String cvssV3;
    @JsonProperty("cvssScore")
    private Double cvssScore;
    @JsonProperty("language")
    private String language;
    @JsonProperty("packageName")
    private String packageName;
    @JsonProperty("version")
    private String version;
    @JsonProperty("packageManager")
    private String packageManager;

    public SnykIssue() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIntroducedDate() {
        return introducedDate;
    }

    public void setIntroducedDate(String introducedDate) {
        this.introducedDate = introducedDate;
    }

    public String getDisclosureTime() {
        return disclosureTime;
    }

    public void setDisclosureTime(String disclosureTime) {
        this.disclosureTime = disclosureTime;
    }

    public String getPublicationTime() {
        return publicationTime;
    }

    public void setPublicationTime(String publicationTime) {
        this.publicationTime = publicationTime;
    }

    public Boolean getIsUpgradable() {
        return isUpgradable;
    }

    public void setIsUpgradable(Boolean isUpgradable) {
        this.isUpgradable = isUpgradable;
    }

    public Boolean getIsPatchable() {
        return isPatchable;
    }

    public void setIsPatchable(Boolean isPatchable) {
        this.isPatchable = isPatchable;
    }

    public Boolean getIsPinnable() {
        return isPinnable;
    }

    public void setIsPinnable(Boolean isPinnable) {
        this.isPinnable = isPinnable;
    }

    public Map<String, List<String>> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, List<String>> identifiers) {
        this.identifiers = identifiers;
    }

    public List<String> getCredit() {
        return credit;
    }

    public void setCredit(List<String> credit) {
        this.credit = credit;
    }

    public String getCvssV3() {
        return cvssV3;
    }

    public void setCvssV3(String cvssV3) {
        this.cvssV3 = cvssV3;
    }

    public Double getCvssScore() {
        return cvssScore;
    }

    public void setCvssScore(Double cvssScore) {
        this.cvssScore = cvssScore;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackageManager() {
        return packageManager;
    }

    public void setPackageManager(String packageManager) {
        this.packageManager = packageManager;
    }

    @Override
    public String toString() {
        return "SnykIssue{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", severity='" + severity + '\'' +
                ", type='" + type + '\'' +
                ", packageName='" + packageName + '\'' +
                ", version='" + version + '\'' +
                ", cvssScore=" + cvssScore +
                '}';
    }
}