package com.rootlens.dashboard.service;

import com.rootlens.dashboard.dto.AnalysisDto;
import com.rootlens.dashboard.dto.IncidentDetailDto;
import com.rootlens.dashboard.dto.IncidentEventDto;
import com.rootlens.dashboard.dto.IncidentSummaryDto;
import com.rootlens.dashboard.dto.PagedResponse;
import com.rootlens.dashboard.entity.AiAnalysis;
import com.rootlens.dashboard.entity.Incident;
import com.rootlens.dashboard.entity.IncidentEventRecord;
import com.rootlens.dashboard.model.IncidentStatus;
import com.rootlens.dashboard.repository.AiAnalysisRepository;
import com.rootlens.dashboard.repository.IncidentEventRecordRepository;
import com.rootlens.dashboard.repository.IncidentRepository;
import com.rootlens.dashboard.specification.IncidentSpecification;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRecordRepository eventRepository;
    private final AiAnalysisRepository analysisRepository;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentEventRecordRepository eventRepository,
                           AiAnalysisRepository analysisRepository) {
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.analysisRepository = analysisRepository;
    }

    @Cacheable(value = "incidents-list",
               key = "'p:'+#page+':s:'+#size+':sev:'+#severity+':st:'+#status+':srv:'+#service")
    public PagedResponse<IncidentSummaryDto> getIncidents(
            int page, int size, String severity, String status, String service) {
        Specification<Incident> spec = IncidentSpecification.withFilters(severity, status, service);
        Page<Incident> result = incidentRepository.findAll(
                spec, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        List<IncidentSummaryDto> content = result.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PagedResponse<>(content, page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    @Cacheable(value = "incident-detail", key = "#id")
    public IncidentDetailDto getIncidentDetail(String id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));

        List<IncidentEventRecord> events = eventRepository.findByIncidentIdOrderByReceivedAtAsc(id);
        Optional<AiAnalysis> analysis = analysisRepository.findByIncidentId(id);

        return toDetail(incident, events, analysis.orElse(null));
    }

    public Optional<AnalysisDto> getAnalysis(String incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new NoSuchElementException("Incident not found: " + incidentId);
        }
        return analysisRepository.findByIncidentId(incidentId).map(this::toAnalysisDto);
    }

    @Caching(evict = {
        @CacheEvict(value = "incidents-list", allEntries = true),
        @CacheEvict(value = "incident-detail", key = "#id")
    })
    public IncidentSummaryDto transition(String id, String requestedStatus) {
        IncidentStatus requested;
        try {
            requested = IncidentStatus.valueOf(requestedStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + requestedStatus);
        }

        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));

        IncidentStatus current = IncidentStatus.valueOf(incident.getStatus());
        if (!current.canTransitionTo(requested)) {
            throw new IllegalStateException(
                    "Cannot transition from " + current + " to " + requested);
        }

        incident.setStatus(requested.name());
        incident.setUpdatedAt(System.currentTimeMillis());
        incidentRepository.save(incident);

        return toSummary(incident);
    }

    private IncidentSummaryDto toSummary(Incident incident) {
        IncidentSummaryDto dto = new IncidentSummaryDto();
        dto.setIncidentId(incident.getId());
        dto.setStatus(incident.getStatus());
        dto.setSeverity(incident.getSeverity());
        dto.setServicesImpacted(incident.getServicesImpacted());
        dto.setCreatedAt(incident.getCreatedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());
        dto.setHasAnalysis(analysisRepository.findByIncidentId(incident.getId()).isPresent());
        return dto;
    }

    private IncidentDetailDto toDetail(Incident incident, List<IncidentEventRecord> events,
                                       AiAnalysis analysis) {
        IncidentDetailDto dto = new IncidentDetailDto();
        dto.setIncidentId(incident.getId());
        dto.setStatus(incident.getStatus());
        dto.setSeverity(incident.getSeverity());
        dto.setServicesImpacted(incident.getServicesImpacted());
        dto.setTraceIds(incident.getTraceIds());
        dto.setCreatedAt(incident.getCreatedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());
        dto.setEvents(events.stream().map(this::toEventDto).toList());
        dto.setAnalysis(analysis != null ? toAnalysisDto(analysis) : null);
        return dto;
    }

    private IncidentEventDto toEventDto(IncidentEventRecord rec) {
        IncidentEventDto dto = new IncidentEventDto();
        dto.setService(rec.getService());
        dto.setSeverity(rec.getSeverity());
        dto.setTraceId(rec.getTraceId());
        dto.setMessage(rec.getMessage());
        dto.setOriginalTimestamp(rec.getOriginalTimestamp());
        dto.setReceivedAt(rec.getReceivedAt());
        return dto;
    }

    private AnalysisDto toAnalysisDto(AiAnalysis a) {
        AnalysisDto dto = new AnalysisDto();
        dto.setSummary(a.getSummary());
        dto.setProbableCause(a.getProbableCause());
        dto.setRemediation(a.getRemediation());
        dto.setModel(a.getModel());
        dto.setAnalysisTimestamp(a.getAnalysisTimestamp());
        dto.setSuccess(a.isSuccess());
        return dto;
    }
}
