package com.rootlens.aianalysis.service;

import com.rootlens.aianalysis.entity.IncidentEventRecord;
import com.rootlens.aianalysis.repository.IncidentEventRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContextAggregationService {

    private final IncidentEventRecordRepository repository;

    public ContextAggregationService(IncidentEventRecordRepository repository) {
        this.repository = repository;
    }

    public List<IncidentEventRecord> aggregate(String incidentId) {
        return repository.findByIncidentIdOrderByReceivedAtAsc(incidentId);
    }
}
