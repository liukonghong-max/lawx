package com.law4x.rag.infrastructure.answer;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.rag.domain.repository.RagAnswerClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OpenAiCompatibleAnswerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(OpenAiCompatibleAnswerConfiguration.class, AnswerFallbackConfiguration.class);

    @Test
    void usesExtractiveAnswerClientWhenOpenAiAnswerIsDisabled() {
        contextRunner
                .withPropertyValues("law4x.answer.openai.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(RagAnswerClient.class);
                    assertThat(context.getBean(RagAnswerClient.class))
                            .isInstanceOf(ExtractiveRagAnswerClient.class);
                });
    }

    @Test
    void createsOpenAiCompatibleAnswerClientWhenOpenAiAnswerIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "law4x.answer.openai.enabled=true",
                        "law4x.answer.openai.api-key=test-key"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RagAnswerClient.class);
                    assertThat(context.getBean(RagAnswerClient.class))
                            .isInstanceOf(OpenAiCompatibleRagAnswerClient.class);
                });
    }

    @Test
    void doesNotCreateFallbackWhenOpenAiAnswerIsEnabledEvenIfFallbackConfigurationIsProcessedFirst() {
        new ApplicationContextRunner()
                .withUserConfiguration(AnswerFallbackConfiguration.class, OpenAiCompatibleAnswerConfiguration.class)
                .withPropertyValues(
                        "law4x.answer.openai.enabled=true",
                        "law4x.answer.openai.api-key=test-key"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RagAnswerClient.class);
                    assertThat(context).doesNotHaveBean(ExtractiveRagAnswerClient.class);
                    assertThat(context.getBean(RagAnswerClient.class))
                            .isInstanceOf(OpenAiCompatibleRagAnswerClient.class);
                });
    }

    @Test
    void failsFastWhenOpenAiAnswerIsEnabledWithoutApiKey() {
        contextRunner
                .withPropertyValues("law4x.answer.openai.enabled=true")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("law4x.answer.openai.api-key must be configured"));
    }
}
