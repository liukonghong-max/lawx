package com.law4x.rag.infrastructure.agent;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Mono;

public class StructuredOutputMiddleware implements MiddlewareBase {

    private static final String STRUCTURED_OUTPUT_APPENDIX = """

            输出要求：
            1. 必须返回单个 JSON object，不能输出 Markdown、代码块标记或额外说明。
            2. 顶层字段固定为：
               - answer：完整回答文本，必须是 string
               - answerSegments：回答片段数组，必须是 array
            3. answerSegments 的每个元素固定包含：
               - id：片段唯一标识，string
               - text：片段正文，string
               - citationIds：引用的 articleId 数组，必须是 string array
            4. 如果某句话没有直接法条依据，citationIds 返回空数组。
            5. 不允许缺少字段，不允许输出 null。
            """;

    @Override
    public Mono<String> onSystemPrompt(Agent agent, RuntimeContext runtimeContext, String systemPrompt) {
        return Mono.just(systemPrompt + STRUCTURED_OUTPUT_APPENDIX);
    }
}
