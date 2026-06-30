package com.law4x.rag.application;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import com.law4x.rag.domain.model.RagSearchResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HybridSearchUseCase {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final BigDecimal ZERO_VECTOR_SCORE = BigDecimal.ZERO;
    private static final String KEYWORD_MATCH_TYPE = "keyword";
    private static final String KEYWORD_REASON = "当前使用关键词检索命中，后续会叠加向量召回和 rerank。";

    private final LawArticleRepository lawArticleRepository;

    public HybridSearchUseCase(LawArticleRepository lawArticleRepository) {
        this.lawArticleRepository = lawArticleRepository;
    }

    public List<RagSearchResult> search(String query, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        int normalizedLimit = normalizeLimit(limit);
        return lawArticleRepository.searchEffectiveArticles(normalizedQuery, normalizedLimit)
                .stream()
                .map(HybridSearchUseCase::fromKeywordResult)
                .toList();
    }

    private static RagSearchResult fromKeywordResult(LawArticleSearchResult result) {
        return new RagSearchResult(
                result.articleId(),
                result.documentTitle(),
                result.articleNo(),
                result.fullPath(),
                result.preview(),
                KEYWORD_MATCH_TYPE,
                result.score(),
                ZERO_VECTOR_SCORE,
                result.score(),
                KEYWORD_REASON
        );
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
