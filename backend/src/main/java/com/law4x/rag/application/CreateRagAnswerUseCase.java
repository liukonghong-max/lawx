package com.law4x.rag.application;

import com.law4x.rag.domain.model.RagAnswer;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.RagAnswerClient;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CreateRagAnswerUseCase {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final CreateRagTestRunUseCase createRagTestRunUseCase;
    private final RagAnswerClient ragAnswerClient;

    public CreateRagAnswerUseCase(
            CreateRagTestRunUseCase createRagTestRunUseCase,
            RagAnswerClient ragAnswerClient
    ) {
        this.createRagTestRunUseCase = createRagTestRunUseCase;
        this.ragAnswerClient = ragAnswerClient;
    }

    public RagAnswer answer(String query, String embeddingModel, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        int normalizedLimit = normalizeLimit(limit);
        CreateRagTestRunUseCase.CreateResult testRun = createRagTestRunUseCase.create(
                normalizedQuery,
                embeddingModel,
                normalizedLimit
        );
        List<RagSearchResult> evidence = testRun.results();
        return new RagAnswer(
                testRun.runId(),
                ragAnswerClient.answer(normalizedQuery, evidence),
                evidence.stream().map(CreateRagAnswerUseCase::toCitation).toList()
        );
    }

    private static RagAnswer.Citation toCitation(RagSearchResult result) {
        return new RagAnswer.Citation(
                result.articleId(),
                result.documentTitle(),
                result.articleNo(),
                result.fullPath(),
                result.preview()
        );
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return query.trim();
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
}
