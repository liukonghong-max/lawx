package com.law4x.rag.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.rag.application.CreateRagTestRunUseCase;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.model.RagTestRun;
import com.law4x.rag.domain.repository.RagTestRunRepository;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RagTestRunController.class)
@Import(RagTestRunControllerTest.TestConfig.class)
class RagTestRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeCreateRagTestRunUseCase createRagTestRunUseCase;

    @Test
    void createsRagTestRun() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        createRagTestRunUseCase.result = new CreateRagTestRunUseCase.CreateResult(
                runId,
                List.of(new RagSearchResult(
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
                ))
        );

        mockMvc.perform(post("/api/rag/test-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "别人欠钱不还怎么办",
                                  "embeddingModel": "text-embedding-v4",
                                  "limit": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.runId").value(runId.toString()))
                .andExpect(jsonPath("$.data.items[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.data.items[0].matchType").value("vector"))
                .andExpect(jsonPath("$.data.items[0].vectorScore").value(0.91));

        org.assertj.core.api.Assertions.assertThat(createRagTestRunUseCase.query)
                .isEqualTo("别人欠钱不还怎么办");
        org.assertj.core.api.Assertions.assertThat(createRagTestRunUseCase.embeddingModel)
                .isEqualTo("text-embedding-v4");
        org.assertj.core.api.Assertions.assertThat(createRagTestRunUseCase.limit)
                .isEqualTo(5);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeCreateRagTestRunUseCase createRagTestRunUseCase() {
            return new FakeCreateRagTestRunUseCase();
        }

        @Bean
        DashScopeEmbeddingProperties dashScopeEmbeddingProperties() {
            DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
            properties.setModelName("text-embedding-v4");
            return properties;
        }
    }

    static class FakeCreateRagTestRunUseCase extends CreateRagTestRunUseCase {
        private String query;
        private String embeddingModel;
        private Integer limit;
        private CreateResult result = new CreateResult(UUID.randomUUID(), List.of());

        FakeCreateRagTestRunUseCase() {
            super(null, new NoopRagTestRunRepository());
        }

        @Override
        public CreateResult create(String query, String embeddingModel, Integer limit) {
            this.query = query;
            this.embeddingModel = embeddingModel;
            this.limit = limit;
            return result;
        }
    }

    static class NoopRagTestRunRepository implements RagTestRunRepository {
        @Override
        public RagTestRun save(RagTestRun testRun) {
            return testRun;
        }
    }
}
