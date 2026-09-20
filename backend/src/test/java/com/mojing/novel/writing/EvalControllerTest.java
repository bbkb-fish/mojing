package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="mojing.ai.api-key=")
@AutoConfigureMockMvc
class EvalControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test void createsRunsAndKeepsComparableEvaluationHistory() throws Exception {
        JsonNode novel=postJson("/api/novels","{\"title\":\"评测小说\",\"description\":\"\",\"outline\":\"\"}");
        long novelId=novel.path("id").asLong();
        JsonNode chapter=postJson("/api/novels/"+novelId+"/chapters","{\"title\":\"第一章\",\"content\":\"他推开旧邮局的大门。\"}");
        JsonNode testCase=postJson("/api/evals/novels/"+novelId+"/cases","""
                {"chapterId":%d,"name":"禁止角色串名","inputContext":"他推开旧邮局的大门。",
                "instruction":"延续紧张气氛","requiredTerms":"脚步声","forbiddenTerms":"顾临，未完待续",
                "minLength":20,"maxLength":160,"enabled":true}
                """.formatted(chapter.path("id").asLong()));
        assertThat(testCase.path("latestRun").isNull()).isTrue();
        long caseId=testCase.path("id").asLong();

        mockMvc.perform(post("/api/evals/cases/{id}/runs",caseId).contentType(MediaType.APPLICATION_JSON).content("{\"useLlmJudge\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.ruleScore").isNumber()).andExpect(jsonPath("$.judgeScore").isNumber())
                .andExpect(jsonPath("$.overallScore").isNumber()).andExpect(jsonPath("$.generatorPromptVersion").value("quick-v1"))
                .andExpect(jsonPath("$.judgePromptVersion").value("novel-judge-v1"));
        mockMvc.perform(get("/api/evals/novels/{id}/cases",novelId)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latestRun.status").value("COMPLETED"));
        mockMvc.perform(get("/api/evals/cases/{id}/runs",caseId)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(post("/api/evals/novels/{id}/runs",novelId).contentType(MediaType.APPLICATION_JSON).content("{\"useLlmJudge\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/evals/cases/{id}",caseId)).andExpect(status().isNoContent());
    }

    private JsonNode postJson(String path,String body)throws Exception{
        String response=mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
