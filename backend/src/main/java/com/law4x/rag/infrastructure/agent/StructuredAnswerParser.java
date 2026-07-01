package com.law4x.rag.infrastructure.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.law4x.rag.domain.model.RagAnswer;
import com.law4x.rag.domain.repository.RagAnswerClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class StructuredAnswerParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagAnswerClient.RagAnswerPayload parse(String rawAnswer) {
        if (rawAnswer == null) {
            throw new IllegalStateException("answer model returned null text");
        }
        String normalized = rawAnswer.trim();
        if (normalized.isBlank()) {
            throw new IllegalStateException("answer model returned empty text");
        }
        try {
            StructuredAnswer payload = objectMapper.readValue(normalized, StructuredAnswer.class);
            List<RagAnswer.AnswerSegment> segments = payload.answerSegments() == null ? List.of()
                : payload.answerSegments()
                    .stream()
                    .map(item -> new RagAnswer.AnswerSegment(
                        item.id(), item.text(), item.citationIds() == null ? List.of() : item.citationIds()
                        .stream()
                        .map(UUID::fromString)
                        .toList()
                    ))
                    .toList();
            String answer = payload.answer() == null ? "" : payload.answer()
                .trim();
            if (answer.isBlank()) {
                throw new IllegalStateException("answer model returned blank answer field");
            }
            return new RagAnswerClient.RagAnswerPayload(answer, segments);
        } catch (Exception exception) {
            throw new IllegalStateException("answer model returned invalid structured payload", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StructuredAnswer(String answer, List<StructuredSegment> answerSegments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StructuredSegment(String id, String text, List<String> citationIds) {
    }
}
