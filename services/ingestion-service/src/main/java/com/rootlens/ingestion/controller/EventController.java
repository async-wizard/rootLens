package com.rootlens.ingestion.controller;

import com.rootlens.ingestion.dto.GenericEventRequest;
import com.rootlens.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventController {

    private final IngestionService ingestionService;

    public EventController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> ingestEvent(@Valid @RequestBody GenericEventRequest request) {
        ingestionService.ingestEvent(request);
        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "topic", "events-topic"));
    }
}
