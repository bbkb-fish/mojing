package com.mojing.novel.writing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "1")
class WritingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsChapterSavePreflightFromViteDevServer() throws Exception {
        mockMvc.perform(options("/api/chapters/2")
                        .header("Origin", "http://127.0.0.1:5175")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String allowedOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
                    if (!"http://127.0.0.1:5175".equals(allowedOrigin)) {
                        throw new AssertionError("CORS origin was not allowed: " + allowedOrigin);
                    }
                });
    }

    @Test
    void createsAndUpdatesAChapter() throws Exception {
        String novelJson = mockMvc.perform(post("/api/novels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"测试小说","description":"持久化测试"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long novelId = extractLong(novelJson, "id");

        String chapterJson = mockMvc.perform(post("/api/novels/{novelId}/chapters", novelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"第一章","content":"雾从港口升起。"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wordCount").value(7))
                .andExpect(jsonPath("$.contentVersion").value(0))
                .andReturn().getResponse().getContentAsString();
        long chapterId = extractLong(chapterJson, "id");
        long version = extractLong(chapterJson, "version");

        mockMvc.perform(put("/api/chapters/{chapterId}", chapterId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"第一章 雾起","content":"雾从港口升起。灯塔熄灭了。","version":%d}
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("第一章 雾起"))
                .andExpect(jsonPath("$.wordCount").value(13))
                .andExpect(jsonPath("$.contentVersion").value(1));

        mockMvc.perform(get("/api/novels/{novelId}/chapters", novelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(chapterId));
    }

    private long extractLong(String json, String field) {
        String marker = "\"" + field + "\":";
        int start = json.indexOf(marker) + marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Long.parseLong(json.substring(start, end));
    }
}
