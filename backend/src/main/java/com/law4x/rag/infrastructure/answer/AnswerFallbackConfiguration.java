package com.law4x.rag.infrastructure.answer;

import com.law4x.rag.domain.repository.RagAnswerClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnswerFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(RagAnswerClient.class)
    @ConditionalOnProperty(
            prefix = "law4x.answer",
            name = {"openai.enabled", "dashscope.enabled"},
            havingValue = "false",
            matchIfMissing = true
    )
    RagAnswerClient extractiveRagAnswerClient() {
        return new ExtractiveRagAnswerClient();
    }
}
