package com.rootlens.dashboard.controller;

import com.rootlens.dashboard.dto.ServiceHealthDto;
import com.rootlens.dashboard.service.ServiceHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/services")
public class ServiceController {

    private final ServiceHealthService serviceHealthService;

    public ServiceController(ServiceHealthService serviceHealthService) {
        this.serviceHealthService = serviceHealthService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<ServiceHealthDto>>> getServiceHealth() {
        return ResponseEntity.ok(Map.of("services", serviceHealthService.getServiceHealth()));
    }
}
