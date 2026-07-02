import React from "react";
import { useMemo, useState } from "react";
import { Bot, Plus, RotateCcw, Search, Sparkles } from "lucide-react";
import {
    Conversation,
    ConversationContent,
    ConversationEmptyState,
    ConversationScrollButton
} from "@/components/ai-elements/conversation";
import {
    Message,
    MessageAction,
    MessageActions,
    MessageContent,
    MessageResponse
} from "@/components/ai-elements/message";
import {
    Reasoning,
    ReasoningContent,
    ReasoningTrigger
} from "@/components/ai-elements/reasoning";
import {
    PromptInput,
    PromptInputBody,
    PromptInputFooter,
    PromptInputSubmit,
    PromptInputTextarea,
    PromptInputTools
} from "@/components/ai-elements/prompt-input";
import {
    Sources,
    SourcesContent,
    SourcesTrigger
} from "@/components/ai-elements/sources";
import {
    Tool,
    ToolContent,
    ToolHeader,
    ToolInput,
    ToolOutput
} from "@/components/ai-elements/tool";
import { Button } from "@/components/ui/button";
import { useAgUiConsultation } from "./useAgUiConsultation";

const examples = [
    "房东不退押金怎么办",
    "劳动仲裁前要准备哪些证据",
    "借款合同逾期利息怎么算",
    "股东退出公司有哪些法律路径",
    "交通事故赔偿项目怎么主张",
    "公司拖欠工资怎么办"
];

function formatToolName(toolCallName) {
    if (!toolCallName) {
        return "工具调用";
    }

    const labels = {
        searchLawArticles: "检索法规",
        getArticleDetail: "查询法条详情",
        validateCitations: "校验引用"
    };

    return labels[toolCallName] || toolCallName;
}

