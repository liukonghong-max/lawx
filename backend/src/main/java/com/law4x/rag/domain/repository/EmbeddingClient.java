package com.law4x.rag.domain.repository;

import java.math.BigDecimal;
import java.util.List;

public interface EmbeddingClient {

    List<BigDecimal> embed(String text, String embeddingModel);
}
