package com.law4x.rag.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.rag.application.GenerateMissingArticleEmbeddingsUseCase;
import com.law4x.rag.domain.model.EmbeddableLawArticle;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.EmbeddingClient;
import com.law4x.rag.domain.repository.LawArticleEmbeddingRepository;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RagEmbeddingAdminController.class)
@Import(RagEmbeddingAdminControllerTest.TestConfig.class)
class RagEmbeddingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeGenerateMissingArticleEmbeddingsUseCase generateMissingArticleEmbeddingsUseCase;

    @BeforeEach
    void setUp() {
        generateMissingArticleEmbeddingsUseCase.reset();
    }

    @Test
    void generatesMissingEmbeddingsWithDefaultModel() throws Exception {
        generateMissingArticleEmbeddingsUseCase.result =
                new GenerateMissingArticleEmbeddingsUseCase.GenerateResult(2, 2);

        mockMvc.perform(post("/api/admin/rag/embeddings/generate")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.embeddingModel").value("text-embedding-v4"))
                .andExpect(jsonPath("$.data.limit").value(20))
                .andExpect(jsonPath("$.data.scanned").value(2))
                .andExpect(jsonPath("$.data.generated").value(2));

        org.assertj.core.api.Assertions.assertThat(generateMissingArticleEmbeddingsUseCase.embeddingModel)
                .isEqualTo("text-embedding-v4");
        org.assertj.core.api.Assertions.assertThat(generateMissingArticleEmbeddingsUseCase.limit)
                .isEqualTo(20);
    }

    @Test
    void generatesAllMissingEmbeddingsInBatchesUntilEmpty() throws Exception {
        generateMissingArticleEmbeddingsUseCase.results.add(
                new GenerateMissingArticleEmbeddingsUseCase.GenerateResult(100, 100)
        );
        generateMissingArticleEmbeddingsUseCase.results.add(
                new GenerateMissingArticleEmbeddingsUseCase.GenerateResult(40, 40)
        );
        generateMissingArticleEmbeddingsUseCase.results.add(
                new GenerateMissingArticleEmbeddingsUseCase.GenerateResult(0, 0)
        );

        mockMvc.perform(post("/api/admin/rag/embeddings/generate-all")
                        .param("batchSize", "100")
                        .param("maxBatches", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.embeddingModel").value("text-embedding-v4"))
                .andExpect(jsonPath("$.data.batchSize").value(100))
                .andExpect(jsonPath("$.data.maxBatches").value(10))
                .andExpect(jsonPath("$.data.batches").value(2))
                .andExpect(jsonPath("$.data.scanned").value(140))
                .andExpect(jsonPath("$.data.generated").value(140))
                .andExpect(jsonPath("$.data.finished").value(true));

        org.assertj.core.api.Assertions.assertThat(generateMissingArticleEmbeddingsUseCase.calls)
                .containsExactly(
                        new FakeGenerateMissingArticleEmbeddingsUseCase.Call("text-embedding-v4", 100),
                        new FakeGenerateMissingArticleEmbeddingsUseCase.Call("text-embedding-v4", 100),
                        new FakeGenerateMissingArticleEmbeddingsUseCase.Call("text-embedding-v4", 100)
                );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeGenerateMissingArticleEmbeddingsUseCase generateMissingArticleEmbeddingsUseCase() {
            return new FakeGenerateMissingArticleEmbeddingsUseCase();
        }

        @Bean
        DashScopeEmbeddingProperties dashScopeEmbeddingProperties() {
            DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
            properties.setModelName("text-embedding-v4");
            return properties;
        }
    }

    static class FakeGenerateMissingArticleEmbeddingsUseCase extends GenerateMissingArticleEmbeddingsUseCase {
        private String embeddingModel;
        private Integer limit;
        private GenerateResult result = new GenerateResult(0, 0);
        private final List<GenerateResult> results = new ArrayList<>();
        private final List<Call> calls = new ArrayList<>();

        FakeGenerateMissingArticleEmbeddingsUseCase() {
            super(new NoopLawArticleEmbeddingRepository(), new NoopEmbeddingClient());
        }

        @Override
        public GenerateResult generate(String embeddingModel, Integer limit) {
            this.embeddingModel = embeddingModel;
            this.limit = limit;
            this.calls.add(new Call(embeddingModel, limit));
            if (!results.isEmpty()) {
                return results.remove(0);
            }
            return result;
        }

        private void reset() {
            embeddingModel = null;
            limit = null;
            result = new GenerateResult(0, 0);
            results.clear();
            calls.clear();
        }

        private record Call(String embeddingModel, Integer limit) {
        }
    }

    static class NoopEmbeddingClient implements EmbeddingClient {
        @Override
        public List<BigDecimal> embed(String text, String embeddingModel) {
            return List.of();
        }
    }

    static class NoopLawArticleEmbeddingRepository implements LawArticleEmbeddingRepository {
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
}
