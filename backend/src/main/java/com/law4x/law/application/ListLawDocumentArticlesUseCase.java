package com.law4x.law.application;

import com.law4x.law.domain.model.LawDocumentArticleItem;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListLawDocumentArticlesUseCase {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final LawArticleRepository lawArticleRepository;

    public ListLawDocumentArticlesUseCase(LawArticleRepository lawArticleRepository) {
        this.lawArticleRepository = lawArticleRepository;
    }

    public LawArticleRepository.PagedResult<LawDocumentArticleItem> list(String documentId, Integer page, Integer pageSize) {
        return lawArticleRepository.listDocumentArticles(
                parseDocumentId(documentId),
                normalizePage(page),
                normalizePageSize(pageSize)
        );
    }

    private static UUID parseDocumentId(String documentId) {
        try {
            return UUID.fromString(documentId);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("documentId must be a valid UUID");
        }
    }

    private static int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
