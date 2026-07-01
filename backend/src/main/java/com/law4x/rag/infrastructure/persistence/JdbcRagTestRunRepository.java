package com.law4x.rag.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.model.RagTestRun;
import com.law4x.rag.domain.repository.RagTestRunRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRagTestRunRepository implements RagTestRunRepository {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public JdbcRagTestRunRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RagTestRun save(RagTestRun testRun) {
        UUID id = jdbcClient.sql("""
                        INSERT INTO rag_test_runs (
                            query,
                            keyword_results,
                            vector_results,
                            rerank_results,
                            selected_article_ids,
                            parameters
                        )
                        VALUES (
                            :query,
                            CAST(:keywordResults AS jsonb),
                            CAST(:vectorResults AS jsonb),
                            CAST(:rerankResults AS jsonb),
                            CAST(:selectedArticleIds AS uuid[]),
                            CAST(:parameters AS jsonb)
                        )
                        RETURNING id
                        """)
                .param("query", testRun.query())
                .param("keywordResults", toJson(testRun.keywordResults()))
                .param("vectorResults", toJson(testRun.vectorResults()))
                .param("rerankResults", toJson(testRun.rerankResults()))
                .param("selectedArticleIds", toUuidArrayLiteral(testRun.selectedArticleIds()))
                .param("parameters", toJson(testRun.parameters()))
                .query(UUID.class)
                .single();
        return testRun.withId(id);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize rag test run", exception);
        }
    }

    private static String toUuidArrayLiteral(List<UUID> ids) {
        return ids.stream()
                .map(UUID::toString)
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
}
