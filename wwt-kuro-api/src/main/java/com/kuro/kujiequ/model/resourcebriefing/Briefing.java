package com.kuro.kujiequ.model.resourcebriefing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-14 10:38
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Briefing {
    private List<Title> weeks;
    private List<Title> months;
    private List<Title> versions;

    public List<Title> getWeeks() {
        return weeks;
    }

    public void setWeeks(List<Title> weeks) {
        this.weeks = weeks;
    }

    public List<Title> getMonths() {
        return months;
    }

    public void setMonths(List<Title> months) {
        this.months = months;
    }

    public List<Title> getVersions() {
        return versions;
    }

    public void setVersions(List<Title> versions) {
        this.versions = versions;
    }
}