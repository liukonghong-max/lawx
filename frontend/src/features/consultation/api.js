export async function createRagAnswer(query) {
    const response = await fetch("/api/rag/answer", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            query,
            limit: 5
        })
    });
    const payload = await response.json();
    if (!response.ok || payload.code !== 200) {
        throw new Error(payload.message || "请求失败，请稍后重试。");
    }
    return payload.data;
}

export async function getLawArticleDetail(articleId) {
    const response = await fetch(`/api/law/articles/${articleId}`);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200 || !payload.data) {
        throw new Error(payload.message || "请稍后重试。");
    }
    return payload.data;
}
