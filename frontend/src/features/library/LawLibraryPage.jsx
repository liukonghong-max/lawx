import React from "react";
import { useLawLibrary } from "./useLawLibrary";

export default function LawLibraryPage() {
    const {
        documents,
        documentsLoading,
        documentsError,
        activeDocumentId,
        activeDocument,
        setActiveDocumentId,
        articles,
        articlesLoading,
        articlesError,
        articlePage,
        articlePageSize,
        articleTotal,
        setArticlePage,
        activeArticleId,
        setActiveArticleId,
        articleDetail,
        articleDetailLoading,
        articleDetailError
    } = useLawLibrary();

    const inDocumentView = Boolean(activeDocumentId && activeDocument);
    const totalPages = Math.max(1, Math.ceil(articleTotal / articlePageSize));

    return (
        <div className="library-page">
            {!inDocumentView ? (
                <section className="library-stage-panel">
                    <div className="library-breadcrumb">
                        <span className="active">法规库</span>
                    </div>
                    <div className="workspace-heading workspace-heading-compact">
                        <p className="eyebrow">法规库</p>
                        <h1>已入库法规浏览</h1>
                        <p className="summary">先查看法规库表格，再进入单部法规的条目目录与详情。</p>
                    </div>

                    <div className="library-master-table">
                        <div className="library-master-table-header">
                            <div>法规名称</div>
                            <div>类型</div>
                            <div>状态</div>
                            <div>生效日期</div>
                            <div>条文数</div>
                            <div>操作</div>
                        </div>
                        <div className="library-master-table-body">
                            {documentsLoading ? (
                                <div className="empty-state compact">
                                    <strong>正在加载法规列表</strong>
                                    <p>请稍候，正在读取已入库法规。</p>
                                </div>
                            ) : documentsError ? (
                                <div className="empty-state compact error-state">
                                    <strong>加载失败</strong>
                                    <p>{documentsError}</p>
                                </div>
                            ) : (
                                documents.map((item) => (
                                    <div key={item.documentId} className="library-master-row">
                                        <div>
                                            <strong>{item.title}</strong>
                                            <p className="library-document-date">{item.issuer || "发布机关待补充"}</p>
                                        </div>
                                        <div>{item.lawType || "法规"}</div>
                                        <div><span className="badge">{formatDocumentStatus(item.status)}</span></div>
                                        <div>{formatDate(item.effectiveDate)}</div>
                                        <div>{item.articleCount}</div>
                                        <div>
                                            <button
                                                type="button"
                                                className="secondary-button"
                                                onClick={() => setActiveDocumentId(item.documentId)}
                                            >
                                                查看条目
                                            </button>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </section>
            ) : (
                <section className="library-stage-panel">
                    <div className="library-breadcrumb">
                        <button
                            type="button"
                            className="library-breadcrumb-link"
                            onClick={() => setActiveDocumentId("")}
                        >
                            法规库
                        </button>
                        <span>/</span>
                        <span className="active">{activeDocument.title}</span>
                    </div>
                    <div className="library-stage-header">
                        <div className="workspace-heading workspace-heading-compact">
                            <p className="eyebrow">法规条目</p>
                            <h1>{activeDocument.title}</h1>
                            <p className="summary">点击条目行可在下方展开法条详情，再返回法规库切换其他法规。</p>
                        </div>
                        <button
                            type="button"
                            className="secondary-button"
                            onClick={() => setActiveDocumentId("")}
                        >
                            返回法规库
                        </button>
                    </div>

                    <div className="library-document-summary">
                        <span className="badge">{activeDocument.lawType || "法规"}</span>
                        <span className="badge">{activeDocument.articleCount} 条</span>
                        <span className="badge">{formatDocumentStatus(activeDocument.status)}</span>
                        <span className="library-document-summary-text">
                            生效日期：{formatDate(activeDocument.effectiveDate)}
                        </span>
                    </div>

                    <div className="library-master-table">
                        <div className="library-master-table-header library-master-table-header-articles">
                            <div>条号</div>
                            <div>目录路径</div>
                            <div>状态</div>
                        </div>
                        <div className="library-master-table-body">
                            {articlesLoading ? (
                                <div className="empty-state compact">
                                    <strong>正在加载条文目录</strong>
                                    <p>请稍候，正在读取该法规下的条文。</p>
                                </div>
                            ) : articlesError ? (
                                <div className="empty-state compact error-state">
                                    <strong>加载失败</strong>
                                    <p>{articlesError}</p>
                                </div>
                            ) : articles.length ? (
                                articles.map((item) => (
                                    <div key={item.articleId} className="library-article-block">
                                        <button
                                            type="button"
                                            className={`library-master-row library-master-row-articles ${activeArticleId === item.articleId ? "active" : ""}`}
                                            onClick={() => setActiveArticleId(item.articleId)}
                                        >
                                            <div><strong>{item.articleNo}</strong></div>
                                            <div>{item.fullPath || buildArticleTrail(item)}</div>
                                            <div><span className="badge">{formatDocumentStatus(item.effectiveStatus)}</span></div>
                                        </button>
                                        {activeArticleId === item.articleId ? (
                                            <div className="library-inline-detail">
                                                {articleDetailLoading ? (
                                                    <div className="empty-state compact">
                                                        <strong>正在加载详情</strong>
                                                        <p>请稍候，正在读取法条原文与元数据。</p>
                                                    </div>
                                                ) : articleDetailError ? (
                                                    <div className="empty-state compact error-state">
                                                        <strong>加载失败</strong>
                                                        <p>{articleDetailError}</p>
                                                    </div>
                                                ) : articleDetail ? (
                                                    <div className="library-detail-body">
                                                        <div className="compact-badges">
                                                            <span className="badge">{articleDetail.documentTitle}</span>
                                                            <span className="badge">{articleDetail.lawType || "法规"}</span>
                                                        </div>
                                                        <p className="citation-path">{articleDetail.fullPath}</p>
                                                        <div className="library-detail-content">{articleDetail.content}</div>
                                                        <div className="library-meta-grid">
                                                            <MetaItem label="发布机关" value={articleDetail.issuer} />
                                                            <MetaItem label="发布日期" value={formatDate(articleDetail.publishDate)} />
                                                            <MetaItem label="生效日期" value={formatDate(articleDetail.effectiveDate)} />
                                                            <MetaItem label="编" value={articleDetail.bookTitle} />
                                                            <MetaItem label="章" value={articleDetail.chapterTitle} />
                                                            <MetaItem label="节" value={articleDetail.sectionTitle} />
                                                            <MetaItem label="条文顺序" value={String(articleDetail.articleOrder)} />
                                                            <MetaItem label="条文状态" value={formatDocumentStatus(articleDetail.effectiveStatus)} />
                                                        </div>
                                                    </div>
                                                ) : null}
                                            </div>
                                        ) : null}
                                    </div>
                                ))
                            ) : (
                                <div className="empty-state compact">
                                    <strong>暂无条文</strong>
                                    <p>当前法规下还没有可浏览条文。</p>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="library-pagination">
                        <span>第 {articlePage} / {totalPages} 页</span>
                        <span>共 {articleTotal} 条</span>
                        <div className="library-pagination-actions">
                            <button
                                type="button"
                                className="secondary-button"
                                onClick={() => setArticlePage((page) => Math.max(1, page - 1))}
                                disabled={articlePage <= 1}
                            >
                                上一页
                            </button>
                            <button
                                type="button"
                                className="secondary-button"
                                onClick={() => setArticlePage((page) => Math.min(totalPages, page + 1))}
                                disabled={articlePage >= totalPages}
                            >
                                下一页
                            </button>
                        </div>
                    </div>
                </section>
            )}
        </div>
    );
}

function MetaItem({ label, value }) {
    return (
        <div className="library-meta-item">
            <span>{label}</span>
            <strong>{value || "-"}</strong>
        </div>
    );
}

function buildArticleTrail(item) {
    return [item.bookTitle, item.chapterTitle, item.sectionTitle].filter(Boolean).join(" / ") || "条文";
}

function formatDate(value) {
    if (!value) {
        return "-";
    }
    return String(value);
}

function formatDocumentStatus(value) {
    switch (value) {
        case "effective":
            return "现行有效";
        case "amended":
            return "修订中";
        case "repealed":
            return "已失效";
        default:
            return value || "未知";
    }
}
