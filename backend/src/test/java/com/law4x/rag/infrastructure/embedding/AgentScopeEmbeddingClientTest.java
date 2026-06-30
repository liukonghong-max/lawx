package com.law4x.rag.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.message.ContentBlock;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class AgentScopeEmbeddingClientTest {

    @Test
    void embedsTextThroughAgentScopeModel() {
        FakeEmbeddingModel model = new FakeEmbeddingModel("text-embedding-v4", 3);
        AgentScopeEmbeddingClient client = new AgentScopeEmbeddingClient(model);

        List<BigDecimal> embedding = client.embed("测试文本", "text-embedding-v4");

        assertThat(model.lastTextBlock).contains("测试文本");
        assertThat(embedding).containsExactly(
                new BigDecimal("0.1"),
                new BigDecimal("0.2"),
                new BigDecimal("0.3")
        );
    }

    @Test
    void rejectsDifferentModelName() {
        AgentScopeEmbeddingClient client = new AgentScopeEmbeddingClient(
                new FakeEmbeddingModel("text-embedding-v4", 3)
        );

        assertThatThrownBy(() -> client.embed("测试文本", "another-model"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("embeddingModel does not match configured AgentScope model");
    }

    private static class FakeEmbeddingModel implements EmbeddingModel {
        private final String modelName;
        private final int dimensions;
        private String lastTextBlock;

        private FakeEmbeddingModel(String modelName, int dimensions) {
            this.modelName = modelName;
            this.dimensions = dimensions;
        }

        @Override
        public Mono<double[]> embed(ContentBlock contentBlock) {
            lastTextBlock = contentBlock.toString();
            return Mono.just(new double[]{0.1, 0.2, 0.3});
        }

        @Override
        public String getModelName() {
            return modelName;
        }

        @Override
        public int getDimensions() {
            return dimensions;
        }
    }
}
