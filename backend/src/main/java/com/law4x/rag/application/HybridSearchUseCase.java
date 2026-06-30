package com.law4x.rag.application;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HybridSearchUseCase {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final BigDecimal ZERO_VECTOR_SCORE = BigDecimal.ZERO;
    private static final String KEYWORD_MATCH_TYPE = "keyword";
    private static final String KEYWORD_EXPANSION_MATCH_TYPE = "keyword_expansion";
    private static final String KEYWORD_REASON = "当前使用关键词检索命中，后续会叠加向量召回和 rerank。";
    private static final String KEYWORD_EXPANSION_REASON = "当前通过法律意图词扩展命中，后续会叠加向量召回和 rerank。";

    private final LawArticleRepository lawArticleRepository;

    public HybridSearchUseCase(LawArticleRepository lawArticleRepository) {
        this.lawArticleRepository = lawArticleRepository;
    }

    public List<RagSearchResult> search(String query, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        int normalizedLimit = normalizeLimit(limit);
        List<RagSearchResult> directResults = lawArticleRepository.searchEffectiveArticles(normalizedQuery, normalizedLimit)
                .stream()
                .map(result -> fromKeywordResult(result, KEYWORD_MATCH_TYPE, KEYWORD_REASON))
                .toList();
        if (!directResults.isEmpty()) {
            return directResults;
        }

        Map<UUID, RagSearchResult> expandedResults = new LinkedHashMap<>();
        for (String expandedQuery : expandQuery(normalizedQuery)) {
            lawArticleRepository.searchEffectiveArticles(expandedQuery, normalizedLimit)
                    .stream()
                    .map(result -> fromKeywordResult(result, KEYWORD_EXPANSION_MATCH_TYPE, KEYWORD_EXPANSION_REASON))
                    .forEach(result -> expandedResults.putIfAbsent(result.articleId(), result));
            if (expandedResults.size() >= normalizedLimit) {
                break;
            }
        }
        return new ArrayList<>(expandedResults.values()).stream()
                .limit(normalizedLimit)
                .toList();
    }

    private static RagSearchResult fromKeywordResult(
            LawArticleSearchResult result,
            String matchType,
            String reason
    ) {
        return new RagSearchResult(
                result.articleId(),
                result.documentTitle(),
                result.articleNo(),
                result.fullPath(),
                result.preview(),
                matchType,
                result.score(),
                ZERO_VECTOR_SCORE,
                result.score(),
                reason
        );
    }

    private static List<String> expandQuery(String query) {
        if (query.contains("欠钱") || query.contains("借钱") || query.contains("不还")) {
            return List.of("借款合同", "违约责任", "诉讼时效");
        }
        return List.of();
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return query.trim();
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
