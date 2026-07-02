package com.law4x.agui.infrastructure.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.agui.application.service.CitationValidationService;
import com.law4x.law.application.GetLawArticleDetailUseCase;
import com.law4x.law.application.SearchLawArticlesUseCase;
import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawArticleSearchResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Law4xAgUiToolsetTest {

    @Test
    void searchLawArticlesReturnsStructuredItems() {
        FakeSearchLawArticlesUseCase searchUseCase = new FakeSearchLawArticlesUseCase();
        searchUseCase.results = List.of(new LawArticleSearchResult(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "中华人民共和国民法典",
                "第六百七十五条",
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                "借款人应当按照约定的期限返还借款。",
                new BigDecimal("42.50")
        ));
        Law4xAgUiToolset toolset = new Law4xAgUiToolset(
                searchUseCase,
                new FakeGetLawArticleDetailUseCase(),
                new CitationValidationService()
        );

        Law4xAgUiToolset.SearchLawArticlesResult result = toolset.searchLawArticles("别人欠钱不还怎么办", 5);

        assertThat(searchUseCase.query).isEqualTo("别人欠钱不还怎么办");
        assertThat(searchUseCase.limit).isEqualTo(5);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).articleId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(result.items().get(0).documentTitle()).isEqualTo("中华人民共和国民法典");
    }

    @Test
    void getArticleDetailReturnsStructuredPayload() {
        FakeGetLawArticleDetailUseCase detailUseCase = new FakeGetLawArticleDetailUseCase();
        detailUseCase.detail = Optional.of(new LawArticleDetail(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "中华人民共和国民法典",
                "法律",
                "全国人民代表大会",
                LocalDate.of(2020, 5, 28),
                LocalDate.of(2021, 1, 1),
                "现行有效",
                "https://example.com/law",
                "第三编 合同",
                "第十二章 借款合同",
                null,
                "第六百七十五条",
                675,
                "借款人应当按照约定的期限返还借款。",
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                "effective"
        ));
        Law4xAgUiToolset toolset = new Law4xAgUiToolset(
                new FakeSearchLawArticlesUseCase(),
                detailUseCase,
                new CitationValidationService()
        );

        Law4xAgUiToolset.GetArticleDetailResult result =
                toolset.getArticleDetail("22222222-2222-2222-2222-222222222222");

        assertThat(detailUseCase.articleId).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(result.found()).isTrue();
        assertThat(result.article().articleNo()).isEqualTo("第六百七十五条");
        assertThat(result.article().content()).contains("返还借款");
    }

    @Test
    void validateCitationsReportsInvalidAndMissingItems() {
        Law4xAgUiToolset toolset = new Law4xAgUiToolset(
                new FakeSearchLawArticlesUseCase(),
                new FakeGetLawArticleDetailUseCase(),
                new CitationValidationService()
        );

        Law4xAgUiToolset.ValidateCitationsResult result = toolset.validateCitations(
                List.of(
                        "11111111-1111-1111-1111-111111111111",
                        "not-a-uuid",
                        "33333333-3333-3333-3333-333333333333"
                ),
                List.of(
                        "11111111-1111-1111-1111-111111111111",
                        "22222222-2222-2222-2222-222222222222"
                )
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.invalidCitationIds()).containsExactly("not-a-uuid");
        assertThat(result.unsupportedCitationIds()).containsExactly("33333333-3333-3333-3333-333333333333");
        assertThat(result.missingAllowedCitationIds()).containsExactly("22222222-2222-2222-2222-222222222222");
    }

    private static final class FakeSearchLawArticlesUseCase extends SearchLawArticlesUseCase {
        private String query;
        private Integer limit;
        private List<LawArticleSearchResult> results = List.of();

        private FakeSearchLawArticlesUseCase() {
            super(null);
        }

        @Override
        public List<LawArticleSearchResult> search(String query, Integer limit) {
            this.query = query;
            this.limit = limit;
            return results;
        }
    }

    private static final class FakeGetLawArticleDetailUseCase extends GetLawArticleDetailUseCase {
        private String articleId;
        private Optional<LawArticleDetail> detail = Optional.empty();

        private FakeGetLawArticleDetailUseCase() {
            super(null);
        }

        @Override
        public Optional<LawArticleDetail> get(String articleId) {
            this.articleId = articleId;
            return detail;
        }
    }
}
