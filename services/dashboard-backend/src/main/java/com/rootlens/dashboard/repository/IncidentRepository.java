package com.rootlens.dashboard.repository;

import com.rootlens.dashboard.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, String>,
        JpaSpecificationExecutor<Incident> {

    Page<Incident> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Incident> findByStatusNot(String status);
}
