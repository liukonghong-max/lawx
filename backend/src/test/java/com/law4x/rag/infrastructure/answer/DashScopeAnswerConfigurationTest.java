package com.law4x.rag.infrastructure.answer;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.rag.domain.repository.RagAnswerClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DashScopeAnswerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DashScopeAnswerConfiguration.class, AnswerFallbackConfiguration.class);

    @Test
    void usesExtractiveAnswerClientWhenDashScopeAnswerIsDisabled() {
        contextRunner
                .withPropertyValues("law4x.answer.dashscope.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(RagAnswerClient.class);
                    assertThat(context.getBean(RagAnswerClient.class))
                            .isInstanceOf(ExtractiveRagAnswerClient.class);
                });
    }

    @Test
    void createsAgentScopeAnswerClientWhenDashScopeAnswerIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "law4x.answer.dashscope.enabled=true",
                        "law4x.answer.dashscope.api-key=test-key",
                        "law4x.answer.dashscope.model-name=qwen-plus"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RagAnswerClient.class);
                    assertThat(context.getBean(RagAnswerClient.class))
                            .isInstanceOf(AgentScopeRagAnswerClient.class);
                });
    }

    @Test
    void failsFastWhenDashScopeAnswerIsEnabledWithoutApiKey() {
        contextRunner
                .withPropertyValues("law4x.answer.dashscope.enabled=true")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("law4x.answer.dashscope.api-key must be configured"));
    }
}
