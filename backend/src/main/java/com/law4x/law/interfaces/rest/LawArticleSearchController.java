package com.law4x.law.interfaces.rest;

import com.law4x.law.application.SearchLawArticlesUseCase;
import com.law4x.law.domain.model.LawArticleSearchResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/law/articles")
public class LawArticleSearchController {

    private final SearchLawArticlesUseCase searchLawArticlesUseCase;

    public LawArticleSearchController(SearchLawArticlesUseCase searchLawArticlesUseCase) {
        this.searchLawArticlesUseCase = searchLawArticlesUseCase;
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam String query,
            @RequestParam(required = false) Integer limit
    ) {
        List<SearchItemResponse> items = searchLawArticlesUseCase.search(query, limit)
                .stream()
                .map(SearchItemResponse::from)
                .toList();
        return new SearchResponse(items);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    public record SearchResponse(List<SearchItemResponse> items) {
    }

    public record SearchItemResponse(
            String documentTitle,
            String articleNo,
            String fullPath,
            String preview,
            BigDecimal score
    ) {
        private static SearchItemResponse from(LawArticleSearchResult result) {
            return new SearchItemResponse(
                    result.documentTitle(),
                    result.articleNo(),
                    result.fullPath(),
                    result.preview(),
                    result.score()
            );
        }
    }

    public record ErrorResponse(String message) {
    }
}
