package com.law4x.law.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record LawDocumentSummary(
        UUID documentId,
        String title,
        String lawType,
        String issuer,
        LocalDate publishDate,
        LocalDate effectiveDate,
        String status,
        int articleCount
) {
}
