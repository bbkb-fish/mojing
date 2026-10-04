package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@WithMockUser(username = "1")
class NovelAgentControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void projectPlanningGeneratesDraftAndOnlyUpdatesNovelAfterSave() throws Exception {
        JsonNode novel = postJson("/api/novels", """
                {"title":"立项工作台测试","description":"旧简介","outline":"旧大纲"}
                """);
        long novelId = novel.path("id").asLong();
        postJson("/api/novels/" + novelId + "/chapters", """
                {"title":"第一章","content":"外卖员沈舟看见客户头顶只剩十分钟的死亡倒计时。"}
                """);

        JsonNode project = objectMapper.readTree(mockMvc.perform(get("/api/novels/{id}/project", novelId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.writtenThroughChapterNo").value(1))
                .andReturn().getResponse().getContentAsString());
        JsonNode generated = postJson("/api/novels/" + novelId + "/project/generate", """
                {"target":"DESCRIPTION","instruction":"突出能力代价",
                 "current":{"inspiration":"能看见死亡倒计时的外卖员","description":"","outline":"旧大纲"}}
                """);
        assertThat(generated.path("content").path("description").asText()).contains("死亡倒计时");
        mockMvc.perform(get("/api/novels/{id}", novelId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("旧简介"));

        var saveBody = objectMapper.createObjectNode();
        saveBody.set("content", generated.path("content"));
        saveBody.put("novelVersion", project.path("novelVersion").asLong());
        saveBody.putNull("profileVersion");
        JsonNode saved = putJson("/api/novels/" + novelId + "/project", objectMapper.writeValueAsString(saveBody));
        assertThat(saved.path("content").path("description").asText()).contains("死亡倒计时");
        mockMvc.perform(get("/api/novels/{id}", novelId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("能看见死亡倒计时的外卖员"));
    }

    @Test
    void storyBibleChatKeepsHistoryAndAppliesANewCharacterSuggestion() throws Exception {
        JsonNode novel = postJson("/api/novels", """
                {"title":"资料库顾问测试","description":"主角调查港口异象","outline":"新一卷需要一组立场复杂的反派"}
                """);
        long novelId = novel.path("id").asLong();

        JsonNode conversation = postJson("/api/novels/" + novelId + "/story-bible/chat/messages", """
                {"message":"请为新一卷设计一组有联系的反派人物"}
                """);
        assertThat(conversation.path("messages").size()).isEqualTo(2);
        JsonNode assistant = conversation.path("messages").get(1);
        assertThat(assistant.path("characterSuggestions").size()).isEqualTo(3);

        long messageId = assistant.path("id").asLong();
        JsonNode applied = postJson("/api/story-bible/chat/messages/" + messageId + "/suggestions/0/apply", "{}");
        assertThat(applied.path("character").path("name").asText()).isEqualTo("闻烬");
        assertThat(applied.path("conversation").path("messages").get(1)
                .path("appliedSuggestionIndexes").get(0).asInt()).isZero();

        mockMvc.perform(get("/api/novels/{id}/characters", novelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("闻烬"))
                .andExpect(jsonPath("$[0].role").value("本卷主要反派"));

        JsonNode continued = postJson("/api/novels/" + novelId + "/story-bible/chat/messages", """
                {"message":"继续分析他和主角之间的价值观冲突"}
                """);
        assertThat(continued.path("messages").size()).isEqualTo(4);
    }

    @Test
    void storyVolumeCanAnalyzeWrittenChaptersAndDetectStaleFacts() throws Exception {
        JsonNode novel = postJson("/api/novels", """
                {"title":"分卷测试","description":"主角调查旧城异象","outline":"第一卷让主角发现幕后组织存在"}
                """);
        long novelId = novel.path("id").asLong();
        JsonNode chapter = postJson("/api/novels/" + novelId + "/chapters", """
                {"title":"旧城来客","content":"沈舟在旧城车站发现了守夜人的黑色徽记。"}
                """);
        long chapterId = chapter.path("id").asLong();

        JsonNode generated = postJson("/api/novels/" + novelId + "/story-volumes/generate", """
                {"volumeNo":1,"title":"旧城迷雾","chapterStart":1,"chapterEnd":30,
                 "instruction":"卷末只确认幕后组织存在，不揭晓首领",
                 "currentContent":{"title":"旧城迷雾","objective":"","retrospective":"","futurePlan":"",
                 "keyTurningPoints":"","climax":"","endingHook":"","foreshadows":"","lockedBeats":"卷末不能揭晓首领"}}
                """);
        assertThat(generated.path("analyzedThroughChapterNo").asInt()).isEqualTo(1);
        assertThat(generated.path("content").path("retrospective").asText()).contains("第1章");

        JsonNode saved = postJson("/api/novels/" + novelId + "/story-volumes", """
                {"volumeNo":1,"title":"旧城迷雾","chapterStart":1,"chapterEnd":30,
                 "objective":"发现幕后组织存在","retrospective":"沈舟发现守夜人徽记",
                 "futurePlan":"继续调查徽记来源","keyTurningPoints":"证人失踪","climax":"旧城追逐",
                 "endingHook":"确认幕后组织存在","foreshadows":"导师的旧照片","lockedBeats":"卷末不能揭晓首领",
                 "analyzedThroughChapterNo":1,"status":"CONFIRMED"}
                """);
        assertThat(saved.path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(saved.path("stale").asBoolean()).isFalse();

        putJson("/api/chapters/" + chapterId, """
                {"title":"旧城来客","content":"沈舟在旧城车站发现徽记，并认出了导师留下的暗号。","version":%d}
                """.formatted(chapter.path("version").asLong()));
        mockMvc.perform(get("/api/novels/{id}/story-volumes", novelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stale").value(true));
    }

    @Test
    void organizationStoryBibleSupportsCrud() throws Exception {
        JsonNode novel = postJson("/api/novels", """
                {"title":"组织资料测试","description":"","outline":""}
                """);
        long novelId = novel.path("id").asLong();
        JsonNode created = postJson("/api/novels/" + novelId + "/organizations", """
                {"name":"玄天剑宗","aliases":"玄天宗，剑宗","type":"宗门","description":"北境剑修宗门","goal":"寻找失落剑谱","structure":"宗主、三峰长老","relationships":"与四海商会合作"}
                """);
        long organizationId = created.path("id").asLong();

        mockMvc.perform(get("/api/novels/{id}/organizations", novelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("玄天剑宗"))
                .andExpect(jsonPath("$[0].aliases").value("玄天宗，剑宗"));

        mockMvc.perform(put("/api/organizations/{id}", organizationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"玄天剑宗","aliases":"玄天宗，剑宗","type":"宗门","description":"北境剑修宗门","goal":"夺回失落剑谱","structure":"宗主、三峰长老","relationships":"与四海商会合作","version":%d}
                                """.formatted(created.path("version").asLong())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal").value("夺回失落剑谱"));

        mockMvc.perform(delete("/api/organizations/{id}", organizationId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/novels/{id}/organizations", novelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void chapterDirectorKeepsAMultiTurnConversationAndCurrentPlan() throws Exception {
        JsonNode novel = postJson("/api/novels", """
                {"title":"多轮导演测试","description":"","outline":"主角追查黑衣人"}
                """);
        long novelId = novel.path("id").asLong();
        JsonNode chapter = postJson("/api/novels/" + novelId + "/chapters", """
                {"title":"追踪","content":"哈基米在河边发现一枚陌生令牌。"}
                """);
        long chapterId = chapter.path("id").asLong();

        JsonNode first = postJson("/api/agent/novels/" + novelId + "/chapters/" + chapterId + "/director/messages",
                "{\"message\":\"让主角追查令牌来源，但不要揭露黑衣人身份\"}");
        assertThat(first.path("messages").size()).isEqualTo(2);
        assertThat(first.path("currentPlan").path("objective").asText()).contains("不要揭露黑衣人身份");

        var secondBody = objectMapper.createObjectNode();
        secondBody.put("message", "保留身份悬念，把高潮改成主角遭到伏击");
        secondBody.set("currentPlan", first.path("currentPlan"));
        JsonNode second = postJson("/api/agent/novels/" + novelId + "/chapters/" + chapterId + "/director/messages",
                objectMapper.writeValueAsString(secondBody));
        assertThat(second.path("messages").size()).isEqualTo(4);
        assertThat(second.path("messages").path(2).path("role").asText()).isEqualTo("USER");
        assertThat(second.path("messages").path(3).path("changeSummary").isArray()).isTrue();
        assertThat(second.path("currentPlan").path("objective").asText()).contains("遭到伏击");
        assertThat(second.path("currentRunId").asLong()).isPositive();

        mockMvc.perform(get("/api/agent/chapters/{id}/director", chapterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(4))
                .andExpect(jsonPath("$.currentPlan.objective").value("保留身份悬念，把高潮改成主角遭到伏击"));
    }

    @Test
    void storyBiblePlanAndDraftFormACompleteHumanApprovalFlow() throws Exception {
        JsonNode novel = postJson("/api/novels", """
                {"title":"测试故事","description":"一场寻找真相的冒险","outline":"主角必须守住秘密"}
                """);
        long novelId = novel.path("id").asLong();
        JsonNode chapter = postJson("/api/novels/" + novelId + "/chapters", """
                {"title":"第一章","content":"门外响起三声敲门声，他没有立刻开门。"}
                """);
        long chapterId = chapter.path("id").asLong();

        mockMvc.perform(post("/api/novels/{id}/characters", novelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"沈舟\",\"role\":\"主角\",\"description\":\"调查员\",\"personality\":\"谨慎\",\"goal\":\"查明真相\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("沈舟"));

        JsonNode run = postJson("/api/agent/novels/" + novelId + "/chapters/" + chapterId + "/plan",
                "{\"guidance\":\"门外的人带来一条危险线索\"}");
        assertThat(run.path("status").asText()).isEqualTo("WAITING_APPROVAL");
        assertThat(run.path("plan").path("developments").isArray()).isTrue();
        assertThat(run.path("steps").size()).isEqualTo(2);

        long runId = run.path("id").asLong();
        var draftBody = objectMapper.createObjectNode();
        draftBody.set("plan", run.path("plan"));
        draftBody.put("targetLength", 600);
        String draftRequest = objectMapper.writeValueAsString(draftBody);
        mockMvc.perform(post("/api/agent/runs/{id}/draft", runId)
                        .contentType(MediaType.APPLICATION_JSON).content(draftRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.draft").isNotEmpty())
                .andExpect(jsonPath("$.steps.length()").value(3))
                .andExpect(jsonPath("$.operation").value("CHAPTER_WRITING"))
                .andExpect(jsonPath("$.calls.length()").value(2))
                .andExpect(jsonPath("$.calls[0].model").value("demo"));

        postJson("/api/ai/completion", """
                {"novelId":%d,"chapterId":%d,"cursorContext":"门外响起三声敲门声。","maxLength":120}
                """.formatted(novelId, chapterId));
        mockMvc.perform(get("/api/agent/novels/{id}/runs", novelId).param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operation").value("QUICK_COMPLETION"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].totalTokens").value(0));

        JsonNode revision = postJson("/api/agent/novels/" + novelId + "/chapters/" + chapterId + "/rewrite", """
                {
                  "selectedText":"门外响起三声敲门声",
                  "beforeContext":"",
                  "afterContext":"他没有立刻开门。",
                  "mode":"POLISH",
                  "instruction":"增强紧张感，不改变事实",
                  "startOffset":0,
                  "endOffset":9
                }
                """);
        assertThat(revision.path("status").asText()).isEqualTo("GENERATED");
        assertThat(revision.path("revisedText").asText()).isNotBlank();
        assertThat(revision.path("changeSummary").isArray()).isTrue();

        long revisionId = revision.path("id").asLong();
        mockMvc.perform(patch("/api/agent/revisions/{id}", revisionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"version\":" + revision.path("version").asLong() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get("/api/agent/chapters/{id}/revisions", chapterId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(revisionId));

        JsonNode memory = postJson("/api/agent/chapters/" + chapterId + "/memory/extract",
                "{\"instruction\":\"重点记录敲门人与危险线索\"}");
        assertThat(memory.path("status").asText()).isEqualTo("GENERATED");
        assertThat(memory.path("content").path("summary").asText()).isNotBlank();
        assertThat(memory.path("stale").asBoolean()).isFalse();

        var confirmMemoryBody = objectMapper.createObjectNode();
        confirmMemoryBody.set("content", memory.path("content"));
        confirmMemoryBody.put("version", memory.path("version").asLong());
        JsonNode confirmedMemory = putJson("/api/agent/chapter-memories/" + memory.path("id").asLong() + "/confirm",
                objectMapper.writeValueAsString(confirmMemoryBody));
        assertThat(confirmedMemory.path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmedMemory.path("stale").asBoolean()).isFalse();
        mockMvc.perform(get("/api/agent/novels/{id}/chapter-memories", novelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chapterId").value(chapterId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        JsonNode report = postJson("/api/agent/novels/" + novelId + "/chapters/" + chapterId + "/consistency-check",
                "{\"focus\":\"重点检查人物行为\"}");
        assertThat(report.path("score").asInt()).isBetween(0, 100);
        assertThat(report.path("issues").size()).isEqualTo(1);
        long issueId = report.path("issues").path(0).path("id").asLong();

        JsonNode fixedIssue = postJson("/api/agent/consistency-issues/" + issueId + "/fix", "{\"instruction\":\"保持紧张感\"}");
        assertThat(fixedIssue.path("status").asText()).isEqualTo("WAITING_APPROVAL");
        JsonNode fixRevision = fixedIssue.path("revision");
        mockMvc.perform(patch("/api/agent/revisions/{id}", fixRevision.path("id").asLong())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"version\":" + fixRevision.path("version").asLong() + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/agent/chapters/{id}/consistency-reports", chapterId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].issues[0].status").value("FIXED"));

        JsonNode changed = putJson("/api/chapters/" + chapterId, """
                {"title":"第一章","content":"门外响起三声敲门声，他终于打开了门。","version":%d}
                """.formatted(chapter.path("version").asLong()));
        assertThat(changed.path("contentVersion").asLong()).isEqualTo(1L);
        mockMvc.perform(get("/api/agent/chapters/{id}/memory", chapterId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stale").value(true));
    }

    private JsonNode postJson(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode putJson(String path, String body) throws Exception {
        String response = mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
