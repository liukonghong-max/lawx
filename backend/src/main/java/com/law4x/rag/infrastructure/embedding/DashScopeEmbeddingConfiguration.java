package com.law4x.rag.infrastructure.embedding;

import com.law4x.rag.domain.repository.EmbeddingClient;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DashScopeEmbeddingProperties.class)
public class DashScopeEmbeddingConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "law4x.embedding.dashscope", name = "enabled", havingValue = "true")
    EmbeddingClient dashScopeEmbeddingClient(DashScopeEmbeddingProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("law4x.embedding.dashscope.api-key must be configured");
        }
        DashScopeTextEmbedding.Builder builder = DashScopeTextEmbedding.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .dimensions(properties.getDimensions());
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return new AgentScopeEmbeddingClient(builder.build());
    }
}
