package com.law4x.rag.application;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.EmbeddingClient;
import com.law4x.rag.domain.repository.LawArticleEmbeddingRepository;
import java.math.BigDecimal;
import java.util.Comparator;
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
    private static final String HYBRID_MATCH_TYPE = "hybrid";
    private static final String KEYWORD_REASON = "当前使用关键词检索命中，后续会叠加向量召回和 rerank。";
    private static final String HYBRID_REASON = "当前使用关键词检索和 pgvector 向量检索共同命中。";

    private final LawArticleRepository lawArticleRepository;
    private final LawArticleEmbeddingRepository lawArticleEmbeddingRepository;
    private final EmbeddingClient embeddingClient;

    public HybridSearchUseCase(
            LawArticleRepository lawArticleRepository,
            LawArticleEmbeddingRepository lawArticleEmbeddingRepository,
            EmbeddingClient embeddingClient
    ) {
        this.lawArticleRepository = lawArticleRepository;
        this.lawArticleEmbeddingRepository = lawArticleEmbeddingRepository;
        this.embeddingClient = embeddingClient;
    }

    public List<RagSearchResult> search(String query, String embeddingModel, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        String normalizedEmbeddingModel = normalizeEmbeddingModel(embeddingModel);
        int normalizedLimit = normalizeLimit(limit);
        List<RagSearchResult> keywordResults = lawArticleRepository.searchEffectiveArticles(normalizedQuery, normalizedLimit)
                .stream()
                .map(result -> fromKeywordResult(result, KEYWORD_MATCH_TYPE, KEYWORD_REASON))
                .toList();
        List<BigDecimal> queryEmbedding = embeddingClient.embed(normalizedQuery, normalizedEmbeddingModel);
        List<RagSearchResult> vectorResults = lawArticleEmbeddingRepository.searchSimilarArticles(
                normalizedEmbeddingModel,
                queryEmbedding,
                normalizedLimit
        );
        return mergeResults(keywordResults, vectorResults, normalizedLimit);
    }

    private static List<RagSearchResult> mergeResults(
            List<RagSearchResult> keywordResults,
            List<RagSearchResult> vectorResults,
            int limit
    ) {
        Map<UUID, RagSearchResult> merged = new LinkedHashMap<>();
        for (RagSearchResult result : vectorResults) {
            merged.put(result.articleId(), result);
        }
        for (RagSearchResult keywordResult : keywordResults) {
            RagSearchResult vectorResult = merged.get(keywordResult.articleId());
            if (vectorResult == null) {
                merged.put(keywordResult.articleId(), keywordResult);
            } else {
                merged.put(keywordResult.articleId(), hybridResult(keywordResult, vectorResult));
            }
        }
        return merged.values()
                .stream()
                .sorted(Comparator.comparing(RagSearchResult::finalScore).reversed())
                .limit(limit)
                .toList();
    }

    private static RagSearchResult hybridResult(RagSearchResult keywordResult, RagSearchResult vectorResult) {
        BigDecimal finalScore = keywordResult.keywordScore().add(vectorResult.vectorScore());
        return new RagSearchResult(
                keywordResult.articleId(),
                keywordResult.documentTitle(),
                keywordResult.articleNo(),
                keywordResult.fullPath(),
                keywordResult.preview(),
                HYBRID_MATCH_TYPE,
                keywordResult.keywordScore(),
                vectorResult.vectorScore(),
                finalScore,
                HYBRID_REASON
        );
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

    private static String normalizeQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return query.trim();
    }

    private static String normalizeEmbeddingModel(String embeddingModel) {
        if (embeddingModel == null || embeddingModel.trim().isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        return embeddingModel.trim();
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
