const form = document.querySelector("#questionForm");
const input = document.querySelector("#questionInput");
const submitButton = document.querySelector("#submitButton");
const clearButton = document.querySelector("#clearButton");
const answerPanel = document.querySelector("#answerPanel");
const citationList = document.querySelector("#citationList");
const exampleButtons = document.querySelectorAll("[data-example]");
const navButtons = document.querySelectorAll("[data-view]");
const workspaceViews = document.querySelectorAll(".workspace-view");

const searchForm = document.querySelector("#searchForm");
const searchInput = document.querySelector("#searchInput");
const searchSubmitButton = document.querySelector("#searchSubmitButton");
const searchClearButton = document.querySelector("#searchClearButton");
const searchStatus = document.querySelector("#searchStatus");
const lawSearchResults = document.querySelector("#lawSearchResults");
const ragSearchResults = document.querySelector("#ragSearchResults");
const selectedEvidenceList = document.querySelector("#selectedEvidenceList");
const selectedEvidenceStatus = document.querySelector("#selectedEvidenceStatus");
const copyEvidenceButton = document.querySelector("#copyEvidenceButton");
const exportMarkdownButton = document.querySelector("#exportMarkdownButton");
const generateParagraphButton = document.querySelector("#generateParagraphButton");
const generatedParagraph = document.querySelector("#generatedParagraph");

const articleDetailCache = new Map();
const selectedEvidence = new Map();
let latestSearchResults = {
    lawItems: [],
    ragItems: []
};

navButtons.forEach((button) => {
    button.addEventListener("click", () => {
        if (button.classList.contains("disabled")) {
            return;
        }
        switchView(button.dataset.view);
    });
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const query = input.value.trim();
    if (!query) {
        showError("请先输入一个法律问题。");
        return;
    }
    await submitQuestion(query);
});

clearButton.addEventListener("click", () => {
    input.value = "";
    renderEmpty();
    input.focus();
});

exampleButtons.forEach((button) => {
    button.addEventListener("click", () => {
        input.value = button.dataset.example;
        input.focus();
    });
});

searchForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const query = searchInput.value.trim();
    if (!query) {
        renderSearchError("请先输入检索问题。");
        return;
    }
    await submitSearch(query);
});

searchClearButton.addEventListener("click", () => {
    searchInput.value = "";
    latestSearchResults = { lawItems: [], ragItems: [] };
    selectedEvidence.clear();
    renderSearchEmpty();
    renderSelectedEvidence();
    renderGeneratedPlaceholder();
    searchInput.focus();
});

copyEvidenceButton.addEventListener("click", async () => {
    const text = buildEvidenceText();
    if (!text) {
        renderSelectionStatus("请先勾选至少一条依据。", true);
        return;
    }
    try {
        await navigator.clipboard.writeText(text);
        renderSelectionStatus("已复制法律依据。");
    } catch (error) {
        renderSelectionStatus("复制失败，请手动复制。", true);
    }
});

exportMarkdownButton.addEventListener("click", () => {
    const markdown = buildEvidenceMarkdown();
    if (!markdown) {
        renderSelectionStatus("请先勾选至少一条依据。", true);
        return;
    }
    downloadTextFile("law4x-evidence.md", markdown);
    renderSelectionStatus("已导出 Markdown。");
});

generateParagraphButton.addEventListener("click", () => {
    const paragraph = buildEvidenceParagraph();
    if (!paragraph) {
        renderSelectionStatus("请先勾选至少一条依据。", true);
        return;
    }
    generatedParagraph.innerHTML = `<div class="generated-text">${escapeHtml(paragraph)}</div>`;
    renderSelectionStatus("已生成法律依据段落。");
});

function switchView(view) {
    navButtons.forEach((button) => {
        button.classList.toggle("active", button.dataset.view === view);
    });
    workspaceViews.forEach((workspace) => {
        const active = workspace.id === `${view}Workspace`;
        workspace.hidden = !active;
        workspace.classList.toggle("active", active);
    });
}

async function submitQuestion(query) {
    setLoading(true);
    renderLoading(query);
    try {
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
        renderAnswer(payload.data);
    } catch (error) {
        showError(error.message || "无法连接后端服务，请确认 Spring Boot 已启动。");
    } finally {
        setLoading(false);
    }
}

async function submitSearch(query) {
    setSearchLoading(true);
    renderSearchLoading(query);
    try {
        const [lawPayload, ragPayload] = await Promise.all([
            fetchSearchPayload(`/api/law/articles/search?query=${encodeURIComponent(query)}&limit=8`),
            fetchSearchPayload(`/api/rag/search?query=${encodeURIComponent(query)}&limit=8`)
        ]);
        latestSearchResults = {
            lawItems: Array.isArray(lawPayload.data?.items) ? lawPayload.data.items : [],
            ragItems: Array.isArray(ragPayload.data?.items) ? ragPayload.data.items : []
        };
        renderSearchResults(query, latestSearchResults);
    } catch (error) {
        renderSearchError(error.message || "检索失败，请稍后重试。");
    } finally {
        setSearchLoading(false);
    }
}

