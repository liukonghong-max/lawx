package com.law4x.agui.infrastructure.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GroundedHarnessAgent implements Agent {

    private final HarnessAgent delegate;
    private final AgUiRuntimeContextHolder runtimeContextHolder;

    public GroundedHarnessAgent(HarnessAgent delegate, AgUiRuntimeContextHolder runtimeContextHolder) {
        this.delegate = delegate;
        this.runtimeContextHolder = runtimeContextHolder;
    }

    @Override
    public String getAgentId() {
        return delegate.getAgentId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void interrupt() {
        delegate.interrupt();
    }

    @Override
    public void interrupt(Msg msg) {
        delegate.interrupt(msg);
    }

    @Override
    public Mono<Msg> call(List<Msg> messages) {
        return delegate.call(messages, consumeRuntimeContext());
    }

    @Override
    public Mono<Msg> call(List<Msg> messages, Class<?> structuredOutputClass) {
        return delegate.call(messages, structuredOutputClass, consumeRuntimeContext());
    }

    @Override
    public Mono<Msg> call(List<Msg> messages, JsonNode structuredOutputSchema) {
        return delegate.call(messages, structuredOutputSchema, consumeRuntimeContext());
    }

    @Override
    public Flux<Event> stream(List<Msg> messages, StreamOptions streamOptions) {
        return delegate.stream(messages, streamOptions, consumeRuntimeContext());
    }

    @Override
    public Flux<Event> stream(List<Msg> messages, StreamOptions streamOptions, Class<?> structuredOutputClass) {
        return delegate.stream(messages, streamOptions, structuredOutputClass, consumeRuntimeContext());
    }

    @Override
    public Flux<Event> stream(List<Msg> messages, StreamOptions streamOptions, JsonNode structuredOutputSchema) {
        return delegate.stream(messages, streamOptions, structuredOutputSchema, consumeRuntimeContext());
    }

    @Override
    public Mono<Void> observe(Msg msg) {
        return delegate.observe(msg);
    }

    @Override
    public Mono<Void> observe(List<Msg> messages) {
        return delegate.observe(messages);
    }

    @Override
    public AgentState getAgentState() {
        return delegate.getAgentState();
    }

    @Override
    public Toolkit getToolkit() {
        return delegate.getToolkit();
    }

    private RuntimeContext consumeRuntimeContext() {
        RuntimeContext runtimeContext = runtimeContextHolder.consume();
        return runtimeContext == null ? RuntimeContext.empty() : runtimeContext;
    }
}
