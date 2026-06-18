package com.rootlens.incidentengine.repository;

import com.rootlens.incidentengine.entity.IncidentEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentEventRecordRepository extends JpaRepository<IncidentEventRecord, Long> {

    List<IncidentEventRecord> findByIncidentId(String incidentId);
}
