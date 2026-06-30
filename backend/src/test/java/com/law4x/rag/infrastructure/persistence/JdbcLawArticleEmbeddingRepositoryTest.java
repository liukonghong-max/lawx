package com.law4x.rag.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.rag.domain.model.EmbeddableLawArticle;
import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcLawArticleEmbeddingRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/law4x",
        "spring.datasource.username=law4x",
        "spring.datasource.password=law4x_dev"
})
class JdbcLawArticleEmbeddingRepositoryTest {

    private static final String MODEL_NAME = "test-embedding-model";

    private final JdbcLawArticleEmbeddingRepository repository;
    private final JdbcClient jdbcClient;

    @Autowired
    JdbcLawArticleEmbeddingRepositoryTest(
            JdbcLawArticleEmbeddingRepository repository,
            JdbcClient jdbcClient,
            DataSource dataSource
    ) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
        assertThat(dataSource).isNotNull();
    }

    @Test
    void searchesSimilarArticlesByPgvectorDistance() {
        UUID documentId = insertDocument();
        UUID closeArticleId = insertArticle(documentId, "测试第一条", 1, "借款人应当按照约定的期限返还借款。");
        UUID farArticleId = insertArticle(documentId, "测试第二条", 2, "物业服务人应当按照约定提供物业服务。");
        insertEmbedding(closeArticleId, oneHotVector(0));
        insertEmbedding(farArticleId, oneHotVector(1));

        List<RagSearchResult> results = repository.searchSimilarArticles(MODEL_NAME, oneHotVector(0), 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).articleId()).isEqualTo(closeArticleId);
        assertThat(results.get(0).articleNo()).isEqualTo("测试第一条");
        assertThat(results.get(0).matchType()).isEqualTo("vector");
        assertThat(results.get(0).keywordScore()).isEqualByComparingTo("0");
        assertThat(results.get(0).vectorScore()).isGreaterThan(results.get(1).vectorScore());
        assertThat(results.get(0).reason()).isEqualTo("当前使用 pgvector 向量检索命中。");
    }

    @Test
    void findsMissingEmbeddingsAndUpsertsGeneratedEmbedding() {
        UUID documentId = insertDocument();
        String contentHash = UUID.randomUUID().toString();
        UUID articleId = insertArticle(documentId, "测试第三条", 3, "承租人应当按照约定支付租金。", contentHash);

        List<EmbeddableLawArticle> missingArticles = repository.findArticlesMissingEmbeddings(MODEL_NAME, 10);

        assertThat(missingArticles).extracting(EmbeddableLawArticle::articleId).contains(articleId);

        repository.upsertArticleEmbedding(articleId, MODEL_NAME, contentHash, oneHotVector(2));

        Integer count = jdbcClient.sql("""
                        SELECT count(*)
                        FROM law_article_embeddings
                        WHERE article_id = :articleId
                          AND embedding_model = :embeddingModel
                          AND content_hash = :contentHash
                        """)
                .param("articleId", articleId)
                .param("embeddingModel", MODEL_NAME)
                .param("contentHash", contentHash)
                .query(Integer.class)
                .single();
        assertThat(count).isEqualTo(1);
        assertThat(repository.findArticlesMissingEmbeddings(MODEL_NAME, 10))
                .extracting(EmbeddableLawArticle::articleId)
                .doesNotContain(articleId);

        repository.upsertArticleEmbedding(articleId, MODEL_NAME, "content-hash-v2", oneHotVector(3));

        Integer updatedCount = jdbcClient.sql("""
                        SELECT count(*)
                        FROM law_article_embeddings
                        WHERE article_id = :articleId
                          AND embedding_model = :embeddingModel
                          AND content_hash = 'content-hash-v2'
                        """)
                .param("articleId", articleId)
                .param("embeddingModel", MODEL_NAME)
                .query(Integer.class)
                .single();
        assertThat(updatedCount).isEqualTo(1);
    }

    private UUID insertDocument() {
        return jdbcClient.sql("""
                        INSERT INTO law_documents (title, law_type, issuer, status)
                        VALUES ('测试向量法规', '法律', '测试机关', 'effective')
                        RETURNING id
                        """)
                .query(UUID.class)
                .single();
    }

    private UUID insertArticle(UUID documentId, String articleNo, int articleOrder, String content) {
        return insertArticle(documentId, articleNo, articleOrder, content, UUID.randomUUID().toString());
    }

    private UUID insertArticle(
            UUID documentId,
            String articleNo,
            int articleOrder,
            String content,
            String contentHash
    ) {
        return jdbcClient.sql("""
                        INSERT INTO law_articles (
                            document_id,
                            article_no,
                            article_order,
                            content,
                            full_path,
                            effective_status,
                            content_hash
                        )
                        VALUES (
                            :documentId,
                            :articleNo,
                            :articleOrder,
                            :content,
                            '测试向量法规 > ' || :articleNo,
                            'effective',
                            :contentHash
                        )
                        RETURNING id
                        """)
                .param("documentId", documentId)
                .param("articleNo", articleNo)
                .param("articleOrder", articleOrder)
                .param("content", content)
                .param("contentHash", contentHash)
                .query(UUID.class)
                .single();
    }

    private void insertEmbedding(UUID articleId, List<BigDecimal> embedding) {
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
                        """)
                .param("articleId", articleId)
                .param("embeddingModel", MODEL_NAME)
                .param("contentHash", UUID.randomUUID().toString())
                .param("embedding", toVectorLiteral(embedding))
                .update();
    }

    private static List<BigDecimal> oneHotVector(int hotIndex) {
        return java.util.stream.IntStream.range(0, 1536)
                .mapToObj(index -> index == hotIndex ? BigDecimal.ONE : BigDecimal.ZERO)
                .toList();
    }

    private static String toVectorLiteral(List<BigDecimal> embedding) {
        return embedding.stream()
                .map(BigDecimal::toPlainString)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
