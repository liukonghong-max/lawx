package com.law4x.law.infrastructure.persistence;

import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLawArticleRepository implements LawArticleRepository {

    private final JdbcClient jdbcClient;

    public JdbcLawArticleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit) {
        return jdbcClient.sql("""
                        SELECT
                            a.id AS article_id,
                            d.title AS document_title,
                            a.article_no,
                            a.full_path,
                            left(a.content, 180) AS preview,
                            (
                                CASE WHEN a.article_no = :query THEN 100 ELSE 0 END
                                + CASE WHEN d.title ILIKE '%' || :query || '%' THEN 20 ELSE 0 END
                                + CASE WHEN a.full_path ILIKE '%' || :query || '%' THEN 15 ELSE 0 END
                                + CASE WHEN a.content ILIKE '%' || :query || '%' THEN 10 ELSE 0 END
                                + similarity(a.content, :query) * 10
                                + similarity(a.full_path, :query) * 5
                            ) AS score
                        FROM law_articles a
                        JOIN law_documents d ON d.id = a.document_id
                        WHERE a.effective_status = 'effective'
                          AND d.status = 'effective'
                          AND (
                              a.article_no = :query
                              OR d.title ILIKE '%' || :query || '%'
                              OR a.full_path ILIKE '%' || :query || '%'
                              OR a.content ILIKE '%' || :query || '%'
                              OR similarity(a.content, :query) > 0.05
                              OR similarity(a.full_path, :query) > 0.1
                          )
                        ORDER BY score DESC, a.article_order ASC
                        LIMIT :limit
                        """)
                .param("query", query)
                .param("limit", limit)
                .query((rs, rowNum) -> new LawArticleSearchResult(
                        rs.getObject("article_id", UUID.class),
                        rs.getString("document_title"),
                        rs.getString("article_no"),
                        rs.getString("full_path"),
                        rs.getString("preview"),
                        rs.getBigDecimal("score")
                ))
                .list();
    }

    @Override
    public Optional<LawArticleDetail> findArticleDetail(UUID articleId) {
        return jdbcClient.sql("""
                        SELECT
                            a.id AS article_id,
                            d.title AS document_title,
                            d.law_type,
                            d.issuer,
                            d.publish_date,
                            d.effective_date,
                            d.status AS document_status,
                            d.source_url,
                            a.book_title,
                            a.chapter_title,
                            a.section_title,
                            a.article_no,
                            a.article_order,
                            a.content,
                            a.full_path,
                            a.effective_status
                        FROM law_articles a
                        JOIN law_documents d ON d.id = a.document_id
                        WHERE a.id = :articleId
                        """)
                .param("articleId", articleId)
                .query((rs, rowNum) -> new LawArticleDetail(
                        rs.getObject("article_id", UUID.class),
                        rs.getString("document_title"),
                        rs.getString("law_type"),
                        rs.getString("issuer"),
                        rs.getObject("publish_date", java.time.LocalDate.class),
                        rs.getObject("effective_date", java.time.LocalDate.class),
                        rs.getString("document_status"),
                        rs.getString("source_url"),
                        rs.getString("book_title"),
                        rs.getString("chapter_title"),
                        rs.getString("section_title"),
                        rs.getString("article_no"),
                        rs.getInt("article_order"),
                        rs.getString("content"),
                        rs.getString("full_path"),
                        rs.getString("effective_status")
                ))
                .optional();
    }
}
