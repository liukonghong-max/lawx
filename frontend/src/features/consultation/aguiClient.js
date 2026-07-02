function createId() {
    if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
        return crypto.randomUUID();
    }
    return `agui-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

let sharedThreadId = createId();

function createEmptyRuntime() {
    return {
        messages: [],
        state: {},
        toolCalls: []
    };
}

function createAssistantMessage(event) {
    return {
        id: event.messageId,
        role: event.role || "assistant",
        content: ""
    };
}

function ensureMessage(runtime, messageId, factory) {
    let message = runtime.messages.find((item) => item.id === messageId);
    if (!message) {
        message = factory();
        runtime.messages.push(message);
    }
    return message;
}

function parseEventFrames(chunkBuffer) {
    const frames = [];
    const parts = chunkBuffer.split(/\r?\n\r?\n/);
    const remainder = parts.pop() || "";

    for (const frame of parts) {
        const dataLines = frame
            .split(/\r?\n/)
            .filter((line) => line.startsWith("data:"))
            .map((line) => line.slice(5).trimStart());

        if (!dataLines.length) {
            continue;
        }

        frames.push(JSON.parse(dataLines.join("\n")));
    }

    return { frames, remainder };
}

function applyStateDelta(currentState, operations) {
    if (!Array.isArray(operations) || !operations.length) {
        return currentState;
    }

    const nextState = structuredClone(currentState);

    for (const operation of operations) {
        if (!operation || typeof operation.path !== "string") {
            continue;
        }

        const segments = operation.path.split("/").slice(1).map((segment) => segment.replace(/~1/g, "/").replace(/~0/g, "~"));
        if (!segments.length) {
            continue;
        }

        let target = nextState;
        for (let index = 0; index < segments.length - 1; index += 1) {
            const key = Array.isArray(target) ? Number(segments[index]) : segments[index];
            if (target[key] == null || typeof target[key] !== "object") {
                target[key] = {};
            }
            target = target[key];
        }

        const lastSegment = segments[segments.length - 1];
        const key = Array.isArray(target) ? Number(lastSegment) : lastSegment;

        if (operation.op === "remove") {
            if (Array.isArray(target)) {
                target.splice(key, 1);
            } else {
                delete target[key];
            }
            continue;
        }

        if (operation.op === "add" && Array.isArray(target) && lastSegment === "-") {
            target.push(operation.value);
            continue;
        }

        target[key] = operation.value;
    }

    return nextState;
}

async function notify(subscriber, handlerName, payload) {
    if (typeof subscriber?.[handlerName] === "function") {
        await subscriber[handlerName](payload);
    }
}

function findToolCall(runtime, toolCallId) {
    return runtime.toolCalls.find((item) => item.id === toolCallId);
}

function ensureToolCall(runtime, event) {
    let toolCall = findToolCall(runtime, event.toolCallId);
    if (!toolCall) {
        toolCall = {
            id: event.toolCallId,
            toolCallName: event.toolCallName || "",
            state: "input-streaming",
            input: {},
            inputBuffer: "",
            output: null,
            errorText: ""
        };
        runtime.toolCalls.push(toolCall);
    }

    if (event.toolCallName) {
        toolCall.toolCallName = event.toolCallName;
    }

    return toolCall;
}

function parseJsonSafely(value, fallback) {
    if (typeof value !== "string" || !value.trim()) {
        return fallback;
    }
    try {
        return JSON.parse(value);
    } catch {
        return fallback;
    }
}

async function dispatchEvent(runtime, input, subscriber, event) {
    const payloadBase = {
        event,
        messages: runtime.messages,
        state: runtime.state,
        input
    };

    await notify(subscriber, "onEvent", payloadBase);

    switch (event.type) {
        case "RUN_STARTED": {
            const incomingMessages = Array.isArray(event.input?.messages) ? event.input.messages : input.messages;
            for (const message of incomingMessages) {
                if (!runtime.messages.some((item) => item.id === message.id)) {
                    runtime.messages.push({ ...message });
                }
            }
            await notify(subscriber, "onRunStartedEvent", payloadBase);
            break;
        }
        case "TEXT_MESSAGE_START": {
            ensureMessage(runtime, event.messageId, () => createAssistantMessage(event));
            await notify(subscriber, "onTextMessageStartEvent", payloadBase);
            break;
        }
        case "TEXT_MESSAGE_CONTENT": {
            const message = ensureMessage(runtime, event.messageId, () => createAssistantMessage(event));
            message.content = `${typeof message.content === "string" ? message.content : ""}${event.delta || ""}`;
            await notify(subscriber, "onTextMessageContentEvent", {
                ...payloadBase,
                textMessageBuffer: message.content
            });
            break;
        }
        case "TEXT_MESSAGE_END": {
            const message = runtime.messages.find((item) => item.id === event.messageId);
            await notify(subscriber, "onTextMessageEndEvent", {
                ...payloadBase,
                textMessageBuffer: typeof message?.content === "string" ? message.content : ""
            });
            break;
        }
        case "REASONING_MESSAGE_START": {
            ensureMessage(runtime, event.messageId, () => ({
                id: event.messageId,
                role: "reasoning",
                content: ""
            }));
            await notify(subscriber, "onReasoningMessageStartEvent", payloadBase);
            break;
        }
        case "REASONING_MESSAGE_CONTENT": {
            const message = ensureMessage(runtime, event.messageId, () => ({
                id: event.messageId,
                role: "reasoning",
                content: ""
            }));
            message.content = `${typeof message.content === "string" ? message.content : ""}${event.delta || ""}`;
            await notify(subscriber, "onReasoningMessageContentEvent", {
                ...payloadBase,
                reasoningMessageBuffer: message.content
            });
            break;
        }
        case "REASONING_MESSAGE_END": {
            const message = runtime.messages.find((item) => item.id === event.messageId);
            await notify(subscriber, "onReasoningMessageEndEvent", {
                ...payloadBase,
                reasoningMessageBuffer: typeof message?.content === "string" ? message.content : ""
            });
            break;
        }
        case "TOOL_CALL_START": {
            const toolCall = ensureToolCall(runtime, event);
            toolCall.state = "input-streaming";
            await notify(subscriber, "onToolCallsChanged", {
                toolCalls: runtime.toolCalls
            });
            await notify(subscriber, "onToolCallStartEvent", {
                ...payloadBase,
                event: {
                    ...event,
                    toolCallName: toolCall.toolCallName
                }
            });
            break;
        }
        case "TOOL_CALL_ARGS": {
            const toolCall = ensureToolCall(runtime, event);
            toolCall.inputBuffer = `${toolCall.inputBuffer || ""}${event.delta || ""}`;
            toolCall.input = parseJsonSafely(toolCall.inputBuffer, toolCall.inputBuffer || {});
            await notify(subscriber, "onToolCallsChanged", {
                toolCalls: runtime.toolCalls
            });
            await notify(subscriber, "onToolCallArgsEvent", {
                ...payloadBase,
                event: {
                    ...event,
                    toolCallName: toolCall.toolCallName
                },
                toolCallBuffer: toolCall.inputBuffer,
                partialToolCallArgs: toolCall.input
            });
            break;
        }
        case "TOOL_CALL_END": {
            const toolCall = ensureToolCall(runtime, event);
            toolCall.state = "input-available";
            toolCall.input = parseJsonSafely(toolCall.inputBuffer, toolCall.input);
            await notify(subscriber, "onToolCallsChanged", {
                toolCalls: runtime.toolCalls
            });
            await notify(subscriber, "onToolCallEndEvent", {
                ...payloadBase,
                event: {
                    ...event,
                    toolCallName: toolCall.toolCallName
                },
                toolCallName: toolCall.toolCallName,
                toolCallArgs: toolCall.input
            });
            break;
        }
        case "TOOL_CALL_RESULT": {
            const toolCall = ensureToolCall(runtime, event);
            toolCall.state = "output-available";
            toolCall.output = parseJsonSafely(event.content, event.content || null);
            toolCall.errorText = "";
            await notify(subscriber, "onToolCallsChanged", {
                toolCalls: runtime.toolCalls
            });
            await notify(subscriber, "onToolCallResultEvent", {
                ...payloadBase,
                event: {
                    ...event,
                    toolCallName: toolCall.toolCallName
                }
            });
            break;
        }
        case "STATE_SNAPSHOT": {
            runtime.state = event.snapshot || {};
            await notify(subscriber, "onStateSnapshotEvent", {
                ...payloadBase,
                state: runtime.state
            });
            await notify(subscriber, "onStateChanged", {
                state: runtime.state,
                messages: runtime.messages
            });
            break;
        }
        case "STATE_DELTA": {
            runtime.state = applyStateDelta(runtime.state, event.delta);
            await notify(subscriber, "onStateDeltaEvent", {
                ...payloadBase,
                state: runtime.state
            });
            await notify(subscriber, "onStateChanged", {
                state: runtime.state,
                messages: runtime.messages
            });
            break;
        }
        case "RUN_FINISHED": {
            await notify(subscriber, "onRunFinishedEvent", payloadBase);
            break;
        }
        case "RUN_ERROR": {
            await notify(subscriber, "onRunErrorEvent", payloadBase);
            throw new Error(event.message || "请求失败，请稍后重试。");
        }
        default:
            break;
    }
}

export function createConsultationRunInput(query) {
    return {
        threadId: sharedThreadId,
        runId: createId(),
        messages: [
            {
                id: createId(),
                role: "user",
                content: query
            }
        ],
        state: {},
        forwardedProps: {}
    };
}

export async function runConsultation(query, subscriber) {
    const input = createConsultationRunInput(query);
    const runtime = createEmptyRuntime();
    const response = await fetch("/ag-ui/runs", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Accept: "text/event-stream"
        },
        body: JSON.stringify(input)
    });

    if (!response.ok) {
        const message = await response.text();
        throw new Error(message || `HTTP ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
        throw new Error("浏览器未返回可读取的流。");
    }

    const decoder = new TextDecoder("utf-8");
    let buffer = "";

    try {
        while (true) {
            const { done, value } = await reader.read();
            if (done) {
                break;
            }

            buffer += decoder.decode(value, { stream: true });
            const parsed = parseEventFrames(buffer);
            buffer = parsed.remainder;

            for (const event of parsed.frames) {
                await dispatchEvent(runtime, input, subscriber, event);
            }
        }

        buffer += decoder.decode();
        if (buffer.trim()) {
            const parsed = parseEventFrames(`${buffer}\n\n`);
            for (const event of parsed.frames) {
                await dispatchEvent(runtime, input, subscriber, event);
            }
        }
    } finally {
        await reader.cancel().catch(() => {});
    }

    return {
        messages: runtime.messages,
        state: runtime.state,
        toolCalls: runtime.toolCalls
    };
}

export function resetConsultationThread() {
    sharedThreadId = createId();
}
