import { useRef, useState } from "react";
import { createRagAnswer, getLawArticleDetail } from "./api";

export function useConsultation() {
    const [loading, setLoading] = useState(false);
    const [answer, setAnswer] = useState("");
    const [citations, setCitations] = useState([]);
    const [answerSegments, setAnswerSegments] = useState([]);
    const [error, setError] = useState("");
    const detailCacheRef = useRef(new Map());

    async function submitQuestion(query) {
        setLoading(true);
        setError("");
        try {
            const data = await createRagAnswer(query);
            setAnswer(data.answer || "");
            setCitations(Array.isArray(data.citations) ? data.citations : []);
            setAnswerSegments(Array.isArray(data.answerSegments) ? data.answerSegments : []);
        } catch (submitError) {
            setError(submitError.message || "无法连接后端服务。");
            setAnswer("");
            setCitations([]);
            setAnswerSegments([]);
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
    }

    return {
        loading,
        answer,
        answerSegments,
        citations,
        error,
        submitQuestion,
        loadCitationDetail,
        reset
    };
}
