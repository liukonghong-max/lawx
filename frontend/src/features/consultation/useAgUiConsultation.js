import { useRef, useState } from "react";
import { getLawArticleDetail } from "./api";
import { runConsultation } from "./aguiClient";

export function useAgUiConsultation() {
    const [loading, setLoading] = useState(false);
    const [answer, setAnswer] = useState("");
    const [citations, setCitations] = useState([]);
    const [answerSegments, setAnswerSegments] = useState([]);
    const [error, setError] = useState("");
    const [statusText, setStatusText] = useState("");
    const detailCacheRef = useRef(new Map());

    async function submitQuestion(query) {
        setLoading(true);
        setError("");
        setAnswer("");
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
                onToolCallStartEvent({ event }) {
                    setStatusText(`正在调用工具：${event.toolCallName}`);
                },
                onToolCallEndEvent({ event }) {
                    setStatusText(`工具完成：${event.toolCallName}`);
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
                setAnswerSegments([
                    {
                        id: assistantMessage.id || "seg-final",
                        text: assistantMessage.content,
                        citationIds: []
                    }
                ]);
            }
        } catch (submitError) {
            setError(submitError.message || "无法连接后端服务。");
            setAnswer("");
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
        setAnswer("");
        setCitations([]);
        setAnswerSegments([]);
        setError("");
        setStatusText("");
    }

    return {
        loading,
        answer,
        answerSegments,
        citations,
        error,
        statusText,
        submitQuestion,
        loadCitationDetail,
        reset
    };
}
