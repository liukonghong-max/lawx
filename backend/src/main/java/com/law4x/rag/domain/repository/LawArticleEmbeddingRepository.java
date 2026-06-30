package com.law4x.rag.domain.repository;

import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.model.EmbeddableLawArticle;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LawArticleEmbeddingRepository {

    List<RagSearchResult> searchSimilarArticles(String embeddingModel, List<BigDecimal> queryEmbedding, int limit);

    List<EmbeddableLawArticle> findArticlesMissingEmbeddings(String embeddingModel, int limit);

    void upsertArticleEmbedding(
            UUID articleId,
            String embeddingModel,
            String contentHash,
            List<BigDecimal> embedding
    );
}
