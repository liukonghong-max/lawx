package com.law4x.agui.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.law4x.agui.application.service.AgUiConsultationGroundingService;
import com.law4x.agui.application.service.AgUiConversationStateService;
import com.law4x.agui.infrastructure.agent.runtime.AgUiRuntimeContextHolder;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebMvcTest(AgUiRunController.class)
@Import(AgUiRunControllerTest.TestConfig.class)
class AgUiRunControllerTest {

    private static final AtomicReference<List<Msg>> LAST_MESSAGES = new AtomicReference<>(List.of());

    @Autowired
    private MockMvc mockMvc;

    @Test
    void streamsAgUiRunEvents() throws Exception {
        MvcResult result = mockMvc.perform(post("/ag-ui/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "threadId": "thread-001",
                                  "runId": "run-001",
                                  "messages": [
                                    {
                                      "id": "msg-user-1",
                                      "role": "user",
                                      "content": "别人欠钱不还怎么办"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(responseBody)
                .contains("data:")
                .contains("RUN_STARTED")
                .contains("TEXT_MESSAGE_START")
                .contains("TEXT_MESSAGE_END")
                .contains("STATE_SNAPSHOT")
                .contains("\"citations\":[{\"articleId\":\"article-001\"}]")
                .contains("RUN_FINISHED");
        org.assertj.core.api.Assertions.assertThat(LAST_MESSAGES.get())
                .hasSize(1)
                .satisfies(messages -> {
                    Msg user = messages.get(0);
                    org.assertj.core.api.Assertions.assertThat(user.getRole()).isEqualTo(MsgRole.USER);
                });
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        AguiAgentAdapter aguiAgentAdapter() {
            Agent testAgent = new Agent() {
                @Override
                public String getAgentId() {
                    return "test-agent";
                }

                @Override
                public String getName() {
                    return "test-agent";
                }

                @Override
                public void interrupt() {
                }

                @Override
                public void interrupt(Msg msg) {
                }

                @Override
                public Mono<Msg> call(List<Msg> messages) {
                    return Mono.just(new UserMessage("ignored"));
                }

                @Override
                public Mono<Msg> call(List<Msg> messages, Class<?> structuredOutputClass) {
                    return Mono.just(new UserMessage("ignored"));
                }

                @Override
                public Mono<Msg> call(List<Msg> messages, JsonNode structuredOutputSchema) {
                    return Mono.just(new UserMessage("ignored"));
                }

                @Override
                public Flux<Event> stream(List<Msg> messages, StreamOptions streamOptions) {
                    LAST_MESSAGES.set(List.copyOf(messages));
                    Msg reply = Msg.builder()
                            .name("test-agent")
                            .role(io.agentscope.core.message.MsgRole.ASSISTANT)
                            .content(List.of(TextBlock.builder().text("hello").build()))
                            .build();
                    return Flux.just(new Event(EventType.SUMMARY, reply, true));
                }

                @Override
                public Flux<Event> stream(List<Msg> messages, StreamOptions streamOptions, Class<?> structuredOutputClass) {
                    return stream(messages, streamOptions);
                }

                @Override
                public Flux<Event> stream(List<Msg> messages, StreamOptions streamOptions, JsonNode structuredOutputSchema) {
                    return stream(messages, streamOptions);
                }

                @Override
                public Mono<Void> observe(List<Msg> messages) {
                    return Mono.empty();
                }

                @Override
                public Mono<Void> observe(Msg msg) {
                    return Mono.empty();
                }
            };
            return new AguiAgentAdapter(
                    testAgent,
                    AguiAdapterConfig.defaultConfig()
            );
        }

        @Bean
        AgUiRuntimeContextHolder agUiRuntimeContextHolder() {
            return new AgUiRuntimeContextHolder();
        }

        @Bean
        AgUiConsultationGroundingService agUiConsultationGroundingService() {
            return new AgUiConsultationGroundingService(null, null) {
                @Override
                public PreparedGrounding prepare(String query, Map<String, Object> currentState, String runId) {
                    return new PreparedGrounding(
                            Map.of(
                                    "allowedCitationIds", List.of("article-001"),
                                    "citations", List.of(Map.of("articleId", "article-001"))
                            ),
                            "系统已在服务端完成本轮法规检索。allowedCitationIds: [article-001]"
                    );
                }
            };
        }

        @Bean
        AgUiConversationStateService agUiConversationStateService() {
            return new AgUiConversationStateService(null, null) {
                @Override
                public Map<String, Object> buildFinalState(String query, String answer, Map<String, Object> currentState) {
                    return Map.ofEntries(
                            Map.entry("answer", answer),
                            Map.entry("citations", currentState.get("citations")),
                            Map.entry("allowedCitationIds", currentState.get("allowedCitationIds")),
                            Map.entry("answerSegments", List.of(Map.of("id", "seg-1", "text", answer, "citationIds", List.of())))
                    );
                }
            };
        }
    }
}
