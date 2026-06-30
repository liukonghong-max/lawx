package com.law4x.rag.interfaces.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import com.law4x.rag.application.HybridSearchUseCase;
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
                .andExpect(jsonPath("$.data.items[0].matchType").value("keyword"))
                .andExpect(jsonPath("$.data.items[0].keywordScore").value(42.50))
                .andExpect(jsonPath("$.data.items[0].vectorScore").value(0))
                .andExpect(jsonPath("$.data.items[0].finalScore").value(42.50))
                .andExpect(jsonPath("$.data.items[0].reason").value("当前使用关键词检索命中，后续会叠加向量召回和 rerank。"));
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
        HybridSearchUseCase hybridSearchUseCase(LawArticleRepository lawArticleRepository) {
            return new HybridSearchUseCase(lawArticleRepository);
        }
    }

    static class FakeLawArticleRepository implements LawArticleRepository {
        private final List<LawArticleSearchResult> results = new ArrayList<>();

        @Override
        public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
            return results;
        }

        @Override
        public Optional<com.law4x.law.domain.model.LawArticleDetail> findArticleDetail(UUID articleId) {
            return Optional.empty();
        }
    }
}
