package com.law4x.law.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class LawDocxParser {

    private static final Pattern ARTICLE_PATTERN = Pattern.compile("^(第[一二三四五六七八九十百千万零〇两]+条)\\s*(.*)$");
    private static final Pattern BOOK_PATTERN = Pattern.compile("^第[一二三四五六七八九十百千万零〇两]+编\\s+.*$");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^第[一二三四五六七八九十百千万零〇两]+章\\s+.*$");
    private static final Pattern SECTION_PATTERN = Pattern.compile("^第[一二三四五六七八九十百千万零〇两]+节\\s+.*$");
    private static final Pattern PASS_DATE_PATTERN = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日");

    public ParsedLawDocument parse(Path path) throws IOException {
        return parse(path, "法律");
    }

    public ParsedLawDocument parse(Path path, String lawType) throws IOException {
        List<String> paragraphs = readParagraphs(path);
        if (paragraphs.size() < 3) {
            throw new IllegalArgumentException("document has too few paragraphs: " + path);
        }

        String title = paragraphs.get(0);
        String publishDate = extractPublishDate(paragraphs.get(1));
        int bodyStart = findBodyStart(paragraphs);

        String bookTitle = null;
        String chapterTitle = null;
        String sectionTitle = null;
        List<ArticleDraft> drafts = new ArrayList<>();
        ArticleDraft current = null;

        for (int index = bodyStart; index < paragraphs.size(); index++) {
            String text = paragraphs.get(index);

            if (BOOK_PATTERN.matcher(text).matches()) {
                bookTitle = normalizeHeading(text);
                chapterTitle = null;
                sectionTitle = null;
                continue;
            }
            if (CHAPTER_PATTERN.matcher(text).matches()) {
                chapterTitle = normalizeHeading(text);
                sectionTitle = null;
                continue;
            }
            if (SECTION_PATTERN.matcher(text).matches()) {
                sectionTitle = normalizeHeading(text);
                continue;
            }

            Matcher articleMatcher = ARTICLE_PATTERN.matcher(text);
            if (articleMatcher.matches()) {
                current = new ArticleDraft(
                        articleMatcher.group(1),
                        drafts.size() + 1,
                        bookTitle,
                        chapterTitle,
                        sectionTitle
                );
                current.append(articleMatcher.group(2).trim());
                drafts.add(current);
                continue;
            }

            if (current != null) {
                current.append(text);
            }
        }

        List<LawArticle> articles = drafts.stream()
                .map(draft -> draft.build(title))
                .toList();

        return new ParsedLawDocument(
                title,
                lawType,
                publishDate,
                path.getFileName().toString(),
                articles
        );
    }

    private static List<String> readParagraphs(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                    .map(paragraph -> normalizeText(paragraph.getText()))
                    .filter(text -> !text.isBlank())
                    .toList();
        }
    }

    private static String normalizeText(String text) {
        return text.replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeHeading(String text) {
        String[] parts = text.split(" ", 2);
        if (parts.length == 1) {
            return text;
        }
        return parts[0] + " " + parts[1].replace(" ", "");
    }

    private static String extractPublishDate(String text) {
        Matcher matcher = PASS_DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return "%04d-%02d-%02d".formatted(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }

    private static int findBodyStart(List<String> paragraphs) {
        for (int index = 0; index < paragraphs.size(); index++) {
            if (ARTICLE_PATTERN.matcher(paragraphs.get(index)).matches()) {
                int start = index;
                while (start > 0 && isHierarchyHeading(paragraphs.get(start - 1))) {
                    start--;
                }
                return start;
            }
        }
        throw new IllegalArgumentException("no article paragraphs found");
    }

    private static boolean isHierarchyHeading(String text) {
        return BOOK_PATTERN.matcher(text).matches()
                || CHAPTER_PATTERN.matcher(text).matches()
                || SECTION_PATTERN.matcher(text).matches();
    }

    private record ArticleDraft(
            String articleNo,
            int articleOrder,
            String bookTitle,
            String chapterTitle,
            String sectionTitle,
            List<String> parts
    ) {
        ArticleDraft(String articleNo, int articleOrder, String bookTitle, String chapterTitle, String sectionTitle) {
            this(articleNo, articleOrder, bookTitle, chapterTitle, sectionTitle, new ArrayList<>());
        }

        void append(String text) {
            if (text != null && !text.isBlank()) {
                parts.add(text);
            }
        }

        LawArticle build(String documentTitle) {
            String content = String.join("\n", parts).trim();
            List<String> pathParts = new ArrayList<>();
            pathParts.add(documentTitle);
            addIfPresent(pathParts, bookTitle);
            addIfPresent(pathParts, chapterTitle);
            addIfPresent(pathParts, sectionTitle);
            pathParts.add(articleNo);

            return new LawArticle(
                    articleNo,
                    articleOrder,
                    content,
                    bookTitle,
                    chapterTitle,
                    sectionTitle,
                    String.join(" > ", pathParts),
                    sha256(content)
            );
        }

        private static void addIfPresent(List<String> values, String value) {
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
