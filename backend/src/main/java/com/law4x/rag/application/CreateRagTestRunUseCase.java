package com.law4x.rag.application;

import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.model.RagTestRun;
import com.law4x.rag.domain.repository.RagTestRunRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CreateRagTestRunUseCase {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final HybridSearchUseCase hybridSearchUseCase;
    private final RagTestRunRepository ragTestRunRepository;

    public CreateRagTestRunUseCase(
            HybridSearchUseCase hybridSearchUseCase,
            RagTestRunRepository ragTestRunRepository
    ) {
        this.hybridSearchUseCase = hybridSearchUseCase;
        this.ragTestRunRepository = ragTestRunRepository;
    }

    public CreateResult create(String query, String embeddingModel, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        String normalizedEmbeddingModel = normalizeEmbeddingModel(embeddingModel);
        int normalizedLimit = normalizeLimit(limit);
        List<RagSearchResult> results = hybridSearchUseCase.search(
                normalizedQuery,
                normalizedEmbeddingModel,
                normalizedLimit
        );
        RagTestRun savedRun = ragTestRunRepository.save(new RagTestRun(
                null,
                normalizedQuery,
                filterResults(results, "keyword"),
                filterResults(results, "vector"),
                List.of(),
                results.stream().map(RagSearchResult::articleId).toList(),
                new RagTestRun.Parameters(normalizedEmbeddingModel, normalizedLimit)
        ));
        return new CreateResult(savedRun.id(), results);
    }

    private static List<RagSearchResult> filterResults(List<RagSearchResult> results, String matchType) {
        return results.stream()
                .filter(result -> matchType.equals(result.matchType()) || "hybrid".equals(result.matchType()))
                .toList();
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return query.trim();
    }

    private static String normalizeEmbeddingModel(String embeddingModel) {
        if (embeddingModel == null || embeddingModel.trim().isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        return embeddingModel.trim();
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    public record CreateResult(
            UUID runId,
            List<RagSearchResult> results
    ) {
    }
}
