package com.law4x.agui.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CitationValidationService {

    public ValidationResult validate(List<String> citationIds, List<String> allowedCitationIds) {
        Set<String> normalizedAllowedIds = normalizeAllowedIds(allowedCitationIds);
        Set<String> validCitationIds = new LinkedHashSet<>();
        Set<String> invalidCitationIds = new LinkedHashSet<>();
        Set<String> unsupportedCitationIds = new LinkedHashSet<>();

        for (String citationId : citationIds == null ? List.<String>of() : citationIds) {
            String normalizedCitationId = normalize(citationId);
            if (normalizedCitationId == null) {
                continue;
            }
            if (!isUuid(normalizedCitationId)) {
                invalidCitationIds.add(normalizedCitationId);
                continue;
            }
            if (normalizedAllowedIds.contains(normalizedCitationId)) {
                validCitationIds.add(normalizedCitationId);
                continue;
            }
            unsupportedCitationIds.add(normalizedCitationId);
        }

        Set<String> missingAllowedIds = new LinkedHashSet<>(normalizedAllowedIds);
        missingAllowedIds.removeAll(validCitationIds);
        boolean valid = invalidCitationIds.isEmpty() && unsupportedCitationIds.isEmpty() && missingAllowedIds.isEmpty();

        return new ValidationResult(
                valid,
                List.copyOf(validCitationIds),
                List.copyOf(invalidCitationIds),
                List.copyOf(unsupportedCitationIds),
                List.copyOf(missingAllowedIds)
        );
    }

    private Set<String> normalizeAllowedIds(List<String> allowedCitationIds) {
        Set<String> normalizedIds = new LinkedHashSet<>();
        for (String citationId : allowedCitationIds == null ? List.<String>of() : allowedCitationIds) {
            String normalizedCitationId = normalize(citationId);
            if (normalizedCitationId != null && isUuid(normalizedCitationId)) {
                normalizedIds.add(normalizedCitationId);
            }
        }
        return normalizedIds;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public record ValidationResult(
            boolean valid,
            List<String> validCitationIds,
            List<String> invalidCitationIds,
            List<String> unsupportedCitationIds,
            List<String> missingAllowedCitationIds
    ) {
    }
}
