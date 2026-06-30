package com.law4x.law.application;

import com.law4x.law.domain.model.LawArticleDetail;
import com.law4x.law.domain.repository.LawArticleRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetLawArticleDetailUseCase {

    private final LawArticleRepository lawArticleRepository;

    public GetLawArticleDetailUseCase(LawArticleRepository lawArticleRepository) {
        this.lawArticleRepository = lawArticleRepository;
    }

    public Optional<LawArticleDetail> get(String articleId) {
        return lawArticleRepository.findArticleDetail(parseArticleId(articleId));
    }

    private static UUID parseArticleId(String articleId) {
        try {
            return UUID.fromString(articleId);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("articleId must be a valid UUID");
        }
    }
}
