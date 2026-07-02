package com.law4x.agui.application.service;

import com.law4x.rag.application.HybridSearchUseCase;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.infrastructure.embedding.DashScopeEmbeddingProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AgUiConversationStateService {

    private static final Pattern SEGMENT_SPLIT_PATTERN = Pattern.compile("(?<=[。？！；\\n])");
    private static final Pattern CITATION_MARK_PATTERN = Pattern.compile("\\[(\\d+)\\]");
    private static final int DEFAULT_LIMIT = 5;

    private final HybridSearchUseCase hybridSearchUseCase;
    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    public AgUiConversationStateService(
            HybridSearchUseCase hybridSearchUseCase,
            DashScopeEmbeddingProperties dashScopeEmbeddingProperties
    ) {
        this.hybridSearchUseCase = hybridSearchUseCase;
        this.dashScopeEmbeddingProperties = dashScopeEmbeddingProperties;
    }

    public Map<String, Object> buildFinalState(String query, String answer, Map<String, Object> currentState) {
        LinkedHashMap<String, Object> state = new LinkedHashMap<>();
        if (currentState != null) {
            state.putAll(currentState);
        }

        String normalizedAnswer = answer == null ? "" : answer;
        List<CitationItem> citations = extractCitations(currentState);
        if (citations.isEmpty()) {
            citations = buildCitations(query);
        }
        state.put("answer", normalizedAnswer);
        state.put("citations", citations.stream().map(CitationItem::toMap).toList());
        state.put("answerSegments", buildAnswerSegments(normalizedAnswer, citations));
        return state;
    }

    private List<CitationItem> extractCitations(Map<String, Object> currentState) {
        if (currentState == null) {
            return List.of();
        }
        Object citationsValue = currentState.get("citations");
        if (!(citationsValue instanceof List<?> citationMaps)) {
            return List.of();
        }
        List<CitationItem> citations = new ArrayList<>();
        for (Object citationValue : citationMaps) {
            if (!(citationValue instanceof Map<?, ?> citationMap)) {
                continue;
            }
            citations.add(new CitationItem(
                    stringValue(citationMap.get("articleId")),
                    stringValue(citationMap.get("documentTitle")),
                    stringValue(citationMap.get("articleNo")),
                    stringValue(citationMap.get("fullPath")),
                    stringValue(citationMap.get("quotedText"))
            ));
        }
        return citations;
    }

    private List<CitationItem> buildCitations(String query) {
        if (query == null || query.trim().isBlank()) {
            return List.of();
        }
        String embeddingModel = dashScopeEmbeddingProperties.getModelName();
        List<RagSearchResult> results = hybridSearchUseCase.search(query.trim(), embeddingModel, DEFAULT_LIMIT);
        LinkedHashSet<UUID> seen = new LinkedHashSet<>();
        List<CitationItem> citations = new ArrayList<>();
        for (RagSearchResult result : results) {
            if (result.articleId() == null || !seen.add(result.articleId())) {
                continue;
            }
            citations.add(new CitationItem(
                    result.articleId().toString(),
                    result.documentTitle(),
                    result.articleNo(),
                    result.fullPath(),
                    result.preview()
            ));
        }
        return citations;
    }

    private List<Map<String, Object>> buildAnswerSegments(String answer, List<CitationItem> citations) {
        if (answer == null || answer.isBlank()) {
            return List.of();
        }
        String[] parts = SEGMENT_SPLIT_PATTERN.split(answer);
        List<Map<String, Object>> segments = new ArrayList<>();
        int index = 0;
        for (String part : parts) {
            String original = part.trim();
            if (original.isBlank()) {
                continue;
            }
            List<String> citationIds = new ArrayList<>();
            Matcher matcher = CITATION_MARK_PATTERN.matcher(original);
            while (matcher.find()) {
                try {
                    int citationIndex = Integer.parseInt(matcher.group(1)) - 1;
                    if (citationIndex >= 0 && citationIndex < citations.size()) {
                        citationIds.add(citations.get(citationIndex).articleId());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            String cleanText = CITATION_MARK_PATTERN.matcher(original).replaceAll("").trim();
            if (cleanText.isBlank()) {
                continue;
            }
            segments.add(Map.of(
                    "id", "seg-" + (++index),
                    "text", cleanText,
                    "citationIds", citationIds
            ));
        }
        return segments;
    }

    private record CitationItem(
            String articleId,
            String documentTitle,
            String articleNo,
            String fullPath,
            String quotedText
    ) {
        private Map<String, Object> toMap() {
            return Map.of(
                    "articleId", articleId,
                    "documentTitle", documentTitle == null ? "" : documentTitle,
                    "articleNo", articleNo == null ? "" : articleNo,
                    "fullPath", fullPath == null ? "" : fullPath,
                    "quotedText", quotedText == null ? "" : quotedText
            );
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
