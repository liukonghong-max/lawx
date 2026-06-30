package com.law4x.law.parser;

public record LawArticle(
        String articleNo,
        int articleOrder,
        String content,
        String bookTitle,
        String chapterTitle,
        String sectionTitle,
        String fullPath,
        String contentHash
) {
}
