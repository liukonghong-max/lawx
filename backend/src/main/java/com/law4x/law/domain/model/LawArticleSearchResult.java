package com.law4x.law.domain.model;

import java.math.BigDecimal;

public record LawArticleSearchResult(
        String documentTitle,
        String articleNo,
        String fullPath,
        String preview,
        BigDecimal score
) {
}