async function fetchSearchPayload(url) {
    const response = await fetch(url);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200) {
        throw new Error(payload.message || "请求失败，请稍后重试。");
    }
    return payload;
}

function setLoading(loading) {
    submitButton.disabled = loading;
    submitButton.textContent = loading ? "生成中..." : "生成回答";
}

function setSearchLoading(loading) {
    searchSubmitButton.disabled = loading;
    searchSubmitButton.textContent = loading ? "检索中..." : "开始检索";
}

function renderLoading(query) {
    answerPanel.innerHTML = `
        <div class="empty-state">
            <strong>正在检索法规依据</strong>
            <p>${escapeHtml(query)}</p>
        </div>
    `;
    citationList.innerHTML = `
        <div class="empty-state compact">
            <strong>等待引用依据</strong>
            <p>系统会先检索相关法条，再生成回答。</p>
        </div>
    `;
}

function renderAnswer(data) {
    const citations = Array.isArray(data.citations) ? data.citations : [];
    answerPanel.innerHTML = `
        <div class="answer-meta">
            <span class="badge">已生成回答</span>
            <span class="badge">${citations.length} 条引用依据</span>
        </div>
        <div class="answer-text">${escapeHtml(data.answer || "未生成回答。")}</div>
        <div class="notice">内容仅供参考，不构成正式法律意见。复杂或高风险事项建议咨询专业律师。</div>
    `;
    renderCitations(citations);
}

function renderCitations(citations) {
    if (!citations.length) {
        citationList.innerHTML = `
            <div class="empty-state compact">
                <strong>未返回引用</strong>
                <p>当前回答没有可展示的 citations，请尝试补充问题事实。</p>
            </div>
        `;
        return;
    }

    citationList.innerHTML = citations.map((citation, index) => `
        <article class="citation-card" data-article-id="${escapeHtml(citation.articleId || "")}">
            <div class="citation-title">
                <strong><span class="citation-index">[${index + 1}]</span> ${escapeHtml(citation.documentTitle || "未知法规")}</strong>
                <span>${escapeHtml(citation.articleNo || "")}</span>
            </div>
            <p class="citation-path">${escapeHtml(citation.fullPath || "暂无章节路径")}</p>
            <details>
                <summary>查看摘录</summary>
                <p class="citation-preview">${escapeHtml(citation.quotedText || citation.preview || "暂无摘录")}</p>
            </details>
            <details class="citation-detail" ${citation.articleId ? "" : "disabled"}>
                <summary>${citation.articleId ? "查看完整条文" : "暂无完整条文"}</summary>
                <div class="citation-detail-content">
                    ${citation.articleId
                        ? '<p class="citation-detail-placeholder">展开后加载完整条文。</p>'
                        : '<p class="citation-detail-error">当前引用缺少法条标识，无法加载完整条文。</p>'}
                </div>
            </details>
        </article>
    `).join("");

    bindCitationDetailEvents();
}

function renderSearchLoading(query) {
    searchStatus.innerHTML = `
        <span class="badge">正在检索</span>
        <span class="badge">${escapeHtml(query)}</span>
    `;
    lawSearchResults.innerHTML = createCompactEmptyState("关键词检索中", "正在查询法条库。");
    ragSearchResults.innerHTML = createCompactEmptyState("Hybrid 检索中", "正在融合关键词和向量结果。");
}

function renderSearchResults(query, results) {
    const lawItems = results.lawItems || [];
    const ragItems = results.ragItems || [];
    searchStatus.innerHTML = `
        <span class="badge">已完成</span>
        <span class="badge">${lawItems.length} 条关键词结果</span>
        <span class="badge">${ragItems.length} 条 hybrid 结果</span>
        <span class="badge">${escapeHtml(query)}</span>
    `;
    lawSearchResults.innerHTML = lawItems.length
        ? lawItems.map((item, index) => renderSearchResultCard(item, "law", index)).join("")
        : createCompactEmptyState("没有关键词结果", "可以尝试换成法规名、条号或更具体的争议事实。");
    ragSearchResults.innerHTML = ragItems.length
        ? ragItems.map((item, index) => renderSearchResultCard(item, "rag", index)).join("")
        : createCompactEmptyState("没有 hybrid 结果", "可以尝试更完整的自然语言问题。");

    bindSearchResultEvents();
    renderSelectedEvidence();
}

