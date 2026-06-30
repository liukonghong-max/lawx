package com.law4x.rag.infrastructure.embedding;

import com.law4x.rag.domain.repository.EmbeddingClient;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.message.TextBlock;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class AgentScopeEmbeddingClient implements EmbeddingClient {

    private final EmbeddingModel embeddingModel;

    public AgentScopeEmbeddingClient(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<BigDecimal> embed(String text, String embeddingModelName) {
        if (!embeddingModel.getModelName().equals(embeddingModelName)) {
            throw new IllegalArgumentException("embeddingModel does not match configured AgentScope model");
        }
        double[] values = embeddingModel.embed(TextBlock.builder().text(text).build()).block();
        if (values == null) {
            throw new IllegalStateException("embedding model returned empty result");
        }
        return Arrays.stream(values)
                .mapToObj(BigDecimal::valueOf)
                .toList();
    }
}
