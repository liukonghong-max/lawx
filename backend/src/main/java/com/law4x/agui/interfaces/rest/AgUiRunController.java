package com.law4x.agui.interfaces.rest;

import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.RunAgentInput;
import java.util.List;
import java.util.Map;
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

    public AgUiRunController(AguiAgentAdapter aguiAgentAdapter) {
        this.aguiAgentAdapter = aguiAgentAdapter;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AguiEvent> createRun(@RequestBody AgUiRunRequest request) {
        return aguiAgentAdapter.run(toRunAgentInput(request));
    }

    private RunAgentInput toRunAgentInput(AgUiRunRequest request) {
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
                .state(request.state() == null ? Map.of() : request.state())
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
