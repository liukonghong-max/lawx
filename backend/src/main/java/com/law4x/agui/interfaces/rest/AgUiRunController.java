package com.law4x.agui.interfaces.rest;

import com.law4x.agui.application.service.AgUiConsultationGroundingService;
import com.law4x.agui.application.service.AgUiConversationStateService;
import com.law4x.agui.infrastructure.agent.middleware.AgUiGroundingPromptMiddleware;
import com.law4x.agui.infrastructure.agent.runtime.AgUiRuntimeContextHolder;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.agent.RuntimeContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ag-ui/runs")
public class AgUiRunController {

    private final AguiAgentAdapter aguiAgentAdapter;
    private final AgUiRuntimeContextHolder agUiRuntimeContextHolder;
    private final AgUiConsultationGroundingService agUiConsultationGroundingService;
    private final AgUiConversationStateService agUiConversationStateService;

    public AgUiRunController(
            AguiAgentAdapter aguiAgentAdapter,
            AgUiRuntimeContextHolder agUiRuntimeContextHolder,
            AgUiConsultationGroundingService agUiConsultationGroundingService,
            AgUiConversationStateService agUiConversationStateService
    ) {
        this.aguiAgentAdapter = aguiAgentAdapter;
        this.agUiRuntimeContextHolder = agUiRuntimeContextHolder;
        this.agUiConsultationGroundingService = agUiConsultationGroundingService;
        this.agUiConversationStateService = agUiConversationStateService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AguiEvent> createRun(@RequestBody AgUiRunRequest request) {
        AtomicReference<StringBuilder> answerBuffer = new AtomicReference<>(new StringBuilder());
        String latestUserQuery = latestUserQuery(request.messages());
        AgUiConsultationGroundingService.PreparedGrounding grounding =
                agUiConsultationGroundingService.prepare(latestUserQuery, request.state(), request.runId());
        RunAgentInput input = toRunAgentInput(request, grounding.state());
        Map<String, Object> currentState = grounding.state();
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .sessionId(request.threadId())
                .userId("ag-ui")
                .put(AgUiGroundingPromptMiddleware.GROUNDING_PROMPT_KEY, grounding.groundingPrompt())
                .build();

        return Flux.defer(() -> {
                    agUiRuntimeContextHolder.set(runtimeContext);
                    return aguiAgentAdapter.run(input);
                })
                .concatMap(event -> {
                    if (event instanceof AguiEvent.TextMessageContent textMessageContent) {
                        answerBuffer.get().append(textMessageContent.delta());
                        return Flux.just(event);
                    }
                    if (event instanceof AguiEvent.RunFinished runFinished) {
                        Map<String, Object> finalState = agUiConversationStateService.buildFinalState(
                                latestUserQuery,
                                answerBuffer.get().toString(),
                                currentState
                        );
                        return Flux.just(
                                new AguiEvent.StateSnapshot(runFinished.threadId(), runFinished.runId(), finalState),
                                event
                        );
                    }
                    return Flux.just(event);
                })
                .doFinally(signalType -> agUiRuntimeContextHolder.clear());
    }

    private RunAgentInput toRunAgentInput(AgUiRunRequest request, Map<String, Object> state) {
        List<AguiMessage> messages = request.messages() == null
                ? List.of()
                : request.messages().stream()
                .map(message -> switch (normalizeRole(message.role())) {
                    case "assistant" -> AguiMessage.assistantMessage(message.id(), defaultText(message.content()));
                    case "system" -> AguiMessage.systemMessage(message.id(), defaultText(message.content()));
                    default -> AguiMessage.userMessage(message.id(), defaultText(message.content()));
                })
                .toList();
        return RunAgentInput.builder()
                .threadId(request.threadId())
                .runId(request.runId())
                .messages(messages)
                .tools(List.of())
                .context(List.of())
                .state(state == null ? Map.of() : state)
                .forwardedProps(request.forwardedProps() == null ? Map.of() : request.forwardedProps())
                .build();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        return role.trim().toLowerCase();
    }

    private String defaultText(String text) {
        return text == null ? "" : text;
    }

    private String latestUserQuery(List<AgUiMessageRequest> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            AgUiMessageRequest message = messages.get(index);
            if ("user".equals(normalizeRole(message.role()))) {
                return defaultText(message.content()).trim();
            }
        }
        return "";
    }

    public record AgUiRunRequest(
            String threadId,
            String runId,
            List<AgUiMessageRequest> messages,
            Map<String, Object> state,
            Map<String, Object> forwardedProps
    ) {
    }

    public record AgUiMessageRequest(
            String id,
            String role,
            String content
    ) {
    }
}
