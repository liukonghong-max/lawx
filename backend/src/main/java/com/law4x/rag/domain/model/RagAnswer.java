package com.law4x.rag.domain.model;

import java.util.List;
import java.util.UUID;

public record RagAnswer(
        UUID runId,
        String answer,
        List<Citation> citations
) {
    public record Citation(
            UUID articleId,
            String documentTitle,
            String articleNo,
            String fullPath,
            String quotedText
    ) {
    }
}
