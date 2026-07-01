package com.law4x.rag.interfaces.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.model.LawDocumentArticleItem;
import com.law4x.law.domain.model.LawDocumentSummary;
import com.law4x.law.domain.repository.LawArticleRepository;
import com.law4x.rag.application.HybridSearchUseCase;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.EmbeddingClient;
import com.law4x.rag.domain.repository.LawArticleEmbeddingRepository;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RagSearchController.class)
@Import(RagSearchControllerTest.TestConfig.class)
class RagSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeLawArticleRepository lawArticleRepository;

    @Autowired
    private FakeLawArticleEmbeddingRepository lawArticleEmbeddingRepository;

    @Test
    void searchesRagEvidence() throws Exception {
        UUID articleId = UUID.randomUUID();
        lawArticleRepository.results.add(new LawArticleSearchResult(
                articleId,
                "中华人民共和国民法典",
                "第六百七十五条",
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                "借款人应当按照约定的期限返还借款。",
                new BigDecimal("42.50")
        ));
        lawArticleEmbeddingRepository.results.add(new RagSearchResult(
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

        mockMvc.perform(get("/api/rag/search")
                        .param("query", "别人欠钱不还怎么办")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.data.items[0].documentTitle").value("中华人民共和国民法典"))
                .andExpect(jsonPath("$.data.items[0].articleNo").value("第六百七十五条"))
                .andExpect(jsonPath("$.data.items[0].matchType").value("hybrid"))
                .andExpect(jsonPath("$.data.items[0].keywordScore").value(42.50))
                .andExpect(jsonPath("$.data.items[0].vectorScore").value(0.91))
                .andExpect(jsonPath("$.data.items[0].finalScore").value(43.41))
                .andExpect(jsonPath("$.data.items[0].reason").value("当前使用关键词检索和 pgvector 向量检索共同命中。"));
    }

    @Test
    void returnsBadRequestForBlankQuery() throws Exception {
        mockMvc.perform(get("/api/rag/search")
                        .param("query", " ")
                        .param("limit", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("query must not be blank"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeLawArticleRepository lawArticleRepository() {
            return new FakeLawArticleRepository();
        }

        @Bean
        FakeLawArticleEmbeddingRepository lawArticleEmbeddingRepository() {
            return new FakeLawArticleEmbeddingRepository();
        }

        @Bean
        EmbeddingClient embeddingClient() {
            return (text, embeddingModel) -> List.of(BigDecimal.ONE);
        }

        @Bean
        DashScopeEmbeddingProperties dashScopeEmbeddingProperties() {
            DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
            properties.setModelName("text-embedding-v4");
            return properties;
        }

        @Bean
        HybridSearchUseCase hybridSearchUseCase(
                LawArticleRepository lawArticleRepository,
                LawArticleEmbeddingRepository lawArticleEmbeddingRepository,
                EmbeddingClient embeddingClient
        ) {
            return new HybridSearchUseCase(lawArticleRepository, lawArticleEmbeddingRepository, embeddingClient);
        }
    }

    static class FakeLawArticleRepository implements LawArticleRepository {
        private final List<LawArticleSearchResult> results = new ArrayList<>();

        @Override
        public List<LawDocumentSummary> listEffectiveDocuments(int limit) {
            return List.of();
        }

        @Override
        public PagedResult<LawDocumentArticleItem> listDocumentArticles(UUID documentId, int page, int pageSize) {
            return new PagedResult<>(List.of(), page, pageSize, 0);
        }

        @Override
        public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
            return results;
        }

        @Override
        public Optional<com.law4x.law.domain.model.LawArticleDetail> findArticleDetail(UUID articleId) {
            return Optional.empty();
        }
    }

    static class FakeLawArticleEmbeddingRepository implements LawArticleEmbeddingRepository {
        private final List<RagSearchResult> results = new ArrayList<>();

        @Override
        public List<RagSearchResult> searchSimilarArticles(String embeddingModel, List<BigDecimal> queryEmbedding, int limit) {
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
}
