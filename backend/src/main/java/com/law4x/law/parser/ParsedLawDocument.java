package com.law4x.law.parser;

import java.util.List;

public record ParsedLawDocument(
        String title,
        String lawType,
        String publishDate,
        String sourceFileName,
        List<LawArticle> articles
) {
    public LawArticle articleByNo(String articleNo) {
        return articles.stream()
                .filter(article -> article.articleNo().equals(articleNo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleNo));
    }
}
