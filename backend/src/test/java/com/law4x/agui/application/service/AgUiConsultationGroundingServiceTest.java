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

class AgUiConsultationGroundingServiceTest {

    @Test
    void preparesGroundingWithHybridSearchResults() {
        RecordingHybridSearchUseCase searchUseCase = new RecordingHybridSearchUseCase();
        DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
        properties.setModelName("text-embedding-v4");
        AgUiConsultationGroundingService service =
                new AgUiConsultationGroundingService(searchUseCase, properties);

        AgUiConsultationGroundingService.PreparedGrounding grounding =
                service.prepare("公司拖欠工资怎么办", Map.of("threadId", "thread-001"), "run-001");

        assertThat(searchUseCase.query).isEqualTo("公司拖欠工资怎么办");
        assertThat(searchUseCase.embeddingModel).isEqualTo("text-embedding-v4");
        assertThat(searchUseCase.limit).isEqualTo(5);
        assertThat(grounding.state())
                .containsEntry("groundingQuery", "公司拖欠工资怎么办")
                .containsKey("citations")
                .containsKey("allowedCitationIds");
        assertThat(grounding.groundingPrompt())
                .contains("系统已在服务端完成本轮法规检索")
                .contains("allowedCitationIds: [11111111-1111-1111-1111-111111111111]")
                .contains("中华人民共和国劳动合同法")
                .contains("第八十五条");
    }

    @Test
    void returnsInsufficientGroundingWhenNoResults() {
        RecordingHybridSearchUseCase searchUseCase = new RecordingHybridSearchUseCase();
        searchUseCase.results = List.of();
        DashScopeEmbeddingProperties properties = new DashScopeEmbeddingProperties();
        AgUiConsultationGroundingService service =
                new AgUiConsultationGroundingService(searchUseCase, properties);

        AgUiConsultationGroundingService.PreparedGrounding grounding =
                service.prepare(" ", Map.of(), "run-001");

        assertThat(grounding.groundingPrompt()).isEmpty();
        assertThat(searchUseCase.query).isNull();
    }

    private static final class RecordingHybridSearchUseCase extends HybridSearchUseCase {
        private String query;
        private String embeddingModel;
        private Integer limit;
        private List<RagSearchResult> results = List.of(
                new RagSearchResult(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "中华人民共和国劳动合同法",
                        "第八十五条",
                        "第七章 法律责任/第八十五条",
                        "用人单位未按照劳动合同的约定或者国家规定及时足额支付劳动者劳动报酬的，应当向劳动者支付赔偿金。",
                        "hybrid",
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.valueOf(2),
                        "当前使用关键词检索和 pgvector 向量检索共同命中。"
                )
        );

        private RecordingHybridSearchUseCase() {
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
