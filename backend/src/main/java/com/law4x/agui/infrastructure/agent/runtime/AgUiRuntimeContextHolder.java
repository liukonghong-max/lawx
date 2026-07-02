package com.law4x.agui.infrastructure.agent.runtime;

import io.agentscope.core.agent.RuntimeContext;
import org.springframework.stereotype.Component;

@Component
public class AgUiRuntimeContextHolder {

    private final ThreadLocal<RuntimeContext> current = new ThreadLocal<>();

    public void set(RuntimeContext runtimeContext) {
        current.set(runtimeContext);
    }

    public RuntimeContext consume() {
        RuntimeContext runtimeContext = current.get();
        current.remove();
        return runtimeContext;
    }

    public void clear() {
        current.remove();
    }
}
