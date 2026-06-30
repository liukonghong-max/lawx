package com.law4x.rag.infrastructure.embedding;

import com.law4x.rag.domain.repository.EmbeddingClient;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UnsupportedEmbeddingClient implements EmbeddingClient {

    @Override
    public List<BigDecimal> embed(String text, String embeddingModel) {
        throw new UnsupportedOperationException("embedding client is not configured");
    }
}
