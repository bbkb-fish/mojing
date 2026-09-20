package com.mojing.novel.completion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "mojing.ai.api-key=")
@AutoConfigureMockMvc
class NovelCompletionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void returnsDemoCompletionWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/ai/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cursorContext", "他推开旧邮局的地下室大门。",
                                "maxLength", 120))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completion").isNotEmpty())
                .andExpect(jsonPath("$.source").value("demo"));
    }

    @Test
    void rejectsBlankContext() throws Exception {
        mockMvc.perform(post("/api/ai/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cursorContext\":\"\",\"maxLength\":120}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("上下文不能为空"));
    }
}
