package com.law4x.law.domain.repository;

import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawArticleSearchResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LawArticleRepository {
    List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit);

    Optional<LawArticleDetail> findArticleDetail(UUID articleId);
}
