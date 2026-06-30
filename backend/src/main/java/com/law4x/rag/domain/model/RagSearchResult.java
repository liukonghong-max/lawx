package com.law4x.rag.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record RagSearchResult(
        UUID articleId,
        String documentTitle,
        String articleNo,
        String fullPath,
        String preview,
        String matchType,
        BigDecimal keywordScore,
        BigDecimal vectorScore,
        BigDecimal finalScore,
        String reason
) {
}
