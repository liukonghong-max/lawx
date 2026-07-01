package com.law4x.rag.infrastructure.agent;

import com.law4x.rag.domain.repository.RagAnswerClient;
import com.law4x.rag.infrastructure.answer.AgentScopeRagAnswerClient;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Law4xAgentScopeProperties.class)
public class AgentScopeAnswerConfiguration {

    @Bean
    Model law4xAnswerModel(Law4xAgentScopeProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("law4x.agentscope.api-key must be configured");
        }
        if ("dashscope".equalsIgnoreCase(properties.getProvider())) {
            DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                    .apiKey(properties.getApiKey())
                    .modelName(properties.getModelName())
                    .stream(false)
                    .enableSearch(false);
            if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
                builder.baseUrl(properties.getBaseUrl());
            }
            return builder.build();
        }
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .stream(false);
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return builder.build();
    }

    @Bean
    AgentStateStore law4xAgentStateStore(Law4xAgentScopeProperties properties) {
        return new JsonFileAgentStateStore(Path.of(properties.getWorkspace(), "state"));
    }

    @Bean
    ReActAgent law4xAnswerAgent(
            Law4xAgentScopeProperties properties,
            Model law4xAnswerModel,
            AgentStateStore law4xAgentStateStore,
            AgentScopeRagAgentPromptFactory promptFactory,
            StructuredAnswerParser structuredAnswerParser
    ) {
        return ReActAgent.builder()
                .name(properties.getAgentName())
                .sysPrompt(promptFactory.systemPrompt())
                .model(law4xAnswerModel)
                .middleware(new StructuredOutputMiddleware())
                .stateStore(law4xAgentStateStore)
                .defaultSessionId(properties.getDefaultSessionId())
                .build();
    }

    @Bean
    RagAnswerClient ragAnswerClient(
            ReActAgent law4xAnswerAgent,
            Law4xAgentScopeProperties properties,
            AgentScopeRagAgentPromptFactory promptFactory,
            StructuredAnswerParser structuredAnswerParser
    ) {
        return new AgentScopeRagAnswerClient(
                law4xAnswerAgent,
                properties,
                promptFactory,
                structuredAnswerParser
        );
    }
}
