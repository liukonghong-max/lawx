package com.law4x.rag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.EmbeddingClient;
import com.law4x.rag.domain.repository.LawArticleEmbeddingRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HybridSearchUseCaseTest {

    @Test
    void rejectsBlankQuery() {
        HybridSearchUseCase useCase = newUseCase(new FakeLawArticleRepository());

        assertThatThrownBy(() -> useCase.search("  ", "text-embedding-v4", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query must not be blank");
    }

    @Test
    void rejectsBlankEmbeddingModel() {
        HybridSearchUseCase useCase = newUseCase(new FakeLawArticleRepository());

        assertThatThrownBy(() -> useCase.search("别人欠钱不还怎么办", "  ", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("embeddingModel must not be blank");
    }

    @Test
    void searchesSimilarArticlesWithQueryEmbedding() {
        UUID articleId = UUID.randomUUID();
        FakeLawArticleEmbeddingRepository embeddingRepository = new FakeLawArticleEmbeddingRepository();
        embeddingRepository.results.add(new RagSearchResult(
                articleId,
                "中华人民共和国民法典",
                "第六百七十五条",
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                "借款人应当按照约定的期限返还借款。",
                "vector",
                BigDecimal.ZERO,
                new BigDecimal("0.91"),
                new BigDecimal("0.91"),
                "当前使用 pgvector 向量检索命中。"
        ));
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        HybridSearchUseCase useCase = new HybridSearchUseCase(
                new FakeLawArticleRepository(),
                embeddingRepository,
                embeddingClient
        );

        List<RagSearchResult> results = useCase.search(" 别人欠钱不还怎么办 ", "text-embedding-v4", 500);

        assertThat(embeddingClient.lastText).isEqualTo("别人欠钱不还怎么办");
        assertThat(embeddingClient.lastEmbeddingModel).isEqualTo("text-embedding-v4");
        assertThat(embeddingRepository.lastEmbeddingModel).isEqualTo("text-embedding-v4");
        assertThat(embeddingRepository.lastQueryEmbedding).containsExactly(
                new BigDecimal("0.1"),
                new BigDecimal("0.2"),
                new BigDecimal("0.3")
        );
        assertThat(embeddingRepository.lastLimit).isEqualTo(20);
        assertThat(results).hasSize(1);
        RagSearchResult result = results.get(0);
        assertThat(result.articleId()).isEqualTo(articleId);
        assertThat(result.matchType()).isEqualTo("vector");
        assertThat(result.vectorScore()).isEqualByComparingTo("0.91");
    }

    @Test
    void mergesKeywordAndVectorResultsByArticle() {
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
        FakeLawArticleEmbeddingRepository embeddingRepository = new FakeLawArticleEmbeddingRepository();
        embeddingRepository.results.add(new RagSearchResult(
                articleId,
                "中华人民共和国民法典",
                "第六百七十五条",
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                "借款人应当按照约定的期限返还借款。",
                "vector",
                BigDecimal.ZERO,
                new BigDecimal("0.91"),
                new BigDecimal("0.91"),
                "当前使用 pgvector 向量检索命中。"
        ));
        HybridSearchUseCase useCase = new HybridSearchUseCase(repository, embeddingRepository, new FakeEmbeddingClient());

        List<RagSearchResult> results = useCase.search("别人欠钱不还怎么办", "text-embedding-v4", 5);

        assertThat(repository.queries).containsExactly("别人欠钱不还怎么办");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchType()).isEqualTo("hybrid");
        assertThat(results.get(0).keywordScore()).isEqualByComparingTo("42.50");
        assertThat(results.get(0).vectorScore()).isEqualByComparingTo("0.91");
        assertThat(results.get(0).finalScore()).isEqualByComparingTo("43.41");
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

    private static HybridSearchUseCase newUseCase(FakeLawArticleRepository repository) {
        return new HybridSearchUseCase(
                repository,
                new FakeLawArticleEmbeddingRepository(),
                new FakeEmbeddingClient()
        );
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

    private static class FakeLawArticleEmbeddingRepository implements LawArticleEmbeddingRepository {
        private final List<RagSearchResult> results = new ArrayList<>();
        private String lastEmbeddingModel;
        private List<BigDecimal> lastQueryEmbedding;
        private int lastLimit;

        @Override
        public List<RagSearchResult> searchSimilarArticles(String embeddingModel, List<BigDecimal> queryEmbedding, int limit) {
            lastEmbeddingModel = embeddingModel;
            lastQueryEmbedding = queryEmbedding;
            lastLimit = limit;
            return results;
        }

        @Override
        public List<com.law4x.rag.domain.model.EmbeddableLawArticle> findArticlesMissingEmbeddings(
                String embeddingModel,
                int limit
        ) {
            return List.of();
        }

        @Override
        public void upsertArticleEmbedding(
                UUID articleId,
                String embeddingModel,
                String contentHash,
                List<BigDecimal> embedding
        ) {
        }
    }

    private static class FakeEmbeddingClient implements EmbeddingClient {
        private String lastText;
        private String lastEmbeddingModel;

        @Override
        public List<BigDecimal> embed(String text, String embeddingModel) {
            lastText = text;
            lastEmbeddingModel = embeddingModel;
            return List.of(
                    new BigDecimal("0.1"),
                    new BigDecimal("0.2"),
                    new BigDecimal("0.3")
            );
        }
    }
}
