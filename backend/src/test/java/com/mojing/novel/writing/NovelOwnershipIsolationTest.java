package com.mojing.novel.writing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class NovelOwnershipIsolationTest {
    @Autowired MockMvc mockMvc;
    @Autowired NovelAccessService novelAccessService;

    @Test
    void anotherUserCannotListOrOpenTheNovelOrItsChapter() throws Exception {
        String novelJson = mockMvc.perform(post("/api/novels").with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"隔离测试小说\",\"description\":\"仅用户1可见\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long novelId = Long.parseLong(novelJson.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));

        String chapterJson = mockMvc.perform(post("/api/novels/{novelId}/chapters", novelId).with(user("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"第一章\",\"content\":\"只属于用户1的正文\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long chapterId = Long.parseLong(chapterJson.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/novels").with(user("2")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/novels/{id}", novelId).with(user("2")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/chapters/{id}", chapterId).with(user("2")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/novels/{id}", novelId).with(user("1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("隔离测试小说"));
        mockMvc.perform(get("/api/chapters/{id}", chapterId).with(user("1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("第一章"));
    }

    @Test
    void everyChildResourceLookupUsesAnOwnershipJoin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", "", List.of()));
        try {
            List<Runnable> checks = List.of(
                    () -> novelAccessService.requireRun(999_999),
                    () -> novelAccessService.requireDirectorMessage(999_999),
                    () -> novelAccessService.requireRevision(999_999),
                    () -> novelAccessService.requireConsistencyIssue(999_999),
                    () -> novelAccessService.requireChapterMemory(999_999),
                    () -> novelAccessService.requireEvalCase(999_999),
                    () -> novelAccessService.requireStoryBibleMessage(999_999),
                    () -> novelAccessService.requireCharacter(999_999),
                    () -> novelAccessService.requireOrganization(999_999),
                    () -> novelAccessService.requireWorldSetting(999_999),
                    () -> novelAccessService.requireStoryVolume(999_999),
                    () -> novelAccessService.requireStoryPart(999_999));
            checks.forEach(check -> assertThatThrownBy(check::run)
                    .isInstanceOf(WritingNotFoundException.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
