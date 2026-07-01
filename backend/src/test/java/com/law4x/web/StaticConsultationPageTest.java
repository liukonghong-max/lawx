package com.law4x.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "law4x.embedding.dashscope.enabled=false",
        "law4x.agentscope.enabled=false"
})
@AutoConfigureMockMvc
class StaticConsultationPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesPublicConsultationPage() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>law4x")))
                .andExpect(content().string(containsString("pageTitle")))
                .andExpect(content().string(containsString("searchWorkspace")))
                .andExpect(content().string(containsString("selectedEvidencePanel")))
                .andExpect(content().string(containsString("questionInput")))
                .andExpect(content().string(containsString("answerPanel")))
                .andExpect(content().string(containsString("citationPanel")));
    }

    @Test
    void servesAppScriptUsingCitationQuotedText() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("citation.quotedText")))
                .andExpect(content().string(containsString("/api/law/articles/${citation.articleId}")))
                .andExpect(content().string(containsString("/api/law/articles/search?")))
                .andExpect(content().string(containsString("/api/rag/search?")))
                .andExpect(content().string(containsString("导出 Markdown")))
                .andExpect(content().string(containsString("生成法律依据段落")))
                .andExpect(content().string(containsString("完整条文加载失败")))
                .andExpect(content().string(containsString("暂无摘录")));
    }
}
