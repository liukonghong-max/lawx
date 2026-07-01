package com.law4x.rag.infrastructure.answer;

import com.law4x.rag.domain.model.RagSearchResult;
import com.law4x.rag.domain.repository.RagAnswerClient;
import com.law4x.rag.infrastructure.agent.AgentScopeRagAgentPromptFactory;
import com.law4x.rag.infrastructure.agent.Law4xAgentScopeProperties;
import com.law4x.rag.infrastructure.agent.StructuredAnswerParser;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.state.AgentStateStore;
import java.util.List;
import java.util.UUID;

public class AgentScopeRagAnswerClient implements RagAnswerClient {

    private final ReActAgent agent;
    private final Law4xAgentScopeProperties properties;
    private final AgentScopeRagAgentPromptFactory promptFactory;
    private final StructuredAnswerParser structuredAnswerParser;

    public AgentScopeRagAnswerClient(
            ReActAgent agent,
            Law4xAgentScopeProperties properties,
            AgentScopeRagAgentPromptFactory promptFactory,
            StructuredAnswerParser structuredAnswerParser
    ) {
        this.agent = agent;
        this.properties = properties;
        this.promptFactory = promptFactory;
        this.structuredAnswerParser = structuredAnswerParser;
    }

    @Override
    public RagAnswerPayload answer(String question, List<RagSearchResult> evidence) {
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId(properties.getDefaultUserId())
                .sessionId(buildSessionId())
                .build();
        Msg response = agent.call(
                        List.of(new UserMessage(promptFactory.buildUserPrompt(question, evidence))),
                        runtimeContext
                )
                .block();
        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            throw new IllegalStateException("answer model returned empty response");
        }
        String answer = response.getContent().stream()
                .filter(TextBlock.class::isInstance)
                .map(TextBlock.class::cast)
                .map(TextBlock::getText)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("")
                .trim();
        return structuredAnswerParser.parse(answer);
    }

    AgentStateStore stateStore() {
        return agent.getStateStore();
    }

    private String buildSessionId() {
        if (properties.isStateful()) {
            return properties.getDefaultSessionId();
        }
        return properties.getDefaultSessionId() + "-" + UUID.randomUUID();
    }
}
