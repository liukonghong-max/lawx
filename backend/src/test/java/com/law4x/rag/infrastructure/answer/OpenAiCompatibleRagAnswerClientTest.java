package com.law4x.rag.infrastructure.answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class OpenAiCompatibleRagAnswerClientTest {

    @Test
    void sendsOpenAiChatCompletionRequestToArkEndpoint() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        OpenAiCompatibleRagAnswerClient client = new OpenAiCompatibleRagAnswerClient(
                restTemplate,
                "https://ark.cn-beijing.volces.com/api/coding/v3",
                "test-key",
                "ark-code-latest"
        );
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

        server.expect(requestTo("https://ark.cn-beijing.volces.com/api/coding/v3/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("ark-code-latest"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value(org.hamcrest.Matchers.containsString("只基于给定法条依据回答")))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("别人欠钱不还怎么办")))
                .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("第六百七十六条")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "可以要求对方返还借款，并主张逾期利息。"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String answer = client.answer("别人欠钱不还怎么办", List.of(evidence));

        assertThat(answer).isEqualTo("可以要求对方返还借款，并主张逾期利息。");
        server.verify();
    }
}
