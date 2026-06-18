package com.rootlens.dashboard.dto;

import java.io.Serializable;

public class ServiceHealthDto implements Serializable {

    private String name;
    private int activeIncidents;
    private String highestSeverity;
    private Long lastEventAt;

    public ServiceHealthDto() {}

    public ServiceHealthDto(String name, int activeIncidents, String highestSeverity, Long lastEventAt) {
        this.name = name;
        this.activeIncidents = activeIncidents;
        this.highestSeverity = highestSeverity;
        this.lastEventAt = lastEventAt;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getActiveIncidents() { return activeIncidents; }
    public void setActiveIncidents(int activeIncidents) { this.activeIncidents = activeIncidents; }

    public String getHighestSeverity() { return highestSeverity; }
    public void setHighestSeverity(String highestSeverity) { this.highestSeverity = highestSeverity; }

    public Long getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(Long lastEventAt) { this.lastEventAt = lastEventAt; }
}
