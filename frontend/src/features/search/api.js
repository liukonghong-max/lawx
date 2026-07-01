export async function searchLawArticles(query) {
    return fetchSearch(`/api/law/articles/search?query=${encodeURIComponent(query)}&limit=8`);
}

export async function searchRagEvidence(query) {
    return fetchSearch(`/api/rag/search?query=${encodeURIComponent(query)}&limit=8`);
}

async function fetchSearch(url) {
    const response = await fetch(url);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200) {
        throw new Error(payload.message || "请求失败，请稍后重试。");
    }
    return Array.isArray(payload.data?.items) ? payload.data.items : [];
}
