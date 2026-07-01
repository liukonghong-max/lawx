export async function listLawDocuments() {
    return fetchItems("/api/law/documents?limit=24");
}

export async function listDocumentArticles(documentId, page = 1, pageSize = 20) {
    const response = await fetch(`/api/law/documents/${documentId}/articles?page=${page}&pageSize=${pageSize}`);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200) {
        throw new Error(payload.message || "加载失败，请稍后重试。");
    }
    return {
        items: Array.isArray(payload.data?.items) ? payload.data.items : [],
        page: payload.data?.page || page,
        pageSize: payload.data?.pageSize || pageSize,
        total: payload.data?.total || 0
    };
}

export async function getLibraryArticleDetail(articleId) {
    const response = await fetch(`/api/law/articles/${articleId}`);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200 || !payload.data) {
        throw new Error(payload.message || "加载法条详情失败，请稍后重试。");
    }
    return payload.data;
}

async function fetchItems(url) {
    const response = await fetch(url);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200) {
        throw new Error(payload.message || "加载失败，请稍后重试。");
    }
    return Array.isArray(payload.data?.items) ? payload.data.items : [];
}
