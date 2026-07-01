package com.law4x.rag.infrastructure.answer;

import com.law4x.rag.domain.repository.RagAnswerClient;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DashScopeAnswerProperties.class)
public class DashScopeAnswerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "law4x.answer.dashscope", name = "enabled", havingValue = "true")
    RagAnswerClient dashScopeRagAnswerClient(DashScopeAnswerProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("law4x.answer.dashscope.api-key must be configured");
        }
        DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .stream(false)
                .enableSearch(false);
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return new AgentScopeRagAnswerClient(builder.build());
    }
}
