package com.rootlens.aianalysis.repository;

import com.rootlens.aianalysis.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, String> {

    Optional<AiAnalysis> findByIncidentId(String incidentId);
}
