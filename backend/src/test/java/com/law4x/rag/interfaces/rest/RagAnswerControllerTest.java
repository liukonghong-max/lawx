package com.law4x.rag.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.rag.application.CreateRagAnswerUseCase;
import com.law4x.rag.domain.model.RagAnswer;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
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

@WebMvcTest(RagAnswerController.class)
@Import(RagAnswerControllerTest.TestConfig.class)
class RagAnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeCreateRagAnswerUseCase createRagAnswerUseCase;

    @Test
    void createsRagAnswer() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        createRagAnswerUseCase.answer = new RagAnswer(
                runId,
                "可以要求对方返还借款，并结合证据主张逾期利息。",
                List.of(new RagAnswer.Citation(
                        articleId,
                        "中华人民共和国民法典",
                        "第六百七十六条",
                        "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十六条",
                        "借款人未按照约定的期限返还借款的，应当按照约定或者国家有关规定支付逾期利息。"
                )),
                List.of(new RagAnswer.AnswerSegment(
                        "seg-1",
                        "可以要求对方返还借款，并结合证据主张逾期利息。",
                        List.of(articleId)
                ))
        );

        mockMvc.perform(post("/api/rag/answer")
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
                .andExpect(jsonPath("$.data.answer").value("可以要求对方返还借款，并结合证据主张逾期利息。"))
                .andExpect(jsonPath("$.data.citations[0].articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.data.citations[0].documentTitle").value("中华人民共和国民法典"))
                .andExpect(jsonPath("$.data.citations[0].articleNo").value("第六百七十六条"))
                .andExpect(jsonPath("$.data.answerSegments[0].id").value("seg-1"))
                .andExpect(jsonPath("$.data.answerSegments[0].text").value("可以要求对方返还借款，并结合证据主张逾期利息。"))
                .andExpect(jsonPath("$.data.answerSegments[0].citationIds[0]").value(articleId.toString()));

        org.assertj.core.api.Assertions.assertThat(createRagAnswerUseCase.query)
                .isEqualTo("别人欠钱不还怎么办");
        org.assertj.core.api.Assertions.assertThat(createRagAnswerUseCase.embeddingModel)
                .isEqualTo("text-embedding-v4");
        org.assertj.core.api.Assertions.assertThat(createRagAnswerUseCase.limit)
                .isEqualTo(5);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        FakeCreateRagAnswerUseCase createRagAnswerUseCase() {
            return new FakeCreateRagAnswerUseCase();
        }

        @Bean
        DashScopeEmbeddingProperties dashScopeEmbeddingProperties() {
            DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
            properties.setModelName("text-embedding-v4");
            return properties;
        }
    }

    static class FakeCreateRagAnswerUseCase extends CreateRagAnswerUseCase {
        private String query;
        private String embeddingModel;
        private Integer limit;
        private RagAnswer answer = new RagAnswer(UUID.randomUUID(), "", List.of());

        FakeCreateRagAnswerUseCase() {
            super(null, null);
        }

        @Override
        public RagAnswer answer(String query, String embeddingModel, Integer limit) {
            this.query = query;
            this.embeddingModel = embeddingModel;
            this.limit = limit;
            return answer;
        }
    }
}
