package com.law4x.rag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.law4x.rag.domain.model.RagAnswer;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.RagAnswerClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateRagAnswerUseCaseTest {

    @Test
    void rejectsBlankQuery() {
        CreateRagAnswerUseCase useCase = new CreateRagAnswerUseCase(
                new FakeCreateRagTestRunUseCase(),
                new FakeRagAnswerClient()
        );

        assertThatThrownBy(() -> useCase.answer("  ", "text-embedding-v4", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query must not be blank");
    }

    @Test
    void createsAnswerWithCitationsFromRagEvidence() {
        UUID runId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        RagSearchResult evidence = new RagSearchResult(
                articleId,
                "中华人民共和国民法典",
                "第六百七十六条",
                "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十六条",
                "借款人未按照约定的期限返还借款的，应当按照约定或者国家有关规定支付逾期利息。",
                "vector",
                BigDecimal.ZERO,
                new BigDecimal("0.91"),
                new BigDecimal("0.91"),
                "当前使用 pgvector 向量检索命中。"
        );
        FakeCreateRagTestRunUseCase testRunUseCase = new FakeCreateRagTestRunUseCase();
        testRunUseCase.result = new CreateRagTestRunUseCase.CreateResult(runId, List.of(evidence));
        FakeRagAnswerClient answerClient = new FakeRagAnswerClient();
        answerClient.answer = "可以依据借款合同相关条文主张返还借款和逾期利息。";
        CreateRagAnswerUseCase useCase = new CreateRagAnswerUseCase(testRunUseCase, answerClient);

        RagAnswer answer = useCase.answer(" 别人欠钱不还怎么办 ", "text-embedding-v4", 50);

        assertThat(testRunUseCase.query).isEqualTo("别人欠钱不还怎么办");
        assertThat(testRunUseCase.embeddingModel).isEqualTo("text-embedding-v4");
        assertThat(testRunUseCase.limit).isEqualTo(20);
        assertThat(answerClient.question).isEqualTo("别人欠钱不还怎么办");
        assertThat(answerClient.evidence).containsExactly(evidence);
        assertThat(answer.runId()).isEqualTo(runId);
        assertThat(answer.answer()).isEqualTo("可以依据借款合同相关条文主张返还借款和逾期利息。");
        assertThat(answer.citations()).hasSize(1);
        assertThat(answer.citations().get(0).articleId()).isEqualTo(articleId);
        assertThat(answer.citations().get(0).documentTitle()).isEqualTo("中华人民共和国民法典");
        assertThat(answer.citations().get(0).articleNo()).isEqualTo("第六百七十六条");
        assertThat(answer.citations().get(0).quotedText()).isEqualTo(evidence.preview());
    }

    private static class FakeCreateRagTestRunUseCase extends CreateRagTestRunUseCase {
        private String query;
        private String embeddingModel;
        private Integer limit;
        private CreateResult result = new CreateResult(UUID.randomUUID(), List.of());

        FakeCreateRagTestRunUseCase() {
            super(null, null);
        }

        @Override
        public CreateResult create(String query, String embeddingModel, Integer limit) {
            this.query = query;
            this.embeddingModel = embeddingModel;
            this.limit = limit;
            return result;
        }
    }

    private static class FakeRagAnswerClient implements RagAnswerClient {
        private String question;
        private List<RagSearchResult> evidence;
        private String answer = "测试回答";

        @Override
        public RagAnswerClient.RagAnswerPayload answer(String question, List<RagSearchResult> evidence) {
            this.question = question;
            this.evidence = evidence;
            return new RagAnswerClient.RagAnswerPayload(answer);
        }
    }
}
