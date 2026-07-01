import React, { useMemo, useState } from "react";
import { useProfessionalSearch } from "./useProfessionalSearch";

export default function ProfessionalSearchPage() {
    const [query, setQuery] = useState("房东不退押金怎么办");
    const [statusMessage, setStatusMessage] = useState("");
    const [activeTab, setActiveTab] = useState("law");
    const {
        loading,
        error,
        lastQuery,
        lawItems,
        ragItems,
        selectedItems,
        submitSearch,
        clearSearch,
        toggleEvidence,
        isSelected
    } = useProfessionalSearch();

    const resultState = useMemo(() => {
        if (loading) {
            return {
                badges: ["正在检索", query]
            };
        }
        if (error) {
            return {
                badges: [error]
            };
        }
        if (!lastQuery) {
            return { badges: [] };
        }
        return {
            badges: ["已完成", `${lawItems.length} 条关键词结果`, `${ragItems.length} 条 hybrid 结果`, lastQuery]
        };
    }, [error, lastQuery, lawItems.length, loading, query, ragItems.length]);

    async function handleSubmit(event) {
        event.preventDefault();
        const trimmed = query.trim();
        if (!trimmed) {
            return;
        }
        setStatusMessage("");
        await submitSearch(trimmed);
    }

    function handleClear() {
        setQuery("");
        setStatusMessage("");
        clearSearch();
    }

    async function handleCopyEvidence() {
        const content = buildEvidenceText(selectedItems);
        if (!content) {
            setStatusMessage("请先勾选至少一条依据。");
            return;
        }
        try {
            await navigator.clipboard.writeText(content);
            setStatusMessage("已复制法律依据。");
        } catch (copyError) {
            setStatusMessage("复制失败，请手动复制。");
        }
    }

    function handleExportMarkdown() {
        const content = buildEvidenceMarkdown(selectedItems);
        if (!content) {
            setStatusMessage("请先勾选至少一条依据。");
            return;
        }
        downloadTextFile("law4x-evidence.md", content);
        setStatusMessage("已导出 Markdown。");
    }

    const activeItems = activeTab === "law" ? lawItems : ragItems;

    return (
        <div className="search-layout-react">
            <section className="search-main-panel">
                <div className="workspace-heading">
                    <p className="eyebrow">专业检索</p>
                    <h1>法律依据工作台</h1>
                    <p className="summary">同时查看关键词检索和 hybrid 检索结果，整理、复制和导出可复用的法律依据。</p>
                </div>

                <form className="question-form" onSubmit={handleSubmit}>
                    <label htmlFor="searchInput">检索问题</label>
                    <textarea
                        id="searchInput"
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder="例如：房东不退押金怎么办"
                        required
                    />
                    <div className="form-row">
                        <button className="primary-button" type="submit" disabled={loading}>
                            {loading ? "检索中..." : "开始检索"}
                        </button>
                        <button className="secondary-button" type="button" onClick={handleClear}>
                            清空结果
                        </button>
                    </div>
                </form>

                <section className="search-results-shell-react">
                    <div className="search-results-header">
                        <div>
                            <p className="eyebrow">结果</p>
                            <h2>检索结果</h2>
                        </div>
                        <div className="answer-meta">
                            {resultState.badges.map((badge) => (
                                <span key={badge} className={`badge ${error ? "badge-error" : ""}`}>
                                    {badge}
                                </span>
                            ))}
                        </div>
                    </div>

                    <div className="search-tabs">
                        <button
                            type="button"
                            className={`search-tab ${activeTab === "law" ? "active" : ""}`}
                            onClick={() => setActiveTab("law")}
                        >
                            法条检索
                            <span className="search-tab-count">{lawItems.length}</span>
                        </button>
                        <button
                            type="button"
                            className={`search-tab ${activeTab === "rag" ? "active" : ""}`}
                            onClick={() => setActiveTab("rag")}
                        >
                            RAG 检索
                            <span className="search-tab-count">{ragItems.length}</span>
                        </button>
                    </div>
                    <ResultTable
                        items={activeItems}
                        source={activeTab}
                        loading={loading}
                        emptyTitle={activeTab === "law"
                            ? (loading ? "关键词检索中" : "等待检索")
                            : (loading ? "Hybrid 检索中" : "等待检索")}
                        emptyMessage={activeTab === "law"
                            ? (loading ? "正在查询法条库。" : "这里会显示按法条关键词命中的结果。")
                            : (loading ? "正在融合关键词和向量结果。" : "这里会显示关键词和向量融合后的结果。")}
                        isSelected={isSelected}
                        onToggle={toggleEvidence}
                    />
                    <div className="search-score-note">
                        <span className="badge">说明</span>
                        <span>Hybrid 结果展示命中方式、综合分、关键词分和语义分，便于判断依据来源与稳定性。</span>
                    </div>
                </section>
            </section>

            <aside className="context-panel search-selection-panel">
                <div className="panel-heading">
                    <p className="eyebrow">整理</p>
                    <h2>已选依据</h2>
                </div>
                <div className="selection-actions">
                    <button className="secondary-button" type="button" onClick={handleCopyEvidence}>
                        复制法律依据
                    </button>
                    <button className="secondary-button" type="button" onClick={handleExportMarkdown}>
                        导出 Markdown
                    </button>
                </div>
                <div className="selection-status">{statusMessage}</div>
                <div className="citation-list-react">
                    {selectedItems.length ? (
                        selectedItems.map((item, index) => (
                            <article key={item.key} className="citation-card">
                                <div className="citation-title">
                                    <strong>
                                        <span className="citation-index">[{index + 1}]</span> {item.documentTitle}
                                    </strong>
                                    <span>{item.articleNo}</span>
                                </div>
                                <div className="answer-meta compact-badges">
                                    <span className="badge">{item.sourceLabel}</span>
                                    <span className="badge">{item.scoreLabel}</span>
                                </div>
                                <p className="citation-path">{item.fullPath}</p>
                                <p className="citation-preview">{item.preview}</p>
                            </article>
                        ))
                    ) : (
                        <div className="empty-state compact">
                            <strong>尚未选择依据</strong>
                            <p>从左侧结果中勾选法条后，会在这里汇总。</p>
                        </div>
                    )}
                </div>
            </aside>
        </div>
    );
}

