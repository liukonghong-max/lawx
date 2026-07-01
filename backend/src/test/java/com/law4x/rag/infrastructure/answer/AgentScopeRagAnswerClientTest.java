package com.law4x.rag.infrastructure.answer;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.rag.domain.model.RagSearchResult;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class AgentScopeRagAnswerClientTest {

    @Test
    void generatesAnswerThroughAgentScopeModelWithGroundedPrompt() {
        FakeModel model = new FakeModel("qwen-plus", "可以要求对方返还借款，并主张逾期利息。");
        AgentScopeRagAnswerClient client = new AgentScopeRagAnswerClient(model);
        RagSearchResult evidence = new RagSearchResult(
                UUID.randomUUID(),
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

        String answer = client.answer("别人欠钱不还怎么办", List.of(evidence));

        assertThat(answer).isEqualTo("可以要求对方返还借款，并主张逾期利息。");
        assertThat(model.lastMessages).hasSize(2);
        assertThat(model.lastMessages.get(0).getRole()).isEqualTo(MsgRole.SYSTEM);
        assertThat(model.lastMessages.get(0).getTextContent())
                .contains("只基于给定法条依据回答")
                .contains("不确定就说明无法仅凭现有法条判断")
                .contains("不要承诺诉讼结果");
        assertThat(model.lastMessages.get(1).getRole()).isEqualTo(MsgRole.USER);
        assertThat(model.lastMessages.get(1).getTextContent())
                .contains("别人欠钱不还怎么办")
                .contains("中华人民共和国民法典")
                .contains("第六百七十六条")
                .contains("借款人未按照约定的期限返还借款");
    }

    private static class FakeModel implements Model {
        private final String modelName;
        private final String answer;
        private List<Msg> lastMessages;

        private FakeModel(String modelName, String answer) {
            this.modelName = modelName;
            this.answer = answer;
        }

        @Override
        public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            lastMessages = messages;
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text(answer).build()))
                    .build());
        }

        @Override
        public String getModelName() {
            return modelName;
        }
    }
}
