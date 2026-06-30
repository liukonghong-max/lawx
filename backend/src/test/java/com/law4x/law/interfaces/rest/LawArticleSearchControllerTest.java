package com.law4x.law.interfaces.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.law.application.SearchLawArticlesUseCase;
import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LawArticleSearchController.class)
@Import(LawArticleSearchControllerTest.TestConfig.class)
class LawArticleSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeLawArticleRepository lawArticleRepository;

    @Test
    void searchesLawArticles() throws Exception {
        lawArticleRepository.results.add(new LawArticleSearchResult(
                "中华人民共和国民法典",
                "第五百七十七条",
                "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
                "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
                new BigDecimal("116.21")
        ));

        mockMvc.perform(get("/api/law/articles/search")
                        .param("query", "第五百七十七条")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].documentTitle").value("中华人民共和国民法典"))
                .andExpect(jsonPath("$.items[0].articleNo").value("第五百七十七条"))
                .andExpect(jsonPath("$.items[0].score").value(116.21));

        org.assertj.core.api.Assertions.assertThat(lawArticleRepository.lastQuery).isEqualTo("第五百七十七条");
        org.assertj.core.api.Assertions.assertThat(lawArticleRepository.lastLimit).isEqualTo(3);
    }

    @Test
    void returnsBadRequestForBlankQuery() throws Exception {
        mockMvc.perform(get("/api/law/articles/search")
                        .param("query", " ")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("query must not be blank"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeLawArticleRepository lawArticleRepository() {
            return new FakeLawArticleRepository();
        }

        @Bean
        SearchLawArticlesUseCase searchLawArticlesUseCase(LawArticleRepository lawArticleRepository) {
            return new SearchLawArticlesUseCase(lawArticleRepository);
        }
    }

    static class FakeLawArticleRepository implements LawArticleRepository {
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
