package com.rootlens.dashboard.controller;

import com.rootlens.dashboard.dto.AnalysisDto;
import com.rootlens.dashboard.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/incidents")
public class AnalysisController {

    private final IncidentService incidentService;

    public AnalysisController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<AnalysisDto> getAnalysis(@PathVariable String id) {
        return incidentService.getAnalysis(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
