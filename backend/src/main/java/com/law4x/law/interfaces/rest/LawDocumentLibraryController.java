package com.law4x.law.interfaces.rest;

import com.law4x.common.interfaces.rest.ApiResponse;
import com.law4x.law.application.ListLawDocumentArticlesUseCase;
import com.law4x.law.application.ListLawDocumentsUseCase;
import com.law4x.law.domain.model.LawDocumentArticleItem;
import com.law4x.law.domain.model.LawDocumentSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.law4x.law.domain.repository.LawArticleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/law/documents")
public class LawDocumentLibraryController {

    private final ListLawDocumentsUseCase listLawDocumentsUseCase;
    private final ListLawDocumentArticlesUseCase listLawDocumentArticlesUseCase;

    public LawDocumentLibraryController(
            ListLawDocumentsUseCase listLawDocumentsUseCase,
            ListLawDocumentArticlesUseCase listLawDocumentArticlesUseCase
    ) {
        this.listLawDocumentsUseCase = listLawDocumentsUseCase;
        this.listLawDocumentArticlesUseCase = listLawDocumentArticlesUseCase;
    }

    @GetMapping
    public ApiResponse<DocumentListResponse> listDocuments(@RequestParam(required = false) Integer limit) {
        List<DocumentSummaryResponse> items = listLawDocumentsUseCase.list(limit)
                .stream()
                .map(DocumentSummaryResponse::from)
                .toList();
        return ApiResponse.success(new DocumentListResponse(items));
    }

    @GetMapping("/{documentId}/articles")
    public ApiResponse<DocumentArticleListResponse> listDocumentArticles(
            @PathVariable String documentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        LawArticleRepository.PagedResult<LawDocumentArticleItem> result = listLawDocumentArticlesUseCase.list(documentId, page, pageSize);
        List<DocumentArticleResponse> items = result.items()
                .stream()
                .map(DocumentArticleResponse::from)
                .toList();
        return ApiResponse.success(new DocumentArticleListResponse(
                items,
                result.page(),
                result.pageSize(),
                result.total()
        ));
    }

    public record DocumentListResponse(List<DocumentSummaryResponse> items) {
    }

    public record DocumentArticleListResponse(
            List<DocumentArticleResponse> items,
            int page,
            int pageSize,
            long total
    ) {
    }

    public record DocumentSummaryResponse(
            UUID documentId,
            String title,
            String lawType,
            String issuer,
            LocalDate publishDate,
            LocalDate effectiveDate,
            String status,
            int articleCount
    ) {
        private static DocumentSummaryResponse from(LawDocumentSummary item) {
            return new DocumentSummaryResponse(
                    item.documentId(),
                    item.title(),
                    item.lawType(),
                    item.issuer(),
                    item.publishDate(),
                    item.effectiveDate(),
                    item.status(),
                    item.articleCount()
            );
        }
    }

    public record DocumentArticleResponse(
            UUID articleId,
            String bookTitle,
            String chapterTitle,
            String sectionTitle,
            String articleNo,
            int articleOrder,
            String content,
            String fullPath,
            String effectiveStatus
    ) {
        private static DocumentArticleResponse from(LawDocumentArticleItem item) {
            return new DocumentArticleResponse(
                    item.articleId(),
                    item.bookTitle(),
                    item.chapterTitle(),
                    item.sectionTitle(),
                    item.articleNo(),
                    item.articleOrder(),
                    item.content(),
                    item.fullPath(),
                    item.effectiveStatus()
            );
        }
    }
}
