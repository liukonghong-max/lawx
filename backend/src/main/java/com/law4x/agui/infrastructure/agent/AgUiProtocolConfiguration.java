package com.law4x.agui.infrastructure.agent;

import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.model.ToolMergeMode;
import io.agentscope.harness.agent.HarnessAgent;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgUiProtocolConfiguration {

    @Bean
    AguiAdapterConfig aguiAdapterConfig() {
        return AguiAdapterConfig.builder()
                .enableReasoning(false)
                .runTimeout(Duration.ofMinutes(5))
                .toolMergeMode(ToolMergeMode.AGENT_ONLY)
                .build();
    }

    @Bean
    AguiAgentAdapter aguiAgentAdapter(
            HarnessAgent law4xAgUiAgent,
            AguiAdapterConfig aguiAdapterConfig
    ) {
        return new AguiAgentAdapter(law4xAgUiAgent, aguiAdapterConfig);
    }
}
