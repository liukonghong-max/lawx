package com.law4x.law.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.law.application.GetLawArticleDetailUseCase;
import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.time.LocalDate;
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

@WebMvcTest(LawArticleDetailController.class)
@Import(LawArticleDetailControllerTest.TestConfig.class)
class LawArticleDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeLawArticleRepository lawArticleRepository;

    @Test
    void returnsArticleDetail() throws Exception {
        UUID articleId = UUID.randomUUID();
        lawArticleRepository.detail = Optional.of(new LawArticleDetail(
                articleId,
                "中华人民共和国民法典",
                "法律",
                "全国人民代表大会",
                LocalDate.of(2020, 5, 28),
                LocalDate.of(2021, 1, 1),
                "effective",
                "https://example.test/civil-code",
                "第三编 合同",
                "第八章 违约责任",
                null,
                "第五百七十七条",
                577,
                "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
                "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
                "effective"
        ));

        mockMvc.perform(get("/api/law/articles/{articleId}", articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.data.documentTitle").value("中华人民共和国民法典"))
                .andExpect(jsonPath("$.data.articleNo").value("第五百七十七条"))
                .andExpect(jsonPath("$.data.content").value("当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。"))
                .andExpect(jsonPath("$.data.documentStatus").value("effective"))
                .andExpect(jsonPath("$.data.effectiveStatus").value("effective"));
    }

    @Test
    void returnsNotFoundWhenArticleDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/law/articles/{articleId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("article not found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeLawArticleRepository lawArticleRepository() {
            return new FakeLawArticleRepository();
        }

        @Bean
        GetLawArticleDetailUseCase getLawArticleDetailUseCase(LawArticleRepository lawArticleRepository) {
            return new GetLawArticleDetailUseCase(lawArticleRepository);
        }
    }

    static class FakeLawArticleRepository implements LawArticleRepository {
        private Optional<LawArticleDetail> detail = Optional.empty();

        @Override
        public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
            return List.of();
        }

        @Override
        public Optional<LawArticleDetail> findArticleDetail(UUID articleId) {
            return detail;
        }
    }
}
