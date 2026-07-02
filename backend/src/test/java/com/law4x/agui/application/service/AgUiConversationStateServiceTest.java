package com.law4x.agui.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.rag.application.HybridSearchUseCase;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgUiConversationStateServiceTest {

    @Test
    void buildsFinalStateFromAnswerAndSearchResults() {
        FakeHybridSearchUseCase searchUseCase = new FakeHybridSearchUseCase();
        searchUseCase.results = List.of(
                new RagSearchResult(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "中华人民共和国民法典",
                        "第六百七十五条",
                        "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                        "借款人应当按照约定的期限返还借款。",
                        "hybrid",
                        new BigDecimal("42.50"),
                        BigDecimal.ZERO,
                        new BigDecimal("42.50"),
                        "关键词命中"
                )
        );
        DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
        properties.setModelName("text-embedding-v4");
        AgUiConversationStateService service = new AgUiConversationStateService(searchUseCase, properties);

        Map<String, Object> state = service.buildFinalState(
                "别人欠钱不还怎么办",
                "可以要求对方返还借款。[1] 如协商无果，可起诉。",
                Map.of("threadMode", "consultation")
        );

        assertThat(searchUseCase.query).isEqualTo("别人欠钱不还怎么办");
        assertThat(searchUseCase.embeddingModel).isEqualTo("text-embedding-v4");
        assertThat(searchUseCase.limit).isEqualTo(5);
        assertThat(state).containsEntry("threadMode", "consultation");
        assertThat(state).containsEntry("answer", "可以要求对方返还借款。[1] 如协商无果，可起诉。");
        assertThat((List<?>) state.get("citations")).hasSize(1);
        assertThat((List<?>) state.get("answerSegments")).hasSize(2);
        assertThat((List<?>) state.get("answerSegments"))
                .anySatisfy(item -> assertThat(((Map<?, ?>) item).get("citationIds"))
                        .isEqualTo(List.of("11111111-1111-1111-1111-111111111111")));
    }

    @Test
    void reusesPreloadedCitationsFromCurrentState() {
        FakeHybridSearchUseCase searchUseCase = new FakeHybridSearchUseCase();
        DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
        properties.setModelName("text-embedding-v4");
        AgUiConversationStateService service = new AgUiConversationStateService(searchUseCase, properties);

        Map<String, Object> state = service.buildFinalState(
                "别人欠钱不还怎么办",
                "可以要求对方返还借款[1]",
                Map.of(
                        "citations", List.of(Map.of(
                                "articleId", "11111111-1111-1111-1111-111111111111",
                                "documentTitle", "中华人民共和国民法典",
                                "articleNo", "第六百七十五条",
                                "fullPath", "中华人民共和国民法典 > 第三编 合同 > 第十二章 借款合同 > 第六百七十五条",
                                "quotedText", "借款人应当按照约定的期限返还借款。"
                        ))
                )
        );

        assertThat(searchUseCase.query).isNull();
        assertThat((List<?>) state.get("citations")).hasSize(1);
        assertThat((List<?>) state.get("answerSegments"))
                .anySatisfy(item -> assertThat(((Map<?, ?>) item).get("citationIds"))
                        .isEqualTo(List.of("11111111-1111-1111-1111-111111111111")));
    }

    private static final class FakeHybridSearchUseCase extends HybridSearchUseCase {
        private String query;
        private String embeddingModel;
        private Integer limit;
        private List<RagSearchResult> results = List.of();

        private FakeHybridSearchUseCase() {
            super(null, null, null);
        }

        @Override
        public List<RagSearchResult> search(String query, String embeddingModel, Integer limit) {
            this.query = query;
            this.embeddingModel = embeddingModel;
            this.limit = limit;
            return results;
        }
    }
}
