package com.law4x.rag.infrastructure.persistence;

import com.law4x.rag.domain.model.EmbeddableLawArticle;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.LawArticleEmbeddingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLawArticleEmbeddingRepository implements LawArticleEmbeddingRepository {

    private static final String VECTOR_MATCH_TYPE = "vector";
    private static final BigDecimal ZERO_KEYWORD_SCORE = BigDecimal.ZERO;
    private static final String VECTOR_REASON = "当前使用 pgvector 向量检索命中。";

    private final JdbcClient jdbcClient;

    public JdbcLawArticleEmbeddingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<RagSearchResult> searchSimilarArticles(String embeddingModel, List<BigDecimal> queryEmbedding, int limit) {
        return jdbcClient.sql("""
                        SELECT
                            a.id AS article_id,
                            d.title AS document_title,
                            a.article_no,
                            a.full_path,
                            left(a.content, 180) AS preview,
                            (1 - (e.embedding <=> CAST(:queryEmbedding AS vector))) AS vector_score
                        FROM law_article_embeddings e
                        JOIN law_articles a ON a.id = e.article_id
                        JOIN law_documents d ON d.id = a.document_id
                        WHERE e.embedding_model = :embeddingModel
                          AND a.effective_status = 'effective'
                          AND d.status = 'effective'
                        ORDER BY e.embedding <=> CAST(:queryEmbedding AS vector), a.article_order ASC
                        LIMIT :limit
                        """)
                .param("embeddingModel", embeddingModel)
                .param("queryEmbedding", toVectorLiteral(queryEmbedding))
                .param("limit", limit)
                .query((rs, rowNum) -> {
                    BigDecimal vectorScore = rs.getBigDecimal("vector_score");
                    return new RagSearchResult(
                            rs.getObject("article_id", UUID.class),
                            rs.getString("document_title"),
                            rs.getString("article_no"),
                            rs.getString("full_path"),
                            rs.getString("preview"),
                            VECTOR_MATCH_TYPE,
                            ZERO_KEYWORD_SCORE,
                            vectorScore,
                            vectorScore,
                            VECTOR_REASON
                    );
                })
                .list();
    }

    @Override
    public List<EmbeddableLawArticle> findArticlesMissingEmbeddings(String embeddingModel, int limit) {
        return jdbcClient.sql("""
                        SELECT
                            a.id AS article_id,
                            a.full_path,
                            a.content,
                            a.content_hash
                        FROM law_articles a
                        JOIN law_documents d ON d.id = a.document_id
                        LEFT JOIN law_article_embeddings e
                          ON e.article_id = a.id
                         AND e.embedding_model = :embeddingModel
                         AND e.content_hash = a.content_hash
                        WHERE a.effective_status = 'effective'
                          AND d.status = 'effective'
                          AND e.id IS NULL
                        ORDER BY a.article_order ASC
                        LIMIT :limit
                        """)
                .param("embeddingModel", embeddingModel)
                .param("limit", limit)
                .query((rs, rowNum) -> new EmbeddableLawArticle(
                        rs.getObject("article_id", UUID.class),
                        rs.getString("full_path"),
                        rs.getString("content"),
                        rs.getString("content_hash")
                ))
                .list();
    }

    @Override
    public void upsertArticleEmbedding(
            UUID articleId,
            String embeddingModel,
            String contentHash,
            List<BigDecimal> embedding
    ) {
        jdbcClient.sql("""
                        INSERT INTO law_article_embeddings (
                            article_id,
                            embedding_model,
                            content_hash,
                            embedding
                        )
                        VALUES (
                            :articleId,
                            :embeddingModel,
                            :contentHash,
                            CAST(:embedding AS vector)
                        )
                        ON CONFLICT (article_id, embedding_model)
                        DO UPDATE SET
                            content_hash = EXCLUDED.content_hash,
                            embedding = EXCLUDED.embedding,
                            updated_at = now()
                        """)
                .param("articleId", articleId)
                .param("embeddingModel", embeddingModel)
                .param("contentHash", contentHash)
                .param("embedding", toVectorLiteral(embedding))
                .update();
    }

    private static String toVectorLiteral(List<BigDecimal> embedding) {
        return embedding.stream()
                .map(BigDecimal::toPlainString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
