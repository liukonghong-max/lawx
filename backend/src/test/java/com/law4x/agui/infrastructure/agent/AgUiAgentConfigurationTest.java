package com.law4x.agui.infrastructure.agent;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AgUiAgentConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AgUiAgentConfiguration.class);

    @Test
    void createsHarnessAgentBean() {
        contextRunner
                .withPropertyValues(
                        "law4x.agentscope.api-key=test-key",
                        "law4x.agentscope.provider=openai-compatible",
                        "law4x.agentscope.model-name=ark-code-latest",
                        "law4x.agentscope.workspace=.agentscope/workspace"
                )
                .run(context -> assertThat(context).hasSingleBean(HarnessAgent.class));
    }
}
