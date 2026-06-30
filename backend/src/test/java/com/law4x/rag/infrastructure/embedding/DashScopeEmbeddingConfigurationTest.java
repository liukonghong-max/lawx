package com.law4x.rag.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.rag.domain.repository.EmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DashScopeEmbeddingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DashScopeEmbeddingConfiguration.class)
            .withUserConfiguration(EmbeddingFallbackConfiguration.class);

    @Test
    void usesUnsupportedClientWhenDashScopeIsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EmbeddingClient.class);
            assertThat(context.getBean(EmbeddingClient.class)).isInstanceOf(UnsupportedEmbeddingClient.class);
        });
    }

    @Test
    void failsFastWhenDashScopeEnabledWithoutApiKey() {
        contextRunner
                .withPropertyValues("law4x.embedding.dashscope.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
}
