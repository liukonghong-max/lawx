package com.law4x.rag.infrastructure.answer;

import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.RagAnswerClient;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.Model;
import java.util.List;

public class AgentScopeRagAnswerClient implements RagAnswerClient {

    private static final String SYSTEM_PROMPT = """
            你是 law4x 法律法规 RAG 助手。
            必须只基于给定法条依据回答，不要编造未提供的事实或法律依据。
            不确定就说明无法仅凭现有法条判断，并提示需要补充事实或证据。
            回答必须引用法条名称和条号，不要承诺诉讼结果、胜诉概率或最终裁判结论。
            语言应简洁、稳健，避免替代律师给出个案最终意见。
            """;

    private final Model model;

    public AgentScopeRagAnswerClient(Model model) {
        this.model = model;
    }

    @Override
    public String answer(String question, List<RagSearchResult> evidence) {
        String prompt = buildUserPrompt(question, evidence);
        ChatResponse response = model.stream(List.of(
                        Msg.builder()
                                .role(MsgRole.SYSTEM)
                                .textContent(SYSTEM_PROMPT)
                                .build(),
                        Msg.builder()
                                .role(MsgRole.USER)
                                .textContent(prompt)
                                .build()
                ), List.of(), null)
                .blockLast();
        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            throw new IllegalStateException("answer model returned empty response");
        }
        String answer = response.getContent()
                .stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .collect(java.util.stream.Collectors.joining("\n"))
                .trim();
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
}
