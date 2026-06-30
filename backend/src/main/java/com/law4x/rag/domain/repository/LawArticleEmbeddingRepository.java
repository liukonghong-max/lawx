package com.law4x.rag.domain.repository;

import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.List;

public interface LawArticleEmbeddingRepository {

    List<RagSearchResult> searchSimilarArticles(String embeddingModel, List<BigDecimal> queryEmbedding, int limit);
}