function renderSearchResultCard(item, source, index) {
    const key = buildEvidenceKey(source, item.articleId);
    const checked = selectedEvidence.has(key) ? "checked" : "";
    const score = source === "rag"
        ? `综合 ${formatScore(item.finalScore)}`
        : `相关度 ${formatScore(item.score)}`;
    const metaBadges = source === "rag"
        ? `
            <span class="badge">${escapeHtml(item.matchType || "unknown")}</span>
            <span class="badge">${score}</span>
          `
        : `
            <span class="badge">keyword</span>
            <span class="badge">${score}</span>
          `;
    const reason = source === "rag" && item.reason
        ? `<p class="result-reason">${escapeHtml(item.reason)}</p>`
        : "";
    return `
        <article class="result-card" data-source="${source}" data-article-id="${escapeHtml(item.articleId || "")}">
            <label class="result-select">
                <input type="checkbox" data-role="select-evidence" data-key="${escapeHtml(key)}" ${checked}>
                <span>加入依据</span>
            </label>
            <div class="citation-title">
                <strong><span class="citation-index">[${index + 1}]</span> ${escapeHtml(item.documentTitle || "未知法规")}</strong>
                <span>${escapeHtml(item.articleNo || "")}</span>
            </div>
            <div class="result-meta">${metaBadges}</div>
            <p class="citation-path">${escapeHtml(item.fullPath || "暂无章节路径")}</p>
            <p class="citation-preview">${escapeHtml(item.preview || "暂无摘录")}</p>
            ${reason}
        </article>
    `;
}

function bindSearchResultEvents() {
    document.querySelectorAll("[data-role='select-evidence']").forEach((checkbox) => {
        if (checkbox.dataset.bound === "true") {
            return;
        }
        checkbox.dataset.bound = "true";
        checkbox.addEventListener("change", (event) => {
            const inputElement = event.currentTarget;
            const card = inputElement.closest(".result-card");
            if (!card) {
                return;
            }
            const source = card.dataset.source;
            const articleId = card.dataset.articleId;
            const item = findSearchItem(source, articleId);
            if (!item) {
                return;
            }
            const key = buildEvidenceKey(source, articleId);
            if (inputElement.checked) {
                selectedEvidence.set(key, buildEvidenceRecord(source, item));
            } else {
                selectedEvidence.delete(key);
            }
            renderSelectedEvidence();
            renderSelectionStatus(`${selectedEvidence.size} 条依据已加入清单。`);
        });
    });
}

function renderSelectedEvidence() {
    const items = Array.from(selectedEvidence.values());
    if (!items.length) {
        selectedEvidenceList.innerHTML = createCompactEmptyState("尚未选择依据", "从左侧结果中勾选法条后，会在这里汇总。");
        selectedEvidenceStatus.textContent = "";
        return;
    }

    selectedEvidenceList.innerHTML = items.map((item, index) => `
        <article class="citation-card">
            <div class="citation-title">
                <strong><span class="citation-index">[${index + 1}]</span> ${escapeHtml(item.documentTitle)}</strong>
                <span>${escapeHtml(item.articleNo)}</span>
            </div>
            <div class="result-meta">
                <span class="badge">${escapeHtml(item.sourceLabel)}</span>
                <span class="badge">${escapeHtml(item.scoreLabel)}</span>
            </div>
            <p class="citation-path">${escapeHtml(item.fullPath)}</p>
            <p class="citation-preview">${escapeHtml(item.preview)}</p>
        </article>
    `).join("");
    selectedEvidenceStatus.textContent = `已选择 ${items.length} 条依据`;
}

function renderSearchEmpty() {
    searchStatus.innerHTML = "";
    lawSearchResults.innerHTML = createCompactEmptyState("等待检索", "这里会显示按法条关键词命中的结果。");
    ragSearchResults.innerHTML = createCompactEmptyState("等待检索", "这里会显示关键词和向量融合后的结果。");
}

function renderSearchError(message) {
    searchStatus.innerHTML = `<span class="badge badge-error">${escapeHtml(message)}</span>`;
    lawSearchResults.innerHTML = createCompactEmptyState("检索失败", "请稍后重试或检查后端服务。");
    ragSearchResults.innerHTML = createCompactEmptyState("检索失败", "请稍后重试或检查后端服务。");
}

function renderGeneratedPlaceholder() {
    generatedParagraph.innerHTML = createCompactEmptyState("等待生成", "选择法条后可生成整理好的法律依据段落。");
}

function renderSelectionStatus(message, isError = false) {
    selectedEvidenceStatus.textContent = message;
    selectedEvidenceStatus.classList.toggle("selection-status-error", isError);
}

function findSearchItem(source, articleId) {
    const items = source === "rag" ? latestSearchResults.ragItems : latestSearchResults.lawItems;
    return items.find((item) => String(item.articleId) === String(articleId));
}

