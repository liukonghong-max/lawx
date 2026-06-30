package com.law4x.law.application;

import com.law4x.law.domain.model.LawArticleSearchResult;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchLawArticlesUseCase {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final LawArticleRepository lawArticleRepository;

    public SearchLawArticlesUseCase(LawArticleRepository lawArticleRepository) {
        this.lawArticleRepository = lawArticleRepository;
    }

    public List<LawArticleSearchResult> search(String query, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        int normalizedLimit = normalizeLimit(limit);
        return lawArticleRepository.searchEffectiveArticles(normalizedQuery, normalizedLimit);
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
