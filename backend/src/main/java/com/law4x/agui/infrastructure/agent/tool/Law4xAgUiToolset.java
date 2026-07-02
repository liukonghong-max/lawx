package com.law4x.agui.infrastructure.agent.tool;

import com.law4x.agui.application.service.CitationValidationService;
import com.law4x.law.application.GetLawArticleDetailUseCase;
import com.law4x.law.application.SearchLawArticlesUseCase;
import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawArticleSearchResult;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Law4xAgUiToolset {

    private final SearchLawArticlesUseCase searchLawArticlesUseCase;
    private final GetLawArticleDetailUseCase getLawArticleDetailUseCase;
    private final CitationValidationService citationValidationService;

    public Law4xAgUiToolset(
            SearchLawArticlesUseCase searchLawArticlesUseCase,
            GetLawArticleDetailUseCase getLawArticleDetailUseCase,
            CitationValidationService citationValidationService
    ) {
        this.searchLawArticlesUseCase = searchLawArticlesUseCase;
        this.getLawArticleDetailUseCase = getLawArticleDetailUseCase;
        this.citationValidationService = citationValidationService;
    }

    public SearchLawArticlesResult searchLawArticles(String query, Integer limit) {
        List<SearchLawArticleItem> items = searchLawArticlesUseCase.search(query, limit).stream()
                .map(SearchLawArticleItem::from)
                .toList();
        return new SearchLawArticlesResult(items);
    }

    @Tool(
            name = "getArticleDetail",
            description = "根据 articleId 获取完整法条正文、章节路径、来源链接和生效状态。",
            readOnly = true
    )
    public GetArticleDetailResult getArticleDetail(
            @ToolParam(name = "articleId", description = "法条 articleId，必须是 UUID") String articleId
    ) {
        return getLawArticleDetailUseCase.get(articleId)
                .map(detail -> new GetArticleDetailResult(true, LawArticleDetailItem.from(detail)))
                .orElseGet(() -> new GetArticleDetailResult(false, null));
    }

    @Tool(
            name = "validateCitations",
            description = "校验回答中的引用是否来自允许的 articleId 列表，避免编造引用。",
            readOnly = true
    )
    public ValidateCitationsResult validateCitations(
            @ToolParam(name = "citationIds", description = "回答中实际引用的 articleId 列表") List<String> citationIds,
            @ToolParam(name = "allowedCitationIds", description = "检索结果允许引用的 articleId 列表") List<String> allowedCitationIds
    ) {
        CitationValidationService.ValidationResult result =
                citationValidationService.validate(citationIds, allowedCitationIds);
        return new ValidateCitationsResult(
                result.valid(),
                result.validCitationIds(),
                result.invalidCitationIds(),
                result.unsupportedCitationIds(),
                result.missingAllowedCitationIds()
        );
    }

    public record SearchLawArticlesResult(List<SearchLawArticleItem> items) {
    }

    public record SearchLawArticleItem(
            String articleId,
            String documentTitle,
            String articleNo,
            String fullPath,
            String preview,
            String score
    ) {
        private static SearchLawArticleItem from(LawArticleSearchResult result) {
            return new SearchLawArticleItem(
                    result.articleId().toString(),
                    result.documentTitle(),
                    result.articleNo(),
                    result.fullPath(),
                    result.preview(),
                    result.score() == null ? null : result.score().toPlainString()
            );
        }
    }

    public record GetArticleDetailResult(
            boolean found,
            LawArticleDetailItem article
    ) {
    }

    public record LawArticleDetailItem(
            String articleId,
            String documentTitle,
            String lawType,
            String issuer,
            String publishDate,
            String effectiveDate,
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
        private static LawArticleDetailItem from(LawArticleDetail detail) {
            return new LawArticleDetailItem(
                    detail.articleId().toString(),
                    detail.documentTitle(),
                    detail.lawType(),
                    detail.issuer(),
                    detail.publishDate() == null ? null : detail.publishDate().toString(),
                    detail.effectiveDate() == null ? null : detail.effectiveDate().toString(),
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

    public record ValidateCitationsResult(
            boolean valid,
            List<String> validCitationIds,
            List<String> invalidCitationIds,
            List<String> unsupportedCitationIds,
            List<String> missingAllowedCitationIds
    ) {
    }
}
