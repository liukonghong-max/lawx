package com.law4x.rag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HybridSearchUseCaseTest {

    @Test
    void rejectsBlankQuery() {
        HybridSearchUseCase useCase = new HybridSearchUseCase(new FakeLawArticleRepository());

        assertThatThrownBy(() -> useCase.search("  ", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query must not be blank");
    }

    @Test
    void wrapsKeywordSearchAsRagResults() {
        UUID articleId = UUID.randomUUID();
        FakeLawArticleRepository repository = new FakeLawArticleRepository();
        repository.results.add(newResult(
                articleId,
                "中华人民共和国民法典",
                "第六百七十五条",
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                "借款人应当按照约定的期限返还借款。",
                new BigDecimal("42.50")
        ));
        HybridSearchUseCase useCase = new HybridSearchUseCase(repository);

        List<RagSearchResult> results = useCase.search(" 别人欠钱不还怎么办 ", 500);

        assertThat(repository.lastQuery).isEqualTo("别人欠钱不还怎么办");
        assertThat(repository.lastLimit).isEqualTo(20);
        assertThat(results).hasSize(1);
        RagSearchResult result = results.get(0);
        assertThat(result.articleId()).isEqualTo(articleId);
        assertThat(result.documentTitle()).isEqualTo("中华人民共和国民法典");
        assertThat(result.articleNo()).isEqualTo("第六百七十五条");
        assertThat(result.matchType()).isEqualTo("keyword");
        assertThat(result.keywordScore()).isEqualByComparingTo("42.50");
        assertThat(result.vectorScore()).isEqualByComparingTo("0");
        assertThat(result.finalScore()).isEqualByComparingTo("42.50");
        assertThat(result.reason()).isEqualTo("当前使用关键词检索命中，后续会叠加向量召回和 rerank。");
    }

    @Test
    void returnsEmptyWhenOriginalQueryHasNoResults() {
        FakeLawArticleRepository repository = new FakeLawArticleRepository();
        HybridSearchUseCase useCase = new HybridSearchUseCase(repository);

        List<RagSearchResult> results = useCase.search("别人欠钱不还怎么办", 5);

        assertThat(repository.queries).containsExactly("别人欠钱不还怎么办");
        assertThat(results).isEmpty();
    }

    private static LawArticleSearchResult newResult(
            UUID articleId,
            String documentTitle,
            String articleNo,
            String fullPath,
            String preview,
            BigDecimal score
    ) {
        return new LawArticleSearchResult(articleId, documentTitle, articleNo, fullPath, preview, score);
    }

    private static class FakeLawArticleRepository implements LawArticleRepository {
        private final List<LawArticleSearchResult> results = new ArrayList<>();
        private final List<String> queries = new ArrayList<>();
        private String lastQuery;
        private int lastLimit;

        @Override
        public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
            queries.add(query);
            lastQuery = query;
            lastLimit = limit;
            return results;
        }

        @Override
        public Optional<com.law4x.law.domain.model.LawArticleDetail> findArticleDetail(UUID articleId) {
            return Optional.empty();
        }
    }
}
