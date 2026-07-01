package com.law4x.rag.interfaces.rest;

import com.law4x.common.interfaces.rest.ApiResponse;
import com.law4x.rag.application.CreateRagAnswerUseCase;
import com.law4x.rag.domain.model.RagAnswer;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/answer")
public class RagAnswerController {

    private final CreateRagAnswerUseCase createRagAnswerUseCase;
    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    public RagAnswerController(
            CreateRagAnswerUseCase createRagAnswerUseCase,
            DashScopeEmbeddingProperties dashScopeEmbeddingProperties
    ) {
        this.createRagAnswerUseCase = createRagAnswerUseCase;
        this.dashScopeEmbeddingProperties = dashScopeEmbeddingProperties;
    }

    @PostMapping
    public ApiResponse<RagAnswerResponse> answer(@RequestBody RagAnswerRequest request) {
        RagAnswer answer = createRagAnswerUseCase.answer(
                request.query(),
                selectEmbeddingModel(request.embeddingModel()),
                request.limit()
        );
        return ApiResponse.success(RagAnswerResponse.from(answer));
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

    public record RagAnswerRequest(
            String query,
            String embeddingModel,
            Integer limit
    ) {
    }

    public record RagAnswerResponse(
            UUID runId,
            String answer,
            List<CitationResponse> citations
    ) {
        private static RagAnswerResponse from(RagAnswer answer) {
            return new RagAnswerResponse(
                    answer.runId(),
                    answer.answer(),
                    answer.citations().stream().map(CitationResponse::from).toList()
            );
        }
    }

    public record CitationResponse(
            UUID articleId,
            String documentTitle,
            String articleNo,
            String fullPath,
            String quotedText
    ) {
        private static CitationResponse from(RagAnswer.Citation citation) {
            return new CitationResponse(
                    citation.articleId(),
                    citation.documentTitle(),
                    citation.articleNo(),
                    citation.fullPath(),
                    citation.quotedText()
            );
        }
    }
}
