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

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 1000;
    private static final int DEFAULT_MAX_BATCHES = 20;
    private static final int MAX_BATCHES = 1000;

    private final GenerateMissingArticleEmbeddingsUseCase generateMissingArticleEmbeddingsUseCase;
    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    public RagEmbeddingAdminController(
            GenerateMissingArticleEmbeddingsUseCase generateMissingArticleEmbeddingsUseCase,
            DashScopeEmbeddingProperties dashScopeEmbeddingProperties
    ) {
        this.generateMissingArticleEmbeddingsUseCase = generateMissingArticleEmbeddingsUseCase;
        this.dashScopeEmbeddingProperties = dashScopeEmbeddingProperties;
    }

    @PostMapping("/generate-all")
    public ApiResponse<GenerateAllEmbeddingsResponse> generateAll(
            @RequestParam(required = false) String embeddingModel,
            @RequestParam(required = false) Integer batchSize,
            @RequestParam(required = false) Integer maxBatches
    ) {
        String selectedModel = selectEmbeddingModel(embeddingModel);
        int normalizedBatchSize = normalizeBatchSize(batchSize);
        int normalizedMaxBatches = normalizeMaxBatches(maxBatches);
        int batches = 0;
        int scanned = 0;
        int generated = 0;
        boolean finished = false;

        for (int i = 0; i < normalizedMaxBatches; i++) {
            GenerateMissingArticleEmbeddingsUseCase.GenerateResult result =
                    generateMissingArticleEmbeddingsUseCase.generate(selectedModel, normalizedBatchSize);
            if (result.scanned() == 0 && result.generated() == 0) {
                finished = true;
                break;
            }
            batches++;
            scanned += result.scanned();
            generated += result.generated();
        }

        return ApiResponse.success(new GenerateAllEmbeddingsResponse(
                selectedModel,
                normalizedBatchSize,
                normalizedMaxBatches,
                batches,
                scanned,
                generated,
                finished
        ));
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

    private int normalizeBatchSize(Integer batchSize) {
        if (batchSize == null) {
            return DEFAULT_BATCH_SIZE;
        }
        if (batchSize < 1) {
            return 1;
        }
        return Math.min(batchSize, MAX_BATCH_SIZE);
    }

    private int normalizeMaxBatches(Integer maxBatches) {
        if (maxBatches == null) {
            return DEFAULT_MAX_BATCHES;
        }
        if (maxBatches < 1) {
            return 1;
        }
        return Math.min(maxBatches, MAX_BATCHES);
    }

    public record GenerateEmbeddingsResponse(
            String embeddingModel,
            Integer limit,
            int scanned,
            int generated
    ) {
    }

    public record GenerateAllEmbeddingsResponse(
            String embeddingModel,
            int batchSize,
            int maxBatches,
            int batches,
            int scanned,
            int generated,
            boolean finished
    ) {
    }
}