function buildEvidenceRecord(source, item) {
    return {
        source,
        sourceLabel: source === "rag" ? `hybrid / ${item.matchType || "unknown"}` : "keyword",
        documentTitle: item.documentTitle || "未知法规",
        articleNo: item.articleNo || "",
        fullPath: item.fullPath || "暂无章节路径",
        preview: item.preview || "暂无摘录",
        scoreLabel: source === "rag"
            ? `综合 ${formatScore(item.finalScore)}`
            : `相关度 ${formatScore(item.score)}`
    };
}

function buildEvidenceKey(source, articleId) {
    return `${source}:${articleId || "missing"}`;
}

function buildEvidenceText() {
    const items = Array.from(selectedEvidence.values());
    if (!items.length) {
        return "";
    }
    return items.map((item, index) => {
        return [
            `[${index + 1}] ${item.documentTitle} ${item.articleNo}`,
            item.fullPath,
            item.preview
        ].join("\n");
    }).join("\n\n");
}

function buildEvidenceMarkdown() {
    const items = Array.from(selectedEvidence.values());
    if (!items.length) {
        return "";
    }
    return items.map((item, index) => {
        return [
            `## [${index + 1}] ${item.documentTitle} ${item.articleNo}`,
            "",
            `- 来源：${item.sourceLabel}`,
            `- 评分：${item.scoreLabel}`,
            `- 路径：${item.fullPath}`,
            "",
            item.preview
        ].join("\n");
    }).join("\n\n");
}

function buildEvidenceParagraph() {
    const items = Array.from(selectedEvidence.values());
    if (!items.length) {
        return "";
    }
    return items.map((item) => {
        return `《${item.documentTitle}》${item.articleNo}规定：${item.preview}`;
    }).join("；");
}

function downloadTextFile(filename, content) {
    const blob = new Blob([content], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
}

function formatScore(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }
    const numberValue = Number(value);
    if (Number.isNaN(numberValue)) {
        return String(value);
    }
    return numberValue.toFixed(2);
}

function createCompactEmptyState(title, message) {
    return `
        <div class="empty-state compact">
            <strong>${escapeHtml(title)}</strong>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

function bindCitationDetailEvents() {
    citationList.querySelectorAll(".citation-detail").forEach((detail) => {
        if (detail.dataset.bound === "true" || detail.hasAttribute("disabled")) {
            return;
        }
        detail.dataset.bound = "true";
        detail.addEventListener("toggle", async () => {
            if (!detail.open || detail.dataset.loaded === "true" || detail.dataset.loading === "true") {
                return;
            }
            const article = detail.closest(".citation-card");
            const articleId = article?.dataset.articleId;
            if (!articleId) {
                return;
            }
            await loadCitationDetail(detail, articleId);
        });
    });
}

async function loadCitationDetail(detailElement, articleId) {
    const container = detailElement.querySelector(".citation-detail-content");
    if (!container) {
        return;
    }

    detailElement.dataset.loading = "true";
    container.innerHTML = '<p class="citation-detail-placeholder">正在加载完整条文...</p>';

    try {
        const detail = await fetchArticleDetail(articleId);
        detailElement.dataset.loaded = "true";
        container.innerHTML = `
            <p class="citation-detail-path">${escapeHtml(detail.fullPath || "暂无章节路径")}</p>
            <p class="citation-detail-text">${escapeHtml(detail.content || "暂无完整条文")}</p>
        `;
    } catch (error) {
        container.innerHTML = `
            <p class="citation-detail-error">完整条文加载失败：${escapeHtml(error.message || "请稍后重试。")}</p>
        `;
    } finally {
        delete detailElement.dataset.loading;
    }
}

async function fetchArticleDetail(articleId) {
    if (articleDetailCache.has(articleId)) {
        return articleDetailCache.get(articleId);
    }

    const response = await fetch(`/api/law/articles/${articleId}`);
    const payload = await response.json();
    if (!response.ok || payload.code !== 200 || !payload.data) {
        throw new Error(payload.message || "请稍后重试。");
    }
    articleDetailCache.set(articleId, payload.data);
    return payload.data;
}

function showError(message) {
    answerPanel.innerHTML = `<div class="error-message">${escapeHtml(message)}</div>`;
    citationList.innerHTML = `
        <div class="empty-state compact">
            <strong>暂无引用</strong>
            <p>请求成功后会显示引用依据。</p>
        </div>
    `;
}

function renderEmpty() {
    answerPanel.innerHTML = `
        <div class="empty-state">
            <strong>等待提问</strong>
            <p>回答会显示在这里，并在右侧同步列出引用依据。</p>
        </div>
    `;
    citationList.innerHTML = `
        <div class="empty-state compact">
            <strong>暂无引用</strong>
            <p>生成回答后会显示法规名、条号、路径和摘录。</p>
        </div>
    `;
}

function escapeHtml(value) {
    return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
}

renderSearchEmpty();
renderSelectedEvidence();
renderGeneratedPlaceholder();
