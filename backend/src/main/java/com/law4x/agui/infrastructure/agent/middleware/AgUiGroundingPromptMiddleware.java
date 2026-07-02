package com.law4x.agui.infrastructure.agent.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Mono;

public class AgUiGroundingPromptMiddleware implements MiddlewareBase {

    public static final String GROUNDING_PROMPT_KEY = "law4xGroundingPrompt";

    @Override
    public Mono<String> onSystemPrompt(Agent agent, RuntimeContext runtimeContext, String systemPrompt) {
        if (runtimeContext == null) {
            return Mono.just(systemPrompt);
        }
        String groundingPrompt = runtimeContext.get(GROUNDING_PROMPT_KEY);
        if (groundingPrompt == null || groundingPrompt.isBlank()) {
            return Mono.just(systemPrompt);
        }
        return Mono.just(systemPrompt + "\n\n" + groundingPrompt.trim());
    }
}
