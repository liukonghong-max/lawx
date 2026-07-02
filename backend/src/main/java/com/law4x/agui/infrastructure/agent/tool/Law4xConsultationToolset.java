package com.law4x.agui.infrastructure.agent.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Law4xConsultationToolset {

    private final Law4xAgUiToolset delegate;

    public Law4xConsultationToolset(Law4xAgUiToolset delegate) {
        this.delegate = delegate;
    }

    @Tool(
            name = "getArticleDetail",
            description = "根据 articleId 获取完整法条正文、章节路径、来源链接和生效状态。",
            readOnly = true
    )
    public Law4xAgUiToolset.GetArticleDetailResult getArticleDetail(
            @ToolParam(name = "articleId", description = "法条 articleId，必须是 UUID") String articleId
    ) {
        return delegate.getArticleDetail(articleId);
    }

    @Tool(
            name = "validateCitations",
            description = "校验回答中的引用是否来自允许的 articleId 列表，避免编造引用。",
            readOnly = true
    )
    public Law4xAgUiToolset.ValidateCitationsResult validateCitations(
            @ToolParam(name = "citationIds", description = "回答中实际引用的 articleId 列表") List<String> citationIds,
            @ToolParam(name = "allowedCitationIds", description = "服务端预检索后允许引用的 articleId 列表") List<String> allowedCitationIds
    ) {
        return delegate.validateCitations(citationIds, allowedCitationIds);
    }
}
