package com.law4x.law.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchLawArticlesUseCaseTest {

    @Test
    void rejectsBlankQuery() {
        SearchLawArticlesUseCase useCase = new SearchLawArticlesUseCase(new FakeLawArticleRepository());

        assertThatThrownBy(() -> useCase.search("  ", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query must not be blank");
    }

    @Test
    void normalizesLimitAndDelegatesToRepository() {
        FakeLawArticleRepository repository = new FakeLawArticleRepository();
        repository.results.add(new LawArticleSearchResult(
                UUID.randomUUID(),
                "中华人民共和国民法典",
                "第五百七十七条",
                "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
                "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
                new BigDecimal("116.21")
        ));
        SearchLawArticlesUseCase useCase = new SearchLawArticlesUseCase(repository);

        List<LawArticleSearchResult> results = useCase.search(" 第五百七十七条 ", 500);

        assertThat(repository.lastQuery).isEqualTo("第五百七十七条");
        assertThat(repository.lastLimit).isEqualTo(100);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).articleNo()).isEqualTo("第五百七十七条");
    }

    private static class FakeLawArticleRepository implements LawArticleRepository {
        private final List<LawArticleSearchResult> results = new ArrayList<>();
        private String lastQuery;
        private int lastLimit;

        @Override
        public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
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
