package com.rootlens.ingestion.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class GenericEventRequest {

    @NotBlank(message = "eventType is required")
    private String eventType;

    @NotBlank(message = "service is required")
    private String service;

    private Map<String, Object> payload;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
