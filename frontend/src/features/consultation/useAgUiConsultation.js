import { useRef, useState } from "react";
import { getAgUiMessageCitations, getLawArticleDetail } from "./api";
import { resetConsultationThread, runConsultation } from "./aguiClient";

export function useAgUiConsultation() {
    const [loading, setLoading] = useState(false);
    const [answer, setAnswer] = useState("");
    const [reasoning, setReasoning] = useState("");
    const [toolCalls, setToolCalls] = useState([]);
    const [citations, setCitations] = useState([]);
    const [error, setError] = useState("");
    const [statusText, setStatusText] = useState("");
    const detailCacheRef = useRef(new Map());

    function normalizeCitations(items) {
        if (!Array.isArray(items)) {
            return [];
        }
        return items.map((citation) => ({
            articleId: citation?.articleId || "",
            documentTitle: citation?.documentTitle || "",
            articleNo: citation?.articleNo || "",
            fullPath: citation?.fullPath || "",
            quotedText: citation?.quotedText || ""
        }));
    }

    async function submitQuestion(query) {
        setLoading(true);
        setError("");
        setAnswer("");
        setReasoning("");
        setToolCalls([]);
        setCitations([]);
        setStatusText("正在连接 AG-UI 流式回答");

        try {
            const result = await runConsultation(query, {
                onRunStartedEvent() {
                    setStatusText("正在生成回答");
                },
                onTextMessageContentEvent({ textMessageBuffer }) {
                    setAnswer(textMessageBuffer || "");
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
                onRunFinishedEvent() {
                    setStatusText("正在整理引用依据");
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
            if (assistantMessage?.id) {
                try {
                    const citationPayload = await getAgUiMessageCitations(assistantMessage.id);
                    setCitations(normalizeCitations(citationPayload?.citations));
                } catch (citationError) {
                    setCitations([]);
                    setError((currentError) => currentError || citationError.message || "依据加载失败，请稍后重试。");
                }
            }
            setStatusText("回答完成");
        } catch (submitError) {
            setError(submitError.message || "无法连接后端服务。");
            setAnswer("");
            setReasoning("");
            setToolCalls([]);
            setCitations([]);
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
        setError("");
        setStatusText("");
    }

    return {
        loading,
        answer,
        reasoning,
        toolCalls,
        citations,
        error,
        statusText,
        submitQuestion,
        loadCitationDetail,
        reset
    };
}
