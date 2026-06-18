package com.rootlens.ingestion.controller;

import com.rootlens.ingestion.dto.MetricEventRequest;
import com.rootlens.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricController {

    private final IngestionService ingestionService;

    public MetricController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> ingestMetric(@Valid @RequestBody MetricEventRequest request) {
        ingestionService.ingestMetric(request);
        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "topic", "metrics-topic"));
    }
}
