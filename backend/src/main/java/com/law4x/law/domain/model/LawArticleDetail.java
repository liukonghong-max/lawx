package com.law4x.law.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record LawArticleDetail(
        UUID articleId,
        String documentTitle,
        String lawType,
        String issuer,
        LocalDate publishDate,
        LocalDate effectiveDate,
        String documentStatus,
        String sourceUrl,
        String bookTitle,
        String chapterTitle,
        String sectionTitle,
        String articleNo,
        int articleOrder,
        String content,
        String fullPath,
        String effectiveStatus
) {
}
