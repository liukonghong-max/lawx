package com.law4x.law.domain.repository;

import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.model.LawDocumentArticleItem;
import com.law4x.law.domain.model.LawDocumentSummary;
import com.law4x.law.domain.model.LawArticleSearchResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LawArticleRepository {
    record PagedResult<T>(
            List<T> items,
            int page,
            int pageSize,
            long total
    ) {
    }

    List<LawDocumentSummary> listEffectiveDocuments(int limit);

    PagedResult<LawDocumentArticleItem> listDocumentArticles(UUID documentId, int page, int pageSize);

    List<LawArticleSearchResult> searchEffectiveArticles(String query, int limit);

    Optional<LawArticleDetail> findArticleDetail(UUID articleId);
}
