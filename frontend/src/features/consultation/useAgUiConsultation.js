import { useRef, useState } from "react";
import { getLawArticleDetail } from "./api";
import { resetConsultationThread, runConsultation } from "./aguiClient";

export function useAgUiConsultation() {
    const [loading, setLoading] = useState(false);
    const [answer, setAnswer] = useState("");
    const [reasoning, setReasoning] = useState("");
    const [toolCalls, setToolCalls] = useState([]);
    const [citations, setCitations] = useState([]);
    const [answerSegments, setAnswerSegments] = useState([]);
    const [error, setError] = useState("");
    const [statusText, setStatusText] = useState("");
    const detailCacheRef = useRef(new Map());

    function applyStructuredState(state) {
        if (!state || typeof state !== "object") {
            return;
        }

        if (typeof state.answer === "string") {
            setAnswer(state.answer);
        }

        if (Array.isArray(state.citations)) {
            setCitations(
                state.citations.map((citation) => ({
                    articleId: citation?.articleId || "",
                    documentTitle: citation?.documentTitle || "",
                    articleNo: citation?.articleNo || "",
                    fullPath: citation?.fullPath || "",
                    quotedText: citation?.quotedText || ""
                }))
            );
        }

        if (Array.isArray(state.answerSegments)) {
            setAnswerSegments(
                state.answerSegments.map((segment, index) => ({
                    id: segment?.id || `seg-${index + 1}`,
                    text: segment?.text || "",
                    citationIds: Array.isArray(segment?.citationIds) ? segment.citationIds.filter(Boolean) : []
                }))
            );
        }
    }

    async function submitQuestion(query) {
        setLoading(true);
        setError("");
        setAnswer("");
        setReasoning("");
        setToolCalls([]);
        setCitations([]);
        setAnswerSegments([]);
        setStatusText("正在连接 AG-UI 流式回答");

        try {
            const result = await runConsultation(query, {
                onRunStartedEvent() {
                    setStatusText("正在生成回答");
                },
                onTextMessageContentEvent({ textMessageBuffer }) {
                    setAnswer(textMessageBuffer || "");
                    setAnswerSegments([
                        {
                            id: "seg-streaming",
                            text: textMessageBuffer || "",
                            citationIds: []
                        }
                    ]);
                },
                onReasoningMessageStartEvent() {
                    setReasoning("");
                    setStatusText("正在分析问题");
                },
                onReasoningMessageContentEvent({ reasoningMessageBuffer }) {
                    setReasoning(reasoningMessageBuffer || "");
                    setStatusText("正在分析问题");
                },
                onReasoningMessageEndEvent({ reasoningMessageBuffer }) {
                    setReasoning(reasoningMessageBuffer || "");
                },
                onToolCallsChanged({ toolCalls: nextToolCalls }) {
                    setToolCalls(Array.isArray(nextToolCalls) ? [...nextToolCalls] : []);
                },
                onToolCallStartEvent({ event }) {
                    setStatusText(`正在调用工具：${event.toolCallName}`);
                },
                onToolCallEndEvent({ event }) {
                    setStatusText(`工具完成：${event.toolCallName}`);
                },
                onStateSnapshotEvent({ event }) {
                    applyStructuredState(event.snapshot);
                    setStatusText("正在整理引用依据");
                },
                onStateChanged({ state }) {
                    applyStructuredState(state);
                },
                onRunFinishedEvent() {
                    setStatusText("回答完成");
                },
                onRunErrorEvent({ event }) {
                    setError(event.message || "请求失败，请稍后重试。");
                }
            });

            const finalMessages = Array.isArray(result?.messages) ? result.messages : [];
            const assistantMessage = [...finalMessages].reverse().find((message) => message?.role === "assistant");
            if (assistantMessage?.content) {
                setAnswer(assistantMessage.content);
            }
            setToolCalls(Array.isArray(result?.toolCalls) ? result.toolCalls : []);
            applyStructuredState(result?.state);
        } catch (submitError) {
            setError(submitError.message || "无法连接后端服务。");
            setAnswer("");
            setReasoning("");
            setToolCalls([]);
            setCitations([]);
            setAnswerSegments([]);
            setStatusText("");
        } finally {
            setLoading(false);
        }
    }

    async function loadCitationDetail(articleId) {
        if (detailCacheRef.current.has(articleId)) {
            return detailCacheRef.current.get(articleId);
        }
        const detail = await getLawArticleDetail(articleId);
        detailCacheRef.current.set(articleId, detail);
        return detail;
    }

    function reset() {
        resetConsultationThread();
        setAnswer("");
        setReasoning("");
        setToolCalls([]);
        setCitations([]);
        setAnswerSegments([]);
        setError("");
        setStatusText("");
    }

    return {
        loading,
        answer,
        reasoning,
        toolCalls,
        answerSegments,
        citations,
        error,
        statusText,
        submitQuestion,
        loadCitationDetail,
        reset
    };
}
