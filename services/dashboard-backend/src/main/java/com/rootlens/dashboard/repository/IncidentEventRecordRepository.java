package com.rootlens.dashboard.repository;

import com.rootlens.dashboard.entity.IncidentEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentEventRecordRepository extends JpaRepository<IncidentEventRecord, Long> {

    List<IncidentEventRecord> findByIncidentIdOrderByReceivedAtAsc(String incidentId);
}
