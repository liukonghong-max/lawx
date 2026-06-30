package com.law4x.law.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.law.domain.model.LawArticleSearchResult;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
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

    @Autowired
    JdbcLawArticleRepositoryTest(JdbcLawArticleRepository repository, DataSource dataSource) {
        this.repository = repository;
        assertThat(dataSource).isNotNull();
    }

    @Test
    void findsArticleByExactArticleNumber() {
        List<LawArticleSearchResult> results = repository.searchEffectiveArticles("第五百七十七条", 3);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).documentTitle()).isEqualTo("中华人民共和国民法典");
        assertThat(results.get(0).articleNo()).isEqualTo("第五百七十七条");
        assertThat(results.get(0).fullPath()).contains("第八章 违约责任");
        assertThat(results.get(0).preview()).contains("不履行合同义务");
    }

    @Test
    void findsArticlesByChapterKeyword() {
        List<LawArticleSearchResult> results = repository.searchEffectiveArticles("借款合同", 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).articleNo()).isEqualTo("第六百六十七条");
        assertThat(results.get(0).fullPath()).contains("第十二章 借款合同");
    }
}
