package com.law4x.rag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.model.RagTestRun;
import com.law4x.rag.domain.repository.RagTestRunRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateRagTestRunUseCaseTest {

    @Test
    void rejectsBlankQuery() {
        CreateRagTestRunUseCase useCase = new CreateRagTestRunUseCase(
                new FakeHybridSearchUseCase(),
                new FakeRagTestRunRepository()
        );

        assertThatThrownBy(() -> useCase.create("  ", "text-embedding-v4", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query must not be blank");
    }

    @Test
    void searchesAndPersistsRagTestRunSnapshot() {
        UUID articleId = UUID.randomUUID();
        FakeHybridSearchUseCase searchUseCase = new FakeHybridSearchUseCase();
        searchUseCase.results.add(new RagSearchResult(
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
        ));
        FakeRagTestRunRepository repository = new FakeRagTestRunRepository();
        CreateRagTestRunUseCase useCase = new CreateRagTestRunUseCase(searchUseCase, repository);

        CreateRagTestRunUseCase.CreateResult result =
                useCase.create(" 别人欠钱不还怎么办 ", "text-embedding-v4", 50);

        assertThat(searchUseCase.lastQuery).isEqualTo("别人欠钱不还怎么办");
        assertThat(searchUseCase.lastEmbeddingModel).isEqualTo("text-embedding-v4");
        assertThat(searchUseCase.lastLimit).isEqualTo(20);
        assertThat(repository.savedRun.query()).isEqualTo("别人欠钱不还怎么办");
        assertThat(repository.savedRun.keywordResults()).isEmpty();
        assertThat(repository.savedRun.vectorResults()).hasSize(1);
        assertThat(repository.savedRun.rerankResults()).isEmpty();
        assertThat(repository.savedRun.selectedArticleIds()).containsExactly(articleId);
        assertThat(repository.savedRun.parameters().embeddingModel()).isEqualTo("text-embedding-v4");
        assertThat(repository.savedRun.parameters().limit()).isEqualTo(20);
        assertThat(result.runId()).isEqualTo(repository.savedRun.id());
        assertThat(result.results()).hasSize(1);
    }

    private static class FakeHybridSearchUseCase extends HybridSearchUseCase {
        private final List<RagSearchResult> results = new ArrayList<>();
        private String lastQuery;
        private String lastEmbeddingModel;
        private Integer lastLimit;

        FakeHybridSearchUseCase() {
            super(null, null, null);
        }

        @Override
        public List<RagSearchResult> search(String query, String embeddingModel, Integer limit) {
            lastQuery = query;
            lastEmbeddingModel = embeddingModel;
            lastLimit = limit;
            return results;
        }
    }

    private static class FakeRagTestRunRepository implements RagTestRunRepository {
        private RagTestRun savedRun;

        @Override
        public RagTestRun save(RagTestRun testRun) {
            savedRun = testRun.withId(UUID.randomUUID());
            return savedRun;
        }
    }
}
