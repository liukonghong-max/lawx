package com.law4x.law.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record LawArticleSearchResult(
        UUID articleId,
        String documentTitle,
        String articleNo,
        String fullPath,
        String preview,
        BigDecimal score
) {
}
