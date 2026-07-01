package com.law4x.rag.infrastructure.answer;

import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.RagAnswerClient;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class OpenAiCompatibleRagAnswerClient implements RagAnswerClient {

    private static final String SYSTEM_PROMPT = """
            你是 law4x 法律法规 RAG 助手。
            必须只基于给定法条依据回答，不要编造未提供的事实或法律依据。
            不确定就说明无法仅凭现有法条判断，并提示需要补充事实或证据。
            回答必须引用法条名称和条号，不要承诺诉讼结果、胜诉概率或最终裁判结论。
            语言应简洁、稳健，避免替代律师给出个案最终意见。
            """;

    private final RestTemplate restTemplate;
    private final String chatCompletionsUrl;
    private final String apiKey;
    private final String modelName;

    public OpenAiCompatibleRagAnswerClient(
            RestTemplate restTemplate,
            String baseUrl,
            String apiKey,
            String modelName
    ) {
        this.restTemplate = restTemplate;
        this.chatCompletionsUrl = normalizeBaseUrl(baseUrl) + "/chat/completions";
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @Override
    public String answer(String question, List<RagSearchResult> evidence) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "model", modelName,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", buildUserPrompt(question, evidence))
                )
        );

        ChatCompletionResponse response = restTemplate.postForObject(
                chatCompletionsUrl,
                new HttpEntity<>(body, headers),
                ChatCompletionResponse.class
        );
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("answer model returned empty response");
        }
        String answer = response.choices().get(0).message().content().trim();
        if (answer.isBlank()) {
            throw new IllegalStateException("answer model returned empty text");
        }
        return answer;
    }

    private static String buildUserPrompt(String question, List<RagSearchResult> evidence) {
        return """
                用户问题：
                %s

                法条依据：
                %s

                请基于上述法条依据回答用户问题，并在回答中点名引用相关法条。
                """.formatted(question, formatEvidence(evidence));
    }

    private static String formatEvidence(List<RagSearchResult> evidence) {
        if (evidence.isEmpty()) {
            return "未检索到相关法条。";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < evidence.size(); i++) {
            RagSearchResult result = evidence.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(result.documentTitle())
                    .append(" ")
                    .append(result.articleNo())
                    .append("\n")
                    .append("路径：")
                    .append(result.fullPath())
                    .append("\n")
                    .append("摘录：")
                    .append(result.preview())
                    .append("\n")
                    .append("命中：")
                    .append(result.matchType())
                    .append("，finalScore=")
                    .append(result.finalScore())
                    .append("\n\n");
        }
        return builder.toString();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
