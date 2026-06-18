package com.rootlens.dashboard.specification;

import com.rootlens.dashboard.entity.Incident;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public class IncidentSpecification {

    private IncidentSpecification() {}

    public static Specification<Incident> withFilters(
            String severity, String status, String service) {
        return Specification
                .where(hasField("severity", severity))
                .and(hasField("status", status))
                .and(servicesImpactedContains(service));
    }

    private static Specification<Incident> hasField(String field, String value) {
        if (value == null || value.isBlank()) return null;
        return (Root<Incident> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get(field), value.toUpperCase());
    }

    // services_impacted is stored as comma-joined TEXT via StringListConverter
    private static Specification<Incident> servicesImpactedContains(String service) {
        if (service == null || service.isBlank()) return null;
        return (Root<Incident> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.like(cb.lower(root.get("servicesImpacted").as(String.class)),
                        "%" + service.toLowerCase() + "%");
    }
}