export default function ConsultationPage({ onNavigate }) {
    const [query, setQuery] = useState("别人欠钱不还怎么办");
    const [submittedQuery, setSubmittedQuery] = useState("");
    const [openedId, setOpenedId] = useState("");
    const [activeCitationId, setActiveCitationId] = useState("");
    const [highlightedSegmentCitationId, setHighlightedSegmentCitationId] = useState("");
    const [showAllCitations, setShowAllCitations] = useState(false);
    const [detailState, setDetailState] = useState({});
    const { loading, answer, reasoning, toolCalls, citations, error, statusText, submitQuestion, loadCitationDetail, reset } = useAgUiConsultation();
    const hasStartedConversation = Boolean(submittedQuery || answer || loading || error);
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
    const citedCitations = citations;

    const conversationMessages = useMemo(() => {
        const messages = [];
        if (submittedQuery) {
            messages.push({
                id: "user-latest",
                role: "user",
                content: submittedQuery
            });
        }
        if (answer) {
            messages.push({
                id: "assistant-latest",
                role: "assistant",
                content: answer
            });
        } else if (loading) {
            messages.push({
                id: "assistant-loading",
                role: "assistant",
                content: statusText || "正在生成回答..."
            });
        }
        return messages;
    }, [answer, loading, statusText, submittedQuery]);

    const answerState = useMemo(() => {
        if (error) {
            return { title: "请求失败", body: error, error: true };
        }
        if (!answer && loading) {
            return { title: "正在检索法规依据", body: statusText || query };
        }
        if (!answer) {
            return { title: "等待提问", body: "回答会显示在这里，并在右侧同步列出引用依据。" };
        }
        return null;
    }, [answer, error, loading, query, statusText]);

    const hasToolActivity = Array.isArray(toolCalls) && toolCalls.length > 0;
    const hasAssistantRichContent = Boolean(
        reasoning || hasToolActivity || answer
    );

    async function handleSubmitInput(message) {
        const trimmed = (message.text || "").trim();
        if (!trimmed) {
            return;
        }
        setShowAllCitations(false);
        setActiveCitationId("");
        setHighlightedSegmentCitationId("");
        setSubmittedQuery(trimmed);
        setOpenedId("");
        setDetailState({});
        setQuery("");
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
        <div className={hasStartedConversation ? "consultation-layout consultation-layout-chat" : "consultation-home-layout"}>
            <section className={`conversation-shell ${hasStartedConversation ? "conversation-shell-chat" : "conversation-shell-home"}`}>
                {!hasStartedConversation ? (
                    <div className="consultation-home-stage">
                        <section className="consultation-home-hero">
                            <div className="home-hero-badge">
                                <Sparkles size={14} />
                                法律咨询
                            </div>
                            <h1>有什么我能帮你的吗？</h1>
                            <p>先提一个法律问题，或从下方推荐问题开始。输入后进入消息对话界面，并同步展示法条依据。</p>
                            <div className="home-suggestion-cloud">
                                {examples.map((example) => (
                                    <button key={example} type="button" className="home-suggestion-chip" onClick={() => setQuery(example)}>
                                        {example}
                                    </button>
                                ))}
                            </div>
                        </section>

                        <section className="composer-panel composer-panel-home">
                            <div className="composer-label">输入案情、争议点，或直接粘贴合同/文书片段</div>
                            <PromptInput className="composer-form agui-prompt-input" onSubmit={handleSubmitInput}>
                                <PromptInputBody>
                                    <PromptInputTextarea
                                        value={query}
                                        onChange={(event) => setQuery(event.target.value)}
                                        placeholder="例如：公司拖欠工资怎么办？我应该准备哪些证据？"
                                    />
                                </PromptInputBody>
                                <PromptInputFooter>
                                    <PromptInputTools className="agui-prompt-tools consultation-home-tools">
                                        <button type="button" className="home-tool-pill" onClick={() => onNavigate?.("search")}>
                                            <Search size={15} />
                                            专业检索
                                        </button>
                                        <button type="button" className="home-tool-pill" onClick={handleReset}>
                                            <Plus size={15} />
                                            新对话
                                        </button>
                                    </PromptInputTools>
                                    <div className="composer-submit">
                                        <span>{query.length} / 2000</span>
                                        <PromptInputSubmit disabled={!query.trim()} status={loading ? "streaming" : "ready"} />
                                    </div>
                                </PromptInputFooter>
                            </PromptInput>
                        </section>
                    </div>
                ) : (
                    <>
                        <section className="conversation-card">
                            <div className="agui-chat-shell">
                                <Conversation className="agui-conversation">
                                    <ConversationContent className="agui-conversation-content">
                                        {conversationMessages.length === 0 && answerState ? (
                                            <ConversationEmptyState
                                                className="consultation-empty-state"
                                                title={answerState.title}
                                                description={answerState.body}
                                                icon={<Bot size={28} />}
                                            />
                                        ) : (
                                            conversationMessages.map((message) => (
                                                <div key={message.id}>
                                                    <Message from={message.role}>
                                                        <MessageContent className={message.role === "assistant" ? "assistant-message-content" : ""}>
                                                            {message.role === "assistant" && hasAssistantRichContent ? (
                                                                <>
                                                                    {reasoning ? (
                                                                        <Reasoning
                                                                            autoOpenOnStreaming={false}
                                                                            className="consultation-reasoning"
                                                                            defaultOpen={false}
                                                                            isStreaming={loading && !answer}
                                                                        >
                                                                            <ReasoningTrigger />
                                                                            <ReasoningContent>{reasoning}</ReasoningContent>
                                                                        </Reasoning>
                                                                    ) : null}
                                                                    {hasToolActivity ? (
                                                                        <div className="tool-call-stack">
                                                                            {toolCalls.map((toolCall) => (
                                                                                <Tool
                                                                                    key={toolCall.id}
                                                                                    className="consultation-tool"
                                                                                    defaultOpen={false}
                                                                                >
                                                                                    <ToolHeader
                                                                                        title={formatToolName(toolCall.toolCallName)}
                                                                                        toolName={toolCall.toolCallName}
                                                                                        type={`tool-${toolCall.toolCallName || "call"}`}
                                                                                        state={toolCall.state}
                                                                                    />
                                                                                    <ToolContent>
                                                                                        <ToolInput input={toolCall.input} />
                                                                                        <ToolOutput
                                                                                            errorText={toolCall.errorText}
                                                                                            output={toolCall.output}
                                                                                        />
                                                                                    </ToolContent>
                                                                                </Tool>
                                                                            ))}
                                                                        </div>
                                                                    ) : null}
                                                                    {answer ? (
                                                                        <>
                                                                            <MessageResponse className="answer-markdown">{answer}</MessageResponse>
                                                                            {citedCitations.length ? (
                                                                                <div className="answer-citation-nav">
                                                                                    <div className="answer-citation-label">本回答引用依据</div>
                                                                                    <div className="answer-citation-chips">
                                                                                        {citedCitations.map((citation) => (
                                                                                            <button
                                                                                                key={citation.articleId}
                                                                                                type="button"
                                                                                                className={`answer-citation-chip ${activeCitationId === citation.articleId ? "active" : ""}`}
                                                                                                onClick={() => handleFocusCitation(citation)}
                                                                                                onMouseEnter={() => handleCitationHover(citation.articleId)}
                                                                                                onMouseLeave={handleCitationLeave}
                                                                                            >
                                                                                                依据 {citationIndexMap.get(citation.articleId) || "?"}
                                                                                            </button>
                                                                                        ))}
                                                                                    </div>
                                                                                </div>
                                                                            ) : null}
                                                                        </>
                                                                    ) : null}
                                                                </>
                                                            ) : (
                                                                <MessageResponse>{message.content}</MessageResponse>
                                                            )}
                                                            {message.role === "assistant" ? (
                                                                <MessageActions className="assistant-message-actions">
                                                                    {!loading ? (
                                                                        <MessageAction
                                                                            label="新对话"
                                                                            tooltip="新对话"
                                                                            onClick={handleReset}
                                                                        >
                                                                            <RotateCcw size={14} />
                                                                        </MessageAction>
                                                                    ) : null}
                                                                </MessageActions>
                                                            ) : null}
                                                        </MessageContent>
                                                    </Message>
                                                </div>
                                            ))
                                        )}
                                    </ConversationContent>
                                    <ConversationScrollButton />
                                </Conversation>
                            </div>
                        </section>

                        <section className="composer-panel composer-panel-chat">
                            <div className="composer-label">继续追问、补充事实，或粘贴更多材料</div>
                            <PromptInput className="composer-form agui-prompt-input" onSubmit={handleSubmitInput}>
                                <PromptInputBody>
                                    <PromptInputTextarea
                                        value={query}
                                        onChange={(event) => setQuery(event.target.value)}
                                        placeholder="继续补充事实，例如：对方已经发了催告函，我是否必须先协商？"
                                    />
                                </PromptInputBody>
                                <PromptInputFooter>
                                    <PromptInputTools className="agui-prompt-tools">
                                        {examples.slice(0, 3).map((example) => (
                                            <Button key={example} type="button" variant="ghost" size="sm" onClick={() => setQuery(example)}>
                                                {example}
                                            </Button>
                                        ))}
                                    </PromptInputTools>
                                    <div className="composer-submit">
                                        <span>{query.length} / 2000</span>
                                        <PromptInputSubmit disabled={!query.trim()} status={loading ? "streaming" : "ready"} />
                                    </div>
                                </PromptInputFooter>
                            </PromptInput>
                        </section>
                    </>
                )}
            </section>

            {hasStartedConversation ? (
            <aside className="reference-panel">
                <div className="citation-list-react">
                    {!citations.length ? (
                        <div className="empty-state compact">
                            <strong>暂无引用</strong>
                            <p>生成回答后会在这里显示相关法条和摘录。</p>
                        </div>
                    ) : (
                        <Sources className="reference-sources" defaultOpen>
                            <SourcesTrigger className="reference-sources-trigger" count={citations.length}>
                                <div className="panel-heading reference-header">
                                    <div>
                                        <p className="eyebrow">依据</p>
                                        <h2>引用法条</h2>
                                    </div>
                                    <span className="reference-count">共 {citations.length} 条</span>
                                </div>
                            </SourcesTrigger>
                            <SourcesContent className="reference-sources-content">
                                {visibleCitations.map((citation, index) => {
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
                                })}

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
                            </SourcesContent>
                        </Sources>
                    )}

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
            ) : null}
        </div>
    );
}
