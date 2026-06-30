package com.law4x.rag.interfaces.rest;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RagSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void expandsDebtQuestionAgainstLocalLawDatabase() throws Exception {
        mockMvc.perform(get("/api/rag/search")
                        .param("query", "别人欠钱不还怎么办")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.items[0].matchType").value("keyword_expansion"));
    }
}
