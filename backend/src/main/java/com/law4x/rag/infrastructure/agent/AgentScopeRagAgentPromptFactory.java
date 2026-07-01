package com.law4x.rag.infrastructure.agent;

import com.law4x.rag.domain.model.RagSearchResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AgentScopeRagAgentPromptFactory {

    private static final String SYSTEM_PROMPT = """
            你是 law4x 法律法规 RAG 助手。
            请仅基于提供的法条依据回答。
            如现有依据不足以支持明确判断，请直接说明依据不足，并提示用户补充事实或证据。
            回答中应点明相关法条名称和条号。
            请避免承诺诉讼结果、胜诉概率或最终裁判结论，也不要替代律师给出个案最终意见。
            对于基于法条依据形成的结论，请明确标注对应依据；对于仅起提示作用、无法条直接支持的内容，可以不标注依据。
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(String question, List<RagSearchResult> evidence) {
        return """
                用户问题：
                %s

                法条依据（编号从1开始）：
                %s

                请严格按照系统指定的 structured output schema 返回内容。
                answer 需要是完整回答文本。
                answerSegments 需要按句子或自然段拆分。
                如果某句话只是提示补充事实、说明信息不足，且没有法条直接支持，请将 citationIds 设为空数组。
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
                    .append("articleId：")
                    .append(result.articleId())
                    .append("\n")
                    .append("路径：")
                    .append(result.fullPath())
                    .append("\n")
                    .append("摘录：")
                    .append(result.preview())
                    .append("\n")
                    .append("命中方式：")
                    .append(result.matchType())
                    .append("\n")
                    .append("keywordScore：")
                    .append(result.keywordScore())
                    .append("\n")
                    .append("vectorScore：")
                    .append(result.vectorScore())
                    .append("\n")
                    .append("finalScore：")
                    .append(result.finalScore())
                    .append("\n")
                    .append("命中原因：")
                    .append(result.reason())
                    .append("\n\n");
        }
        return builder.toString();
    }
}
