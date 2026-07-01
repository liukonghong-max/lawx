import React from "react";
import { useMemo, useState } from "react";
import { useConsultation } from "./useConsultation";

const examples = [
    "别人欠钱不还怎么办",
    "房东不退押金怎么办",
    "公司拖欠工资怎么办"
];

const answerActions = ["复制", "有用", "无用", "分享"];

export default function ConsultationPage() {
    const [query, setQuery] = useState("别人欠钱不还怎么办");
    const [submittedQuery, setSubmittedQuery] = useState("");
    const [openedId, setOpenedId] = useState("");
    const [activeCitationId, setActiveCitationId] = useState("");
    const [highlightedSegmentCitationId, setHighlightedSegmentCitationId] = useState("");
    const [showAllCitations, setShowAllCitations] = useState(false);
    const [detailState, setDetailState] = useState({});
    const { loading, answer, answerSegments, citations, error, submitQuestion, loadCitationDetail, reset } = useConsultation();
    const visibleCitations = showAllCitations ? citations : citations.slice(0, 3);
    const hiddenCitationCount = Math.max(citations.length - visibleCitations.length, 0);
    const citationIndexMap = useMemo(() => {
        const map = new Map();
        citations.forEach((c, index) => {
            if (c.articleId) {
                map.set(c.articleId, index + 1);
            }
        });
        return map;
    }, [citations]);

    const answerState = useMemo(() => {
        if (loading) {
            return { title: "正在检索法规依据", body: query };
        }
        if (error) {
            return { title: "请求失败", body: error, error: true };
        }
        if (!answer) {
            return { title: "等待提问", body: "回答会显示在这里，并在右侧同步列出引用依据。" };
        }
        return null;
    }, [answer, error, loading, query]);

    async function handleSubmit(event) {
        event.preventDefault();
        const trimmed = query.trim();
        if (!trimmed) {
            return;
        }
        setShowAllCitations(false);
        setActiveCitationId("");
        setHighlightedSegmentCitationId("");
        setSubmittedQuery(trimmed);
        setOpenedId("");
        setDetailState({});
        await submitQuestion(trimmed);
    }

    function handleReset() {
        setQuery("");
        setSubmittedQuery("");
        setOpenedId("");
        setShowAllCitations(false);
        setActiveCitationId("");
        setHighlightedSegmentCitationId("");
        setDetailState({});
        reset();
    }

    async function handleToggleCitation(citation) {
        if (!citation.articleId) {
            return;
        }
        const nextOpenedId = openedId === citation.articleId ? "" : citation.articleId;
        setOpenedId(nextOpenedId);
        if (!nextOpenedId || detailState[citation.articleId]?.detail || detailState[citation.articleId]?.loading) {
            return;
        }
        setDetailState((current) => ({
            ...current,
            [citation.articleId]: { loading: true, error: "" }
        }));
        try {
            const detail = await loadCitationDetail(citation.articleId);
            setDetailState((current) => ({
                ...current,
                [citation.articleId]: { loading: false, error: "", detail }
            }));
        } catch (detailError) {
            setDetailState((current) => ({
                ...current,
                [citation.articleId]: {
                    loading: false,
                    error: detailError.message || "请稍后重试。"
                }
            }));
        }
    }

    async function handleFocusCitation(citation) {
        if (!citation.articleId) {
            return;
        }
        if (!showAllCitations && citations.slice(3).some((item) => item.articleId === citation.articleId)) {
            setShowAllCitations(true);
        }
        setActiveCitationId(citation.articleId);
        await handleToggleCitation(citation);
    }

    function handleCitationHover(citationId) {
        setHighlightedSegmentCitationId(citationId);
    }

    function handleCitationLeave() {
        setHighlightedSegmentCitationId("");
    }

    return (
        <div className="consultation-layout">
            <section className="conversation-shell">
                <div className="conversation-toolbar">
                    <div className="workspace-heading workspace-heading-compact">
                        <p className="eyebrow">法律咨询</p>
                        <h1>基于法条依据的问答</h1>
                        <p className="summary">围绕具体法律问题生成带依据的回答，并在右侧同步展开引用条文。</p>
                    </div>
                    <div className="toolbar-meta">
                        <span className="toolbar-hint">案例咨询</span>
                        <button className="toolbar-action" type="button" onClick={handleReset}>
                            清空对话
                        </button>
                    </div>
                </div>

                <section className="conversation-card">
                    {submittedQuery ? (
                        <article className="message-row user">
                            <div className="avatar">人</div>
                            <div className="message-bubble user-bubble">
                                <div className="message-meta">
                                    <strong>你</strong>
                                    <span>今天 10:24</span>
                                </div>
                                <p>{submittedQuery}</p>
                            </div>
                        </article>
                    ) : null}

                    <article className="message-row assistant">
                        <div className="avatar assistant-avatar">法</div>
                        <div className="message-bubble assistant-bubble">
                            {answerState ? (
                                <div className={`empty-state ${answerState.error ? "error-state" : ""}`}>
                                    <strong>{answerState.title}</strong>
                                    <p>{answerState.body}</p>
                                </div>
                            ) : (
                                <>
                                    <div className="message-meta">
                                        <strong>法律咨询助手</strong>
                                        <span>今天 10:24</span>
                                    </div>
                                    {citations.length ? (
                                        <div className="answer-citation-nav">
                                            <span className="answer-citation-label">本回答主要依据</span>
                                            <div className="answer-citation-chips">
                                                {citations.slice(0, 4).map((citation, index) => (
                                                    <button
                                                        key={citation.articleId || `${citation.documentTitle}-${index}`}
                                                        type="button"
                                                        className={`answer-citation-chip ${activeCitationId === citation.articleId ? "active" : ""}`}
                                                        onClick={() => handleFocusCitation(citation)}
                                                        onMouseEnter={() => handleCitationHover(citation.articleId)}
                                                        onMouseLeave={handleCitationLeave}
                                                    >
                                                        依据 {index + 1}
                                                    </button>
                                                ))}
                                            </div>
                                        </div>
                                    ) : null}

                                    {answerSegments && answerSegments.length ? (
                                        <div className="answer-text">
                                            {answerSegments.map((segment, index) => {
                                                const isHighlighted = highlightedSegmentCitationId && segment.citationIds && segment.citationIds.includes(highlightedSegmentCitationId);
                                                return (
                                                    <div key={segment.id || index} className={`answer-segment ${isHighlighted ? "highlighted" : ""}`}>
                                                        <span>{segment.text}</span>
                                                        {segment.citationIds && segment.citationIds.length ? (
                                                            <span className="segment-citation-tags">
                                                                {segment.citationIds.map((cId) => {
                                                                    const citeIndex = citationIndexMap.get(cId);
                                                                    const citation = citations.find(c => c.articleId === cId);
                                                                    if (!citeIndex || !citation) return null;
                                                                    return (
                                                                        <button
                                                                            key={cId}
                                                                            type="button"
                                                                            className="segment-citation-tag"
                                                                            onClick={() => handleFocusCitation(citation)}
                                                                            onMouseEnter={() => handleCitationHover(cId)}
                                                                            onMouseLeave={handleCitationLeave}
                                                                        >
                                                                            [依据{citeIndex}]
                                                                        </button>
                                                                    );
                                                                })}
                                                            </span>
                                                        ) : null}
                                                    </div>
                                                );
                                            })}
                                        </div>
                                    ) : (
                                        <div className="answer-text">{answer}</div>
                                    )}

                                    <div className="notice">以上内容由 AI 生成，仅供参考，不构成法律意见。</div>
                                    <div className="assistant-actions">
                                        {answerActions.map((action) => (
                                            <button key={action} type="button" className="assistant-action-button">
                                                {action}
                                            </button>
                                        ))}
                                        <span className="assistant-status">已完成思考</span>
                                    </div>
                                </>
                            )}
                        </div>
                    </article>
                </section>

                <section className="composer-panel">
                    <form className="composer-form" onSubmit={handleSubmit}>
                        <div className="composer-label">输入你的法律问题，或粘贴合同/文书片段...</div>
                        <textarea
                            id="questionInput"
                            value={query}
                            onChange={(event) => setQuery(event.target.value)}
                            placeholder="例如：公司拖欠工资怎么办？我应该准备哪些证据？"
                            required
                        />
                        <div className="composer-footer">
                            <div className="composer-actions">
                                <button className="secondary-button" type="button">
                                    上传文件
                                </button>
                                <button className="secondary-button" type="button">
                                    粘贴
                                </button>
                                {examples.map((example) => (
                                    <button key={example} className="ghost-chip" type="button" onClick={() => setQuery(example)}>
                                        {example}
                                    </button>
                                ))}
                            </div>
                            <div className="composer-submit">
                                <span>{query.length} / 2000</span>
                                <button className="primary-button" type="submit" disabled={loading}>
                                    {loading ? "发送中..." : "发送"}
                                </button>
                            </div>
                        </div>
                    </form>
                </section>
            </section>

            <aside className="reference-panel">
                <div className="panel-heading reference-header">
                    <div>
                        <p className="eyebrow">依据</p>
                        <h2>引用法条</h2>
                    </div>
                    <span className="reference-count">共 {citations.length} 条</span>
                </div>
                <div className="citation-list-react">
                    {!citations.length ? (
                        <div className="empty-state compact">
                            <strong>暂无引用</strong>
                            <p>生成回答后会在这里显示相关法条和摘录。</p>
                        </div>
                    ) : (
                        visibleCitations.map((citation, index) => {
                            const detailEntry = detailState[citation.articleId] || {};
                            const expanded = openedId === citation.articleId;
                            return (
                                <article
                                    key={citation.articleId || `${citation.documentTitle}-${index}`}
                                    className={`citation-card ${activeCitationId === citation.articleId ? "active" : ""} ${highlightedSegmentCitationId === citation.articleId ? "highlighted" : ""}`}
                                    onMouseEnter={() => handleCitationHover(citation.articleId)}
                                    onMouseLeave={handleCitationLeave}
                                >
                                    <div className="citation-chip-row">
                                        <span className="citation-chip">依据 {index + 1}</span>
                                        <span className="status-chip">现行有效</span>
                                    </div>
                                    <div className="citation-title stacked">
                                        <strong>{citation.documentTitle || "未知法规"}</strong>
                                        <span>{citation.articleNo || ""}</span>
                                    </div>
                                    <p className="citation-preview condensed">{citation.quotedText || citation.preview || "暂无摘录"}</p>
                                    <div className="citation-meta-list">
                                        <p><span>来源：</span>全国人民代表大会常务委员会</p>
                                        <p><span>生效状态：</span>现行有效</p>
                                    </div>
                                    <button
                                        type="button"
                                        className="detail-link-button"
                                        onClick={() => handleToggleCitation(citation)}
                                        disabled={!citation.articleId}
                                    >
                                        {expanded ? "收起完整条文" : "查看完整条文"}
                                    </button>
                                    {expanded ? (
                                        <div className="citation-detail-box">
                                            {detailEntry.loading ? (
                                                <p className="citation-detail-placeholder">正在加载完整条文...</p>
                                            ) : detailEntry.error ? (
                                                <p className="citation-detail-error">完整条文加载失败：{detailEntry.error}</p>
                                            ) : (
                                                <>
                                                    <p className="citation-detail-path">
                                                        {detailEntry.detail?.fullPath || "暂无章节路径"}
                                                    </p>
                                                    <p className="citation-detail-text">
                                                        {detailEntry.detail?.content || "暂无完整条文"}
                                                    </p>
                                                </>
                                            )}
                                        </div>
                                    ) : null}
                                </article>
                            );
                        })
                    )}

                    {hiddenCitationCount > 0 ? (
                        <button
                            type="button"
                            className="show-more-citations-button"
                            onClick={() => setShowAllCitations(true)}
                        >
                            查看其余 {hiddenCitationCount} 条依据
                        </button>
                    ) : null}

                    {showAllCitations && citations.length > 3 ? (
                        <button
                            type="button"
                            className="show-more-citations-button secondary"
                            onClick={() => setShowAllCitations(false)}
                        >
                            收起扩展依据
                        </button>
                    ) : null}

                    {answer && !loading ? (
                        <>
                            <section className="info-card warning-card">
                                <div className="info-card-title">待确认事实</div>
                                <ul>
                                    <li>是否存在拖欠工资的事实？具体金额和期间？</li>
                                    <li>是否已与公司沟通？公司答复如何？</li>
                                    <li>是否已经向劳动监察部门投诉或申请仲裁？</li>
                                </ul>
                            </section>
                            <section className="info-card risk-card">
                                <div className="info-card-title">风险提示</div>
                                <p>仲裁时效一般为自知道或应当知道权利被侵害之日起一年。建议及时收集证据并依法维权。</p>
                            </section>
                        </>
                    ) : null}
                </div>
            </aside>
        </div>
    );
}
