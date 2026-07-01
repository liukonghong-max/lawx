package com.law4x.rag.domain.model;

import java.util.List;
import java.util.UUID;

public record RagTestRun(
        UUID id,
        String query,
        List<RagSearchResult> keywordResults,
        List<RagSearchResult> vectorResults,
        List<RagSearchResult> rerankResults,
        List<UUID> selectedArticleIds,
        Parameters parameters
) {
    public RagTestRun withId(UUID id) {
        return new RagTestRun(
                id,
                query,
                keywordResults,
                vectorResults,
                rerankResults,
                selectedArticleIds,
                parameters
        );
    }

    public record Parameters(
            String embeddingModel,
            int limit
    ) {
    }
}
