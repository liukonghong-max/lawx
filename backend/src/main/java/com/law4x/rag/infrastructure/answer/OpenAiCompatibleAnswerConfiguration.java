package com.law4x.rag.infrastructure.answer;

import com.law4x.rag.domain.repository.RagAnswerClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(OpenAiCompatibleAnswerProperties.class)
public class OpenAiCompatibleAnswerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "law4x.answer.openai", name = "enabled", havingValue = "true")
    RagAnswerClient openAiCompatibleRagAnswerClient(OpenAiCompatibleAnswerProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("law4x.answer.openai.api-key must be configured");
        }
        return new OpenAiCompatibleRagAnswerClient(
                new RestTemplate(),
                properties.getBaseUrl(),
                properties.getApiKey(),
                properties.getModelName()
        );
    }
}
