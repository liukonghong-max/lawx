package com.law4x.law.interfaces.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.law.application.ListLawDocumentArticlesUseCase;
import com.law4x.law.application.ListLawDocumentsUseCase;
import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.model.LawDocumentArticleItem;
import com.law4x.law.domain.model.LawDocumentSummary;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LawDocumentLibraryController.class)
@Import(LawDocumentLibraryControllerTest.TestConfig.class)
@ImportAutoConfiguration(WebMvcAutoConfiguration.class)
class LawDocumentLibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeLawArticleRepository lawArticleRepository;

    @Test
    void listsEffectiveDocuments() throws Exception {
        UUID documentId = UUID.randomUUID();
        lawArticleRepository.documents.add(new LawDocumentSummary(
                documentId,
                "中华人民共和国民法典",
                "法律",
                "全国人民代表大会",
                LocalDate.of(2020, 5, 28),
                LocalDate.of(2021, 1, 1),
                "effective",
                1260
        ));

        mockMvc.perform(get("/api/law/documents").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.data.items[0].title").value("中华人民共和国民法典"))
                .andExpect(jsonPath("$.data.items[0].articleCount").value(1260));
    }

    @Test
    void listsDocumentArticles() throws Exception {
        UUID articleId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        lawArticleRepository.documentArticles.add(new LawDocumentArticleItem(
                articleId,
                "第三编 合同",
                "第八章 违约责任",
                null,
                "第五百七十七条",
                577,
                "当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。",
                "中华人民共和国民法典 > 第三编 合同 > 第八章 违约责任 > 第五百七十七条",
                "effective"
        ));

        mockMvc.perform(get("/api/law/documents/{documentId}/articles", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.data.items[0].articleNo").value("第五百七十七条"))
                .andExpect(jsonPath("$.data.items[0].chapterTitle").value("第八章 违约责任"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeLawArticleRepository lawArticleRepository() {
            return new FakeLawArticleRepository();
        }

        @Bean
        ListLawDocumentsUseCase listLawDocumentsUseCase(LawArticleRepository lawArticleRepository) {
            return new ListLawDocumentsUseCase(lawArticleRepository);
        }

        @Bean
        ListLawDocumentArticlesUseCase listLawDocumentArticlesUseCase(LawArticleRepository lawArticleRepository) {
            return new ListLawDocumentArticlesUseCase(lawArticleRepository);
        }
    }

    static class FakeLawArticleRepository implements LawArticleRepository {
        private final List<LawDocumentSummary> documents = new ArrayList<>();
        private final List<LawDocumentArticleItem> documentArticles = new ArrayList<>();

        @Override
        public List<LawDocumentSummary> listEffectiveDocuments(int limit) {
            return documents;
        }

        @Override
        public PagedResult<LawDocumentArticleItem> listDocumentArticles(UUID documentId, int page, int pageSize) {
            return new PagedResult<>(documentArticles, page, pageSize, documentArticles.size());
        }

        @Override
        public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
            return List.of();
        }

        @Override
        public Optional<LawArticleDetail> findArticleDetail(UUID articleId) {
            return Optional.empty();
        }
    }
}
