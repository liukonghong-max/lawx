package com.law4x.rag.application;

import com.law4x.rag.domain.model.RagAnswer;
import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.RagAnswerClient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class CreateRagAnswerUseCase {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final Pattern SEGMENT_SPLIT_PATTERN = Pattern.compile("(?<=[。？！；\n])");
    private static final Pattern CITATION_MARK_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    private final CreateRagTestRunUseCase createRagTestRunUseCase;
    private final RagAnswerClient ragAnswerClient;

    public CreateRagAnswerUseCase(
            CreateRagTestRunUseCase createRagTestRunUseCase,
            RagAnswerClient ragAnswerClient
    ) {
        this.createRagTestRunUseCase = createRagTestRunUseCase;
        this.ragAnswerClient = ragAnswerClient;
    }

    public RagAnswer answer(String query, String embeddingModel, Integer limit) {
        String normalizedQuery = normalizeQuery(query);
        int normalizedLimit = normalizeLimit(limit);
        CreateRagTestRunUseCase.CreateResult testRun = createRagTestRunUseCase.create(
                normalizedQuery,
                embeddingModel,
                normalizedLimit
        );
        List<RagSearchResult> evidence = testRun.results();
        RagAnswerClient.RagAnswerPayload answerPayload = ragAnswerClient.answer(normalizedQuery, evidence);
        List<RagAnswer.Citation> citations = evidence.stream().map(CreateRagAnswerUseCase::toCitation).toList();
        String answerContent = answerPayload.answer();
        List<RagAnswer.AnswerSegment> segments = answerPayload.answerSegments().isEmpty()
                ? parseAnswerSegments(answerContent, citations)
                : answerPayload.answerSegments();

        return new RagAnswer(
                testRun.runId(),
                answerContent,
                citations,
                segments
        );
    }

    private List<RagAnswer.AnswerSegment> parseAnswerSegments(String answerContent, List<RagAnswer.Citation> citations) {
        List<RagAnswer.AnswerSegment> segments = new ArrayList<>();
        if (answerContent == null || answerContent.isBlank()) {
            return segments;
        }

        // 按句子拆分回答
        String[] parts = SEGMENT_SPLIT_PATTERN.split(answerContent);
        for (int i = 0; i < parts.length; i++) {
            String originalText = parts[i].trim();
            if (originalText.isBlank()) {
                continue;
            }
            // 提取句子里的所有引用标记
            Matcher matcher = CITATION_MARK_PATTERN.matcher(originalText);
            List<UUID> citationIds = new ArrayList<>();
            while (matcher.find()) {
                try {
                    int citeIndex = Integer.parseInt(matcher.group(1)) - 1;
                    if (citeIndex >= 0 && citeIndex < citations.size()) {
                        citationIds.add(citations.get(citeIndex).articleId());
                    }
                } catch (NumberFormatException ignored) {
                    }
            }
            // 移除句子里的引用标记，返回纯文本
            String cleanText = CITATION_MARK_PATTERN.matcher(originalText).replaceAll("").trim();
            if (!cleanText.isBlank()) {
                segments.add(new RagAnswer.AnswerSegment(
                    "seg_" + UUID.randomUUID(),
                    cleanText,
                    citationIds.isEmpty() ? List.of() : citationIds
                ));
            }
        }
        return segments;
    }

    private static RagAnswer.Citation toCitation(RagSearchResult result) {
        return new RagAnswer.Citation(
                result.articleId(),
                result.documentTitle(),
                result.articleNo(),
                result.fullPath(),
                result.preview()
        );
    }

    private static String normalizeQuery(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return query.trim();
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
