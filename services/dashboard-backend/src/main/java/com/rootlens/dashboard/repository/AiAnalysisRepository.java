package com.rootlens.dashboard.repository;

import com.rootlens.dashboard.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, String> {

    Optional<AiAnalysis> findByIncidentId(String incidentId);
}
