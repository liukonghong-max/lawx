package com.law4x.law.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawArticleSearchResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcLawArticleRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/law4x",
        "spring.datasource.username=law4x",
        "spring.datasource.password=law4x_dev"
})
class JdbcLawArticleRepositoryTest {

    private final JdbcLawArticleRepository repository;
    private final JdbcClient jdbcClient;

    @Autowired
    JdbcLawArticleRepositoryTest(JdbcLawArticleRepository repository, JdbcClient jdbcClient, DataSource dataSource) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
        assertThat(dataSource).isNotNull();
    }

    @Test
    void findsArticleByExactArticleNumber() {
        List<LawArticleSearchResult> results = repository.searchEffectiveArticles("第五百七十七条", 3);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).articleId()).isNotNull();
        assertThat(results.get(0).documentTitle()).isEqualTo("中华人民共和国民法典");
        assertThat(results.get(0).articleNo()).isEqualTo("第五百七十七条");
        assertThat(results.get(0).fullPath()).contains("第八章 违约责任");
        assertThat(results.get(0).preview()).contains("不履行合同义务");

        Optional<LawArticleDetail> detail = repository.findArticleDetail(results.get(0).articleId());
        assertThat(detail).isPresent();
        assertThat(detail.get().articleNo()).isEqualTo("第五百七十七条");
    }

    @Test
    void findsArticlesByChapterKeyword() {
        List<LawArticleSearchResult> results = repository.searchEffectiveArticles("借款合同", 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).articleNo()).isEqualTo("第六百六十七条");
        assertThat(results.get(0).fullPath()).contains("第十二章 借款合同");
    }

    @Test
    void findsArticleDetailById() {
        UUID articleId = jdbcClient.sql("""
                        SELECT a.id
                        FROM law_articles a
                        JOIN law_documents d ON d.id = a.document_id
                        WHERE d.title = '中华人民共和国民法典'
                          AND a.article_no = '第五百七十七条'
                        """)
                .query(UUID.class)
                .single();

        Optional<LawArticleDetail> detail = repository.findArticleDetail(articleId);

        assertThat(detail).isPresent();
        assertThat(detail.get().articleId()).isEqualTo(articleId);
        assertThat(detail.get().documentTitle()).isEqualTo("中华人民共和国民法典");
        assertThat(detail.get().articleNo()).isEqualTo("第五百七十七条");
        assertThat(detail.get().content()).contains("承担继续履行、采取补救措施或者赔偿损失等违约责任");
        assertThat(detail.get().fullPath()).contains("第八章 违约责任");
        assertThat(detail.get().documentStatus()).isEqualTo("effective");
        assertThat(detail.get().effectiveStatus()).isEqualTo("effective");
        assertThat(detail.get().publishDate()).isNotNull();
        assertThat(detail.get().effectiveDate()).isNotNull();
    }
}
