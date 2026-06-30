package com.law4x.rag.domain.model;

import java.util.UUID;

public record EmbeddableLawArticle(
        UUID articleId,
        String fullPath,
        String content,
        String contentHash
) {
    public String embeddingText() {
        if (fullPath == null || fullPath.isBlank()) {
            return content;
        }
        return fullPath + "\n" + content;
    }
}
