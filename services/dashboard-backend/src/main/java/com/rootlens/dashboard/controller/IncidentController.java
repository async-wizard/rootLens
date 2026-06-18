package com.rootlens.dashboard.controller;

import com.rootlens.dashboard.dto.IncidentDetailDto;
import com.rootlens.dashboard.dto.IncidentSummaryDto;
import com.rootlens.dashboard.dto.PagedResponse;
import com.rootlens.dashboard.dto.StatusUpdateRequest;
import com.rootlens.dashboard.service.IncidentService;
import com.rootlens.dashboard.sse.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Incidents", description = "Incident lifecycle management")
@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final SseEmitterRegistry sseEmitterRegistry;

    public IncidentController(IncidentService incidentService, SseEmitterRegistry sseEmitterRegistry) {
        this.incidentService = incidentService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @Operation(summary = "List incidents (paginated, filterable by severity/status/service)")
    @GetMapping
    public ResponseEntity<PagedResponse<IncidentSummaryDto>> listIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String service) {
        return ResponseEntity.ok(incidentService.getIncidents(page, size, severity, status, service));
    }

    @Operation(summary = "Get incident detail")
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDetailDto> getIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getIncidentDetail(id));
    }

    @Operation(summary = "SSE stream — real-time incident updates")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseEmitterRegistry.register();
    }

    @Operation(summary = "Transition incident status (FSM-guarded)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentSummaryDto> updateStatus(
            @PathVariable String id,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(incidentService.transition(id, request.getStatus()));
    }
}
