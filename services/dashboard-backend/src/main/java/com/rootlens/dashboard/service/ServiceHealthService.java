package com.rootlens.dashboard.service;

import com.rootlens.dashboard.dto.ServiceHealthDto;
import com.rootlens.dashboard.entity.Incident;
import com.rootlens.dashboard.repository.IncidentEventRecordRepository;
import com.rootlens.dashboard.repository.IncidentRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceHealthService {

    private static final List<String> SEVERITY_ORDER = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW");

    private final IncidentRepository incidentRepository;
    private final IncidentEventRecordRepository eventRepository;

    public ServiceHealthService(IncidentRepository incidentRepository,
                                IncidentEventRecordRepository eventRepository) {
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
    }

    @Cacheable(value = "services-health", key = "'all'")
    public List<ServiceHealthDto> getServiceHealth() {
        List<Incident> activeIncidents = incidentRepository.findByStatusNot("RESOLVED");

        // service name → [incidentCount, highestSeverity, lastEventAt]
        Map<String, int[]> incidentCount = new HashMap<>();
        Map<String, String> highestSeverity = new HashMap<>();
        Map<String, Long> lastEventAt = new HashMap<>();

        for (Incident incident : activeIncidents) {
            for (String service : incident.getServicesImpacted()) {
                incidentCount.merge(service, new int[]{1}, (a, b) -> new int[]{a[0] + 1});
                highestSeverity.merge(service, incident.getSeverity(), this::higherSeverity);
                lastEventAt.merge(service, incident.getUpdatedAt(), Math::max);
            }
        }

        List<ServiceHealthDto> result = new ArrayList<>();
        for (String service : incidentCount.keySet()) {
            result.add(new ServiceHealthDto(
                    service,
                    incidentCount.get(service)[0],
                    highestSeverity.getOrDefault(service, "MEDIUM"),
                    lastEventAt.getOrDefault(service, 0L)
            ));
        }

        result.sort((a, b) -> severityRank(a.getHighestSeverity()) - severityRank(b.getHighestSeverity()));
        return result;
    }

    private String higherSeverity(String a, String b) {
        return severityRank(a) <= severityRank(b) ? a : b;
    }

    private int severityRank(String severity) {
        int idx = SEVERITY_ORDER.indexOf(severity);
        return idx == -1 ? SEVERITY_ORDER.size() : idx;
    }
}
