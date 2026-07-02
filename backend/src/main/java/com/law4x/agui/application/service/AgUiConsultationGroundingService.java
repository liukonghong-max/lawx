package com.law4x.agui.application.service;

import com.law4x.rag.application.HybridSearchUseCase;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AgUiConsultationGroundingService {

    private static final int DEFAULT_LIMIT = 5;

    private final HybridSearchUseCase hybridSearchUseCase;
    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    public AgUiConsultationGroundingService(
            HybridSearchUseCase hybridSearchUseCase,
            DashScopeEmbeddingProperties dashScopeEmbeddingProperties
    ) {
        this.hybridSearchUseCase = hybridSearchUseCase;
        this.dashScopeEmbeddingProperties = dashScopeEmbeddingProperties;
    }

    public PreparedGrounding prepare(String query, Map<String, Object> currentState, String runId) {
        LinkedHashMap<String, Object> state = new LinkedHashMap<>();
        if (currentState != null) {
            state.putAll(currentState);
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery == null) {
            return new PreparedGrounding(state, "");
        }

        String embeddingModel = dashScopeEmbeddingProperties.getModelName();
        List<CitationItem> citations = hybridSearchUseCase.search(normalizedQuery, embeddingModel, DEFAULT_LIMIT).stream()
                .map(CitationItem::from)
                .toList();
        List<String> allowedCitationIds = citations.stream()
                .map(CitationItem::articleId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        state.put("groundingQuery", normalizedQuery);
        state.put("citations", citations.stream().map(CitationItem::toMap).toList());
        state.put("allowedCitationIds", allowedCitationIds);

        return new PreparedGrounding(
                state,
                buildGroundingPrompt(normalizedQuery, citations, allowedCitationIds)
        );
    }

    private String normalize(String query) {
        if (query == null) {
            return null;
        }
        String normalized = query.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String buildGroundingPrompt(
            String query,
            List<CitationItem> citations,
            List<String> allowedCitationIds
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("系统已在服务端完成本轮法规检索。")
                .append("你不得自行决定是否检索，也不要调用 searchLawArticles。")
                .append("你必须仅基于以下候选法条回答；如需要完整正文，只能调用 getArticleDetail。")
                .append("输出最终回答前，必须调用 validateCitations，且 allowedCitationIds 只能使用本消息提供的列表。")
                .append("\n\n")
                .append("用户本轮问题：").append(query).append("\n")
                .append("allowedCitationIds: ").append(allowedCitationIds).append("\n");

        if (citations.isEmpty()) {
            builder.append("候选法条：未检索到足够依据。请明确说明信息不足，并提示用户补充事实，不要编造法条或引用。");
            return builder.toString();
        }

        builder.append("候选法条：\n");
        for (int index = 0; index < citations.size(); index++) {
            CitationItem citation = citations.get(index);
            builder.append(index + 1)
                    .append(". articleId=").append(nullSafe(citation.articleId()))
                    .append(" | ").append(nullSafe(citation.documentTitle()))
                    .append(" ").append(nullSafe(citation.articleNo()))
                    .append("\n")
                    .append("   路径：").append(nullSafe(citation.fullPath()))
                    .append("\n")
                    .append("   摘录：").append(nullSafe(citation.quotedText()))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public record PreparedGrounding(
            Map<String, Object> state,
            String groundingPrompt
    ) {
    }

    private record CitationItem(
            String articleId,
            String documentTitle,
            String articleNo,
            String fullPath,
            String quotedText
    ) {
        private static CitationItem from(RagSearchResult result) {
            return new CitationItem(
                    result.articleId() == null ? "" : result.articleId().toString(),
                    result.documentTitle(),
                    result.articleNo(),
                    result.fullPath(),
                    result.preview()
            );
        }

        private Map<String, Object> toMap() {
            return Map.of(
                    "articleId", articleId == null ? "" : articleId,
                    "documentTitle", documentTitle == null ? "" : documentTitle,
                    "articleNo", articleNo == null ? "" : articleNo,
                    "fullPath", fullPath == null ? "" : fullPath,
                    "quotedText", quotedText == null ? "" : quotedText
            );
        }
    }
}
