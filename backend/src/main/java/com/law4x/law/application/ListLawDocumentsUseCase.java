package com.law4x.law.application;

import com.law4x.law.domain.model.LawDocumentSummary;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListLawDocumentsUseCase {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final LawArticleRepository lawArticleRepository;

    public ListLawDocumentsUseCase(LawArticleRepository lawArticleRepository) {
        this.lawArticleRepository = lawArticleRepository;
    }

    public List<LawDocumentSummary> list(Integer limit) {
        return lawArticleRepository.listEffectiveDocuments(normalizeLimit(limit));
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
