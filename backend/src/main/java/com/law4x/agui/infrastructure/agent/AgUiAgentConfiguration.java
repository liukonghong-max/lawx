package com.law4x.agui.infrastructure.agent;

import com.law4x.rag.infrastructure.agent.Law4xAgentScopeProperties;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Law4xAgentScopeProperties.class)
public class AgUiAgentConfiguration {

    @Bean
    HarnessAgent law4xAgUiAgent(Law4xAgentScopeProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("law4x.agentscope.api-key must be configured");
        }
        Model model = createModel(properties);
        return HarnessAgent.builder()
                .name(properties.getAgentName())
                .sysPrompt("你是 law4x 法律法规咨询助手。必须基于法规依据回答，不能编造来源。")
                .model(model)
                .workspace(Paths.get(properties.getWorkspace()))
                .build();
    }

    private Model createModel(Law4xAgentScopeProperties properties) {
        String provider = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        if ("dashscope".equals(provider)) {
            DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                    .apiKey(properties.getApiKey())
                    .modelName(properties.getModelName())
                    .stream(true)
                    .enableSearch(false);
            if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
                builder.baseUrl(properties.getBaseUrl());
            }
            return builder.build();
        }
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .stream(true);
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return builder.build();
    }
}
