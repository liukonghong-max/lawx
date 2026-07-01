package com.law4x.law.domain.model;

import java.util.UUID;

public record LawDocumentArticleItem(
        UUID articleId,
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