function ResultTable({ items, source, loading, emptyTitle, emptyMessage, isSelected, onToggle }) {
    return (
        <div className="search-table-shell">
            <div className="search-table-header">
                <div>选择</div>
                <div>法规与条号</div>
                <div>命中方式</div>
                <div>{source === "rag" ? "分数" : "相关度"}</div>
                <div>摘要</div>
            </div>
            <div className="search-table-body">
                {items.length ? (
                    items.map((item, index) => (
                        <div key={`${source}-${item.articleId || index}`} className="search-table-row">
                            <div className="search-table-cell search-table-cell-check">
                                <label className="result-select">
                                    <input
                                        type="checkbox"
                                        checked={isSelected(source, item.articleId)}
                                        onChange={() => onToggle(source, item)}
                                    />
                                    <span>加入</span>
                                </label>
                            </div>
                            <div className="search-table-cell">
                                <div className="search-table-title">
                                    <strong>{item.documentTitle || "未知法规"}</strong>
                                    <span>{item.articleNo || ""}</span>
                                </div>
                                <p className="search-table-path">{item.fullPath || "暂无章节路径"}</p>
                            </div>
                            <div className="search-table-cell">
                                <div className="compact-badges">
                                    <span className="badge">{source === "rag" ? "Hybrid" : "Keyword"}</span>
                                    <span className="badge">
                                        {source === "rag" ? formatMatchType(item.matchType) : "条文关键词"}
                                    </span>
                                </div>
                            </div>
                            <div className="search-table-cell">
                                {source === "rag" ? (
                                    <div className="search-table-score-list">
                                        <span>综合 {formatScore(item.finalScore)}</span>
                                        <span>关键词 {formatScore(item.keywordScore)}</span>
                                        <span>语义 {formatScore(item.vectorScore)}</span>
                                    </div>
                                ) : (
                                    <div className="search-table-score-list">
                                        <span>相关度 {formatScore(item.score)}</span>
                                    </div>
                                )}
                            </div>
                            <div className="search-table-cell">
                                <p className="search-table-preview">{item.preview || "暂无摘录"}</p>
                                {source === "rag" && item.reason ? (
                                    <p className="search-table-reason">{item.reason}</p>
                                ) : null}
                            </div>
                        </div>
                    ))
                ) : (
                    <div className="empty-state compact">
                        <strong>{loading ? emptyTitle : emptyTitle}</strong>
                        <p>{emptyMessage}</p>
                    </div>
                )}
            </div>
        </div>
    );
}

function buildEvidenceText(items) {
    if (!items.length) {
        return "";
    }
    return items.map((item, index) => {
        return [`[${index + 1}] ${item.documentTitle} ${item.articleNo}`, item.fullPath, item.preview].join("\n");
    }).join("\n\n");
}

function buildEvidenceMarkdown(items) {
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

function downloadTextFile(filename, content) {
    const blob = new Blob([content], { type: "text/markdown;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
}

function formatMatchType(value) {
    switch (value) {
        case "hybrid":
            return "关键词 + 语义";
        case "vector":
            return "语义召回";
        case "keyword":
            return "关键词";
        default:
            return value || "未知";
    }
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
