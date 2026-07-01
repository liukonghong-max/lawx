import React, { useMemo, useState } from "react";
import { useProfessionalSearch } from "./useProfessionalSearch";

export default function ProfessionalSearchPage() {
    const [query, setQuery] = useState("房东不退押金怎么办");
    const [statusMessage, setStatusMessage] = useState("");
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

                    <div className="search-results-grid-react">
                        <ResultColumn
                            title="法条检索"
                            eyebrow="Keyword"
                            items={lawItems}
                            source="law"
                            emptyTitle={loading ? "关键词检索中" : "等待检索"}
                            emptyMessage={loading ? "正在查询法条库。" : "这里会显示按法条关键词命中的结果。"}
                            isSelected={isSelected}
                            onToggle={toggleEvidence}
                        />
                        <ResultColumn
                            title="RAG 检索"
                            eyebrow="Hybrid"
                            items={ragItems}
                            source="rag"
                            emptyTitle={loading ? "Hybrid 检索中" : "等待检索"}
                            emptyMessage={loading ? "正在融合关键词和向量结果。" : "这里会显示关键词和向量融合后的结果。"}
                            isSelected={isSelected}
                            onToggle={toggleEvidence}
                        />
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

function ResultColumn({ title, eyebrow, items, source, emptyTitle, emptyMessage, isSelected, onToggle }) {
    return (
        <section className="search-result-panel-react">
            <div className="panel-heading panel-heading-light">
                <p className="eyebrow">{eyebrow}</p>
                <h2>{title}</h2>
            </div>
            <div className="result-list-react">
                {items.length ? (
                    items.map((item, index) => {
                        const scoreLabel = source === "rag"
                            ? `综合 ${formatScore(item.finalScore)}`
                            : `相关度 ${formatScore(item.score)}`;
                        return (
                            <article key={`${source}-${item.articleId || index}`} className="result-card">
                                <label className="result-select">
                                    <input
                                        type="checkbox"
                                        checked={isSelected(source, item.articleId)}
                                        onChange={() => onToggle(source, item)}
                                    />
                                    <span>加入依据</span>
                                </label>
                                <div className="citation-title">
                                    <strong>
                                        <span className="citation-index">[{index + 1}]</span> {item.documentTitle || "未知法规"}
                                    </strong>
                                    <span>{item.articleNo || ""}</span>
                                </div>
                                <div className="answer-meta compact-badges">
                                    <span className="badge">{source === "rag" ? item.matchType || "unknown" : "keyword"}</span>
                                    <span className="badge">{scoreLabel}</span>
                                </div>
                                <p className="citation-path">{item.fullPath || "暂无章节路径"}</p>
                                <p className="citation-preview">{item.preview || "暂无摘录"}</p>
                                {source === "rag" && item.reason ? (
                                    <p className="result-reason">{item.reason}</p>
                                ) : null}
                            </article>
                        );
                    })
                ) : (
                    <div className="empty-state compact">
                        <strong>{emptyTitle}</strong>
                        <p>{emptyMessage}</p>
                    </div>
                )}
            </div>
        </section>
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
