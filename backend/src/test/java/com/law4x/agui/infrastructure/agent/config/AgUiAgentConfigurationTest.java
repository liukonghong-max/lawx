package com.law4x.agui.infrastructure.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.law4x.agui.application.service.CitationValidationService;
import com.law4x.agui.infrastructure.agent.tool.Law4xAgUiToolset;
import com.law4x.agui.infrastructure.agent.tool.Law4xConsultationToolset;
import com.law4x.law.application.GetLawArticleDetailUseCase;
import com.law4x.law.application.SearchLawArticlesUseCase;
import io.agentscope.harness.agent.HarnessAgent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

class AgUiAgentConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AgUiAgentConfiguration.class, TestConfig.class);

    @Test
    void createsHarnessAgentBean() {
        contextRunner
                .withPropertyValues(
                        "law4x.agentscope.api-key=test-key",
                        "law4x.agentscope.provider=openai-compatible",
                        "law4x.agentscope.model-name=ark-code-latest",
                        "law4x.agentscope.workspace=.agentscope/workspace"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(HarnessAgent.class);
                    HarnessAgent agent = context.getBean(HarnessAgent.class);
                    assertThat(agent.getToolkit().getToolNames())
                            .contains("getArticleDetail", "validateCitations")
                            .doesNotContain("searchLawArticles");
                    assertThat(agent.getDelegate().getSysPrompt())
                            .contains("系统会在服务端先完成本轮法规检索")
                            .contains("不得调用 searchLawArticles")
                            .contains("输出最终回答前，必须调用 validateCitations")
                            .contains("基于已检索证据作答的助手")
                            .contains("最终回答必须使用 Markdown 格式输出")
                            .contains("allowedCitationIds");
                });
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        SearchLawArticlesUseCase searchLawArticlesUseCase() {
            return new SearchLawArticlesUseCase(null);
        }

        @Bean
        GetLawArticleDetailUseCase getLawArticleDetailUseCase() {
            return new GetLawArticleDetailUseCase(null);
        }

        @Bean
        CitationValidationService citationValidationService() {
            return new CitationValidationService();
        }

        @Bean
        Law4xAgUiToolset law4xAgUiToolset(
                SearchLawArticlesUseCase searchLawArticlesUseCase,
                GetLawArticleDetailUseCase getLawArticleDetailUseCase,
                CitationValidationService citationValidationService
        ) {
            return new Law4xAgUiToolset(
                    searchLawArticlesUseCase,
                    getLawArticleDetailUseCase,
                    citationValidationService
            );
        }

        @Bean
        Law4xConsultationToolset law4xConsultationToolset(Law4xAgUiToolset law4xAgUiToolset) {
            return new Law4xConsultationToolset(law4xAgUiToolset);
        }
    }
}
