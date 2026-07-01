package com.law4x.rag.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.model.RagTestRun;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcRagTestRunRepository.class, JdbcRagTestRunRepositoryTest.TestConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/law4x",
        "spring.datasource.username=law4x",
        "spring.datasource.password=law4x_dev"
})
class JdbcRagTestRunRepositoryTest {

    private final JdbcRagTestRunRepository repository;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    @Autowired
    JdbcRagTestRunRepositoryTest(
            JdbcRagTestRunRepository repository,
            JdbcClient jdbcClient,
            DataSource dataSource,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        assertThat(dataSource).isNotNull();
    }

    @Test
    void savesRagTestRunSnapshot() throws Exception {
        UUID articleId = UUID.randomUUID();
        RagTestRun saved = repository.save(new RagTestRun(
                null,
                "别人欠钱不还怎么办",
                List.of(),
                List.of(new RagSearchResult(
                        articleId,
                        "中华人民共和国民法典",
                        "第六百七十五条",
                        "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                        "借款人应当按照约定的期限返还借款。",
                        "vector",
                        BigDecimal.ZERO,
                        new BigDecimal("0.91"),
                        new BigDecimal("0.91"),
                        "当前使用 pgvector 向量检索命中。"
                )),
                List.of(),
                List.of(articleId),
                new RagTestRun.Parameters("text-embedding-v4", 5)
        ));

        SavedRow row = jdbcClient.sql("""
                        SELECT query,
                               vector_results::text,
                               selected_article_ids::text,
                               parameters::text
                        FROM rag_test_runs
                        WHERE id = :id
                        """)
                .param("id", saved.id())
                .query((rs, rowNum) -> new SavedRow(
                        rs.getString("query"),
                        rs.getString("vector_results"),
                        rs.getString("selected_article_ids"),
                        rs.getString("parameters")
                ))
                .single();
        JsonNode vectorResults = objectMapper.readTree(row.vectorResults());
        JsonNode parameters = objectMapper.readTree(row.parameters());

        assertThat(saved.id()).isNotNull();
        assertThat(row.query()).isEqualTo("别人欠钱不还怎么办");
        assertThat(vectorResults).hasSize(1);
        assertThat(vectorResults.get(0).get("articleId").asText()).isEqualTo(articleId.toString());
        assertThat(row.selectedArticleIds()).contains(articleId.toString());
        assertThat(parameters.get("embeddingModel").asText()).isEqualTo("text-embedding-v4");
        assertThat(parameters.get("limit").asInt()).isEqualTo(5);
    }

    private record SavedRow(
            String query,
            String vectorResults,
            String selectedArticleIds,
            String parameters
    ) {
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
