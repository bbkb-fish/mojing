package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class NovelAgentDirectorParserTest {
    @Test
    void acceptsCommonJsonFieldVariantsFromTheModel() {
        NovelAgentService service = mock(NovelAgentService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        AgentDtos.DirectorTurnOutput output = service.parseDirectorTurn("""
                ```json
                {
                  "message":"已按要求调整",
                  "changes":["保留悬念","加强冲突"],
                  "chapterPlan":{
                    "标题":"墓园惊魂",
                    "本章目标":"让主角意识到危险",
                    "开场":"主角从棺材爬出",
                    "情节推进":[{"content":"查看墓碑"},{"event":"寻找手机"},{"description":"遭遇食尸鬼"}],
                    "高潮":"左手被抓伤",
                    "结尾钩子":"主角逃入森林",
                    "出场人物":["哈基米","食尸鬼"],
                    "连续性约束":["第三人称"]
                  }
                }
                ```
                """, null);

        assertThat(output.reply()).isEqualTo("已按要求调整");
        assertThat(output.plan().developments()).containsExactly("查看墓碑", "寻找手机", "遭遇食尸鬼");
        assertThat(output.plan().endingHook()).isEqualTo("主角逃入森林");
        assertThat(new ObjectMapper().valueToTree(output.plan()).has("climax")).isFalse();
        assertThat(output.changeSummary()).containsExactly("保留悬念", "加强冲突");
    }
}
