package com.law4x.rag.interfaces.rest;

import com.law4x.common.interfaces.rest.ApiResponse;
import com.law4x.rag.application.GenerateMissingArticleEmbeddingsUseCase;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rag/embeddings")
public class RagEmbeddingAdminController {

    private final GenerateMissingArticleEmbeddingsUseCase generateMissingArticleEmbeddingsUseCase;
    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    public RagEmbeddingAdminController(
            GenerateMissingArticleEmbeddingsUseCase generateMissingArticleEmbeddingsUseCase,
            DashScopeEmbeddingProperties dashScopeEmbeddingProperties
    ) {
        this.generateMissingArticleEmbeddingsUseCase = generateMissingArticleEmbeddingsUseCase;
        this.dashScopeEmbeddingProperties = dashScopeEmbeddingProperties;
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateEmbeddingsResponse> generate(
            @RequestParam(required = false) String embeddingModel,
            @RequestParam(required = false) Integer limit
    ) {
        String selectedModel = selectEmbeddingModel(embeddingModel);
        GenerateMissingArticleEmbeddingsUseCase.GenerateResult result =
                generateMissingArticleEmbeddingsUseCase.generate(selectedModel, limit);
        return ApiResponse.success(new GenerateEmbeddingsResponse(
                selectedModel,
                limit,
                result.scanned(),
                result.generated()
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

    public record GenerateEmbeddingsResponse(
            String embeddingModel,
            Integer limit,
            int scanned,
            int generated
    ) {
    }
}
