package com.law4x.law.domain.repository;

import com.law4x.law.domain.model.LawArticleSearchResult;
import java.util.List;

public interface LawArticleRepository {
    List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit);
}
