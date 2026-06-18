package com.rootlens.incidentengine.repository;

import com.rootlens.incidentengine.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, String> {
}
