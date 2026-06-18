package com.rootlens.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MetricEventRequest {

    @NotBlank(message = "service is required")
    private String service;

    @NotBlank(message = "metricName is required")
    private String metricName;

    @NotNull(message = "value is required")
    private Double value;

    private String unit;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
