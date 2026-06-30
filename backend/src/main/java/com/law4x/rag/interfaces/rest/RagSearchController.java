package com.law4x.rag.interfaces.rest;

import com.law4x.common.interfaces.rest.ApiResponse;
import com.law4x.rag.application.HybridSearchUseCase;
import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagSearchController {

    private final HybridSearchUseCase hybridSearchUseCase;

    public RagSearchController(HybridSearchUseCase hybridSearchUseCase) {
        this.hybridSearchUseCase = hybridSearchUseCase;
    }

    @GetMapping("/search")
    public ApiResponse<RagSearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) Integer limit
    ) {
        List<RagSearchItemResponse> items = hybridSearchUseCase.search(query, limit)
                .stream()
                .map(RagSearchItemResponse::from)
                .toList();
        return ApiResponse.success(new RagSearchResponse(items));
    }

    public record RagSearchResponse(List<RagSearchItemResponse> items) {
    }

    public record RagSearchItemResponse(
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
        private static RagSearchItemResponse from(RagSearchResult result) {
            return new RagSearchItemResponse(
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
