package com.law4x.agui.infrastructure.agent;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AgUiProtocolConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AgUiAgentConfiguration.class, AgUiProtocolConfiguration.class);

    @Test
    void createsAgUiAdapterBeans() {
        contextRunner
                .withPropertyValues(
                        "law4x.agentscope.api-key=test-key",
                        "law4x.agentscope.provider=openai-compatible",
                        "law4x.agentscope.model-name=ark-code-latest",
                        "law4x.agentscope.workspace=.agentscope/workspace"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AguiAdapterConfig.class);
                    assertThat(context).hasSingleBean(AguiAgentAdapter.class);
                });
    }
}
