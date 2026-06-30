package com.law4x.rag.application;

import com.law4x.rag.domain.model.EmbeddableLawArticle;
import com.law4x.rag.domain.repository.EmbeddingClient;
import com.law4x.rag.domain.repository.LawArticleEmbeddingRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GenerateMissingArticleEmbeddingsUseCase {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    private final LawArticleEmbeddingRepository lawArticleEmbeddingRepository;
    private final EmbeddingClient embeddingClient;

    public GenerateMissingArticleEmbeddingsUseCase(
            LawArticleEmbeddingRepository lawArticleEmbeddingRepository,
            EmbeddingClient embeddingClient
    ) {
        this.lawArticleEmbeddingRepository = lawArticleEmbeddingRepository;
        this.embeddingClient = embeddingClient;
    }

    public GenerateResult generate(String embeddingModel, Integer limit) {
        String normalizedModel = normalizeEmbeddingModel(embeddingModel);
        int normalizedLimit = normalizeLimit(limit);
        List<EmbeddableLawArticle> articles = lawArticleEmbeddingRepository.findArticlesMissingEmbeddings(
                normalizedModel,
                normalizedLimit
        );
        int generated = 0;
        for (EmbeddableLawArticle article : articles) {
            lawArticleEmbeddingRepository.upsertArticleEmbedding(
                    article.articleId(),
                    normalizedModel,
                    article.contentHash(),
                    embeddingClient.embed(article.embeddingText(), normalizedModel)
            );
            generated++;
        }
        return new GenerateResult(articles.size(), generated);
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

    public record GenerateResult(int scanned, int generated) {
    }
}
