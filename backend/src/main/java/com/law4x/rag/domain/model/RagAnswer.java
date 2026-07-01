package com.law4x.rag.domain.model;

import java.util.List;
import java.util.UUID;

public record RagAnswer(
        UUID runId,
        String answer,
        List<Citation> citations,
        List<AnswerSegment> answerSegments
) {
    public record Citation(
            UUID articleId,
            String documentTitle,
            String articleNo,
            String fullPath,
            String quotedText
    ) {
    }

    public record AnswerSegment(
            String id,
            String text,
            List<UUID> citationIds
    ) {
    }

    // 兼容老的构造方法
    public RagAnswer(UUID runId, String answer, List<Citation> citations) {
        this(runId, answer, citations, List.of());
    }
}
