package com.rootlens.ingestion.controller;

import com.rootlens.ingestion.dto.TraceEventRequest;
import com.rootlens.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/traces")
public class TraceController {

    private final IngestionService ingestionService;

    public TraceController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> ingestTrace(@Valid @RequestBody TraceEventRequest request) {
        ingestionService.ingestTrace(request);
        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "topic", "traces-topic"));
    }
}
