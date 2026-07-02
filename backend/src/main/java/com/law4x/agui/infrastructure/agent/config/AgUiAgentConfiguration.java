package com.law4x.agui.infrastructure.agent.config;

import com.law4x.agui.infrastructure.agent.middleware.AgUiGroundingPromptMiddleware;
import com.law4x.agui.infrastructure.agent.tool.Law4xConsultationToolset;
import com.law4x.rag.infrastructure.agent.Law4xAgentScopeProperties;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Law4xAgentScopeProperties.class)
public class AgUiAgentConfiguration {

    private static final String LEGAL_CONSULTATION_SYSTEM_PROMPT = """
            你是 law4x 法律法规咨询助手。
            你的职责不是自行检索，而是基于系统已经提供的法规证据完成法律分析与回答。
            你的回答必须基于真实法规依据，不能编造来源，不能脱离证据直接给出法律建议。

            系统会在服务端先完成本轮法规检索，并把候选法条和 allowedCitationIds 注入上下文。
            你必须把这份服务端检索结果当作本轮回答的主依据来源，不要自行决定是否需要重新检索。

            对于每一个法律咨询问题，必须严格遵守以下流程：
            1. 先阅读系统提供的候选法条与 allowedCitationIds。
            2. 先判断候选法条是否足以支撑回答；如摘要不足，可调用 getArticleDetail 查看完整条文。
            3. 基于候选法条和补充条文形成回答草稿，并且只允许引用 allowedCitationIds 中的 articleId。
            4. 输出最终回答前，必须调用 validateCitations，校验回答中的引用是否合法。
            5. 只有在 validateCitations 通过后，才能输出最终回答。

            强约束：
            - 不得调用 searchLawArticles。
            - 要把自己视为“基于已检索证据作答的助手”，而不是“自行决定检索策略的助手”。
            - 没有读完系统提供的候选法条前，不得直接给出法律建议、结论或处置方案。
            - 没有完成 validateCitations 前，不得输出带法规依据的最终回答。
            - 如果检索结果不足，应明确说明信息不足，并提示用户补充事实，而不是编造法条。
            - 如用户问题涉及事实认定、程序节点、金额计算、主体身份等关键信息不足，应先指出缺失事实，再在现有法条范围内给出有限度建议。
            - 优先引用现行有效、与争议点最直接相关的条文。
            """;

    @Bean
    HarnessAgent law4xAgUiAgent(
            Law4xAgentScopeProperties properties,
            Law4xConsultationToolset law4xConsultationToolset
    ) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalArgumentException("law4x.agentscope.api-key must be configured");
        }
        Model model = createModel(properties);
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(law4xConsultationToolset);
        return HarnessAgent.builder()
                .name(properties.getAgentName())
                .sysPrompt(LEGAL_CONSULTATION_SYSTEM_PROMPT)
                .model(model)
                .toolkit(toolkit)
                .middleware(new AgUiGroundingPromptMiddleware())
                .workspace(Paths.get(properties.getWorkspace()))
                .build();
    }

    private Model createModel(Law4xAgentScopeProperties properties) {
        String provider = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        if ("dashscope".equals(provider)) {
            DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                    .apiKey(properties.getApiKey())
                    .modelName(properties.getModelName())
                    .stream(true)
                    .enableSearch(false);
            if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
                builder.baseUrl(properties.getBaseUrl());
            }
            return builder.build();
        }
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .stream(true);
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return builder.build();
    }
}
