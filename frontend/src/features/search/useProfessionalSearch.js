import { useMemo, useState } from "react";
import { searchLawArticles, searchRagEvidence } from "./api";

export function useProfessionalSearch() {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [lastQuery, setLastQuery] = useState("");
    const [lawItems, setLawItems] = useState([]);
    const [ragItems, setRagItems] = useState([]);
    const [selectedMap, setSelectedMap] = useState(new Map());

    async function submitSearch(query) {
        setLoading(true);
        setError("");
        setLastQuery(query);
        try {
            const [lawResults, ragResults] = await Promise.all([
                searchLawArticles(query),
                searchRagEvidence(query)
            ]);
            setLawItems(lawResults);
            setRagItems(ragResults);
        } catch (searchError) {
            setError(searchError.message || "检索失败，请稍后重试。");
            setLawItems([]);
            setRagItems([]);
        } finally {
            setLoading(false);
        }
    }

    function clearSearch() {
        setError("");
        setLastQuery("");
        setLawItems([]);
        setRagItems([]);
        setSelectedMap(new Map());
    }

    function toggleEvidence(source, item) {
        const key = buildEvidenceKey(source, item.articleId);
        setSelectedMap((current) => {
            const next = new Map(current);
            if (next.has(key)) {
                next.delete(key);
            } else {
                next.set(key, buildEvidenceRecord(source, item));
            }
            return next;
        });
    }

    const selectedItems = useMemo(() => Array.from(selectedMap.values()), [selectedMap]);

    return {
        loading,
        error,
        lastQuery,
        lawItems,
        ragItems,
        selectedItems,
        submitSearch,
        clearSearch,
        toggleEvidence,
        isSelected(source, articleId) {
            return selectedMap.has(buildEvidenceKey(source, articleId));
        }
    };
}

function buildEvidenceKey(source, articleId) {
    return `${source}:${articleId || "missing"}`;
}

function buildEvidenceRecord(source, item) {
    return {
        key: buildEvidenceKey(source, item.articleId),
        source,
        sourceLabel: source === "rag" ? `hybrid / ${item.matchType || "unknown"}` : "keyword",
        scoreLabel: source === "rag" ? `综合 ${formatScore(item.finalScore)}` : `相关度 ${formatScore(item.score)}`,
        documentTitle: item.documentTitle || "未知法规",
        articleNo: item.articleNo || "",
        fullPath: item.fullPath || "暂无章节路径",
        preview: item.preview || "暂无摘录"
    };
}

function formatScore(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }
    const numericValue = Number(value);
    if (Number.isNaN(numericValue)) {
        return String(value);
    }
    return numericValue.toFixed(2);
}
