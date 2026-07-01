package com.law4x.rag.interfaces.rest;

import com.law4x.common.interfaces.rest.ApiResponse;
import com.law4x.rag.application.CreateRagTestRunUseCase;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/test-runs")
public class RagTestRunController {

    private final CreateRagTestRunUseCase createRagTestRunUseCase;
    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    public RagTestRunController(
            CreateRagTestRunUseCase createRagTestRunUseCase,
            DashScopeEmbeddingProperties dashScopeEmbeddingProperties
    ) {
        this.createRagTestRunUseCase = createRagTestRunUseCase;
        this.dashScopeEmbeddingProperties = dashScopeEmbeddingProperties;
    }

    @PostMapping
    public ApiResponse<CreateRagTestRunResponse> create(@RequestBody CreateRagTestRunRequest request) {
        CreateRagTestRunUseCase.CreateResult result = createRagTestRunUseCase.create(
                request.query(),
                selectEmbeddingModel(request.embeddingModel()),
                request.limit()
        );
        return ApiResponse.success(new CreateRagTestRunResponse(
                result.runId(),
                result.results().stream().map(RagTestRunItemResponse::from).toList()
        ));
    }

    private String selectEmbeddingModel(String embeddingModel) {
        if (embeddingModel != null && !embeddingModel.trim().isBlank()) {
            return embeddingModel.trim();
        }
        String configuredModel = dashScopeEmbeddingProperties.getModelName();
        if (configuredModel == null || configuredModel.trim().isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        return configuredModel.trim();
    }

    public record CreateRagTestRunRequest(
            String query,
            String embeddingModel,
            Integer limit
    ) {
    }

    public record CreateRagTestRunResponse(
            UUID runId,
            List<RagTestRunItemResponse> items
    ) {
    }

    public record RagTestRunItemResponse(
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
        private static RagTestRunItemResponse from(RagSearchResult result) {
            return new RagTestRunItemResponse(
                    result.articleId(),
                    result.documentTitle(),
                    result.articleNo(),
                    result.fullPath(),
                    result.preview(),
                    result.matchType(),
                    result.keywordScore(),
                    result.vectorScore(),
                    result.finalScore(),
                    result.reason()
            );
        }
    }
}
