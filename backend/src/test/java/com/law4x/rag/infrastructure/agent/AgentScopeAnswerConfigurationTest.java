package com.law4x.rag.infrastructure.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.rag.domain.repository.RagAnswerClient;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.AgentStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AgentScopeAnswerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AgentScopeAnswerConfiguration.class, AgentScopeRagAgentPromptFactory.class, StructuredAnswerParser.class);

    @Test
    void createsSingleAgentScopeAnswerChainForOpenAiCompatibleProvider() {
        contextRunner
                .withPropertyValues(
                        "law4x.agentscope.api-key=test-key",
                        "law4x.agentscope.provider=openai-compatible",
                        "law4x.agentscope.model-name=ark-code-latest"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RagAnswerClient.class);
                    assertThat(context).hasSingleBean(ReActAgent.class);
                    assertThat(context).hasSingleBean(AgentStateStore.class);
                    assertThat(context.getBean(RagAnswerClient.class)).isInstanceOf(com.law4x.rag.infrastructure.answer.AgentScopeRagAnswerClient.class);
                });
    }

    @Test
    void failsFastWhenApiKeyIsMissing() {
        contextRunner
                .withPropertyValues("law4x.agentscope.api-key=")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("law4x.agentscope.api-key must be configured"));
    }

    @Test
    void exposesStructuredAnswerParserBean() {
        contextRunner
                .withPropertyValues(
                        "law4x.agentscope.api-key=test-key",
                        "law4x.agentscope.provider=openai-compatible",
                        "law4x.agentscope.model-name=ark-code-latest"
                )
                .run(context -> assertThat(context).hasSingleBean(StructuredAnswerParser.class));
    }
}
