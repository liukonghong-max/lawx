package com.law4x.rag.infrastructure.embedding;

import com.law4x.rag.domain.repository.EmbeddingClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingClient.class)
    EmbeddingClient unsupportedEmbeddingClient() {
        return new UnsupportedEmbeddingClient();
    }
}
