package com.law4x.rag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.law4x.rag.domain.model.EmbeddableLawArticle;
import com.law4x.rag.domain.repository.EmbeddingClient;
import com.law4x.rag.domain.repository.LawArticleEmbeddingRepository;
import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GenerateMissingArticleEmbeddingsUseCaseTest {

    @Test
    void rejectsBlankEmbeddingModel() {
        GenerateMissingArticleEmbeddingsUseCase useCase = new GenerateMissingArticleEmbeddingsUseCase(
                new FakeLawArticleEmbeddingRepository(),
                new FakeEmbeddingClient()
        );

        assertThatThrownBy(() -> useCase.generate(" ", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("embeddingModel must not be blank");
    }

    @Test
    void generatesAndStoresEmbeddingsForMissingArticles() {
        UUID articleId = UUID.randomUUID();
        FakeLawArticleEmbeddingRepository repository = new FakeLawArticleEmbeddingRepository();
        repository.missingArticles.add(new EmbeddableLawArticle(
                articleId,
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                "借款人应当按照约定的期限返还借款。",
                "hash-675"
        ));
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        embeddingClient.embedding = List.of(new BigDecimal("0.25"), new BigDecimal("0.75"));
        GenerateMissingArticleEmbeddingsUseCase useCase = new GenerateMissingArticleEmbeddingsUseCase(
                repository,
                embeddingClient
        );

        GenerateMissingArticleEmbeddingsUseCase.GenerateResult result = useCase.generate("test-model", 100);

        assertThat(repository.lastFindModel).isEqualTo("test-model");
        assertThat(repository.lastFindLimit).isEqualTo(100);
        assertThat(embeddingClient.requests).containsExactly(
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条\n借款人应当按照约定的期限返还借款。"
        );
        assertThat(repository.savedEmbeddings).hasSize(1);
        FakeLawArticleEmbeddingRepository.SavedEmbedding saved = repository.savedEmbeddings.get(0);
        assertThat(saved.articleId()).isEqualTo(articleId);
        assertThat(saved.embeddingModel()).isEqualTo("test-model");
        assertThat(saved.contentHash()).isEqualTo("hash-675");
        assertThat(saved.embedding()).containsExactly(new BigDecimal("0.25"), new BigDecimal("0.75"));
        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.generated()).isEqualTo(1);
    }

    private static class FakeEmbeddingClient implements EmbeddingClient {
        private final List<String> requests = new ArrayList<>();
        private List<BigDecimal> embedding = List.of();

        @Override
        public List<BigDecimal> embed(String text, String embeddingModel) {
            requests.add(text);
            return embedding;
        }
    }

    private static class FakeLawArticleEmbeddingRepository implements LawArticleEmbeddingRepository {
        private final List<EmbeddableLawArticle> missingArticles = new ArrayList<>();
        private final List<SavedEmbedding> savedEmbeddings = new ArrayList<>();
        private String lastFindModel;
        private int lastFindLimit;

        @Override
        public List<RagSearchResult> searchSimilarArticles(
                String embeddingModel,
                List<BigDecimal> queryEmbedding,
                int limit
        ) {
            return List.of();
        }

        @Override
        public List<EmbeddableLawArticle> findArticlesMissingEmbeddings(String embeddingModel, int limit) {
            lastFindModel = embeddingModel;
            lastFindLimit = limit;
            return missingArticles;
        }

        @Override
        public void upsertArticleEmbedding(
                UUID articleId,
                String embeddingModel,
                String contentHash,
                List<BigDecimal> embedding
        ) {
            savedEmbeddings.add(new SavedEmbedding(articleId, embeddingModel, contentHash, embedding));
        }

        private record SavedEmbedding(
                UUID articleId,
                String embeddingModel,
                String contentHash,
                List<BigDecimal> embedding
        ) {
        }
    }
}
