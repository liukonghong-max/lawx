package com.law4x.law.infrastructure.persistence;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.util.List;
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
                        rs.getString("document_title"),
                        rs.getString("article_no"),
                        rs.getString("full_path"),
                        rs.getString("preview"),
                        rs.getBigDecimal("score")
                ))
                .list();
    }
}
