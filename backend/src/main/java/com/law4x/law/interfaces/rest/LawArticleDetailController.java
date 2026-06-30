package com.law4x.law.interfaces.rest;

import com.law4x.common.interfaces.rest.ApiResponse;
import com.law4x.law.application.GetLawArticleDetailUseCase;
import com.law4x.law.domain.model.LawArticleDetail;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/law/articles")
public class LawArticleDetailController {

    private final GetLawArticleDetailUseCase getLawArticleDetailUseCase;

    public LawArticleDetailController(GetLawArticleDetailUseCase getLawArticleDetailUseCase) {
        this.getLawArticleDetailUseCase = getLawArticleDetailUseCase;
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> get(@PathVariable String articleId) {
        return getLawArticleDetailUseCase.get(articleId)
                .map(ArticleDetailResponse::from)
                .map(ApiResponse::success)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("NOT_FOUND", "article not found")));
    }

    public record ArticleDetailResponse(
            UUID articleId,
            String documentTitle,
            String lawType,
            String issuer,
            LocalDate publishDate,
            LocalDate effectiveDate,
            String documentStatus,
            String sourceUrl,
            String bookTitle,
            String chapterTitle,
            String sectionTitle,
            String articleNo,
            int articleOrder,
            String content,
            String fullPath,
            String effectiveStatus
    ) {
        private static ArticleDetailResponse from(LawArticleDetail detail) {
            return new ArticleDetailResponse(
                    detail.articleId(),
                    detail.documentTitle(),
                    detail.lawType(),
                    detail.issuer(),
                    detail.publishDate(),
                    detail.effectiveDate(),
                    detail.documentStatus(),
                    detail.sourceUrl(),
                    detail.bookTitle(),
                    detail.chapterTitle(),
                    detail.sectionTitle(),
                    detail.articleNo(),
                    detail.articleOrder(),
                    detail.content(),
                    detail.fullPath(),
                    detail.effectiveStatus()
            );
        }
    }
}
