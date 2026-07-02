package com.law4x.agui.infrastructure.agent.config;

import com.law4x.agui.infrastructure.agent.runtime.AgUiRuntimeContextHolder;
import com.law4x.agui.infrastructure.agent.runtime.GroundedHarnessAgent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agent.Agent;
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
    Agent groundedAgUiAgent(
            HarnessAgent law4xAgUiAgent,
            AgUiRuntimeContextHolder agUiRuntimeContextHolder
    ) {
        return new GroundedHarnessAgent(law4xAgUiAgent, agUiRuntimeContextHolder);
    }

    @Bean
    AguiAgentAdapter aguiAgentAdapter(
            Agent groundedAgUiAgent,
            AguiAdapterConfig aguiAdapterConfig
    ) {
        return new AguiAgentAdapter(groundedAgUiAgent, aguiAdapterConfig);
    }
}
