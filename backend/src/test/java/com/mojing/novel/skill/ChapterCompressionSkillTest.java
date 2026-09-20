package com.mojing.novel.skill;

import com.mojing.novel.writing.AgentDtos.ChapterMemoryContent;
import com.mojing.novel.writing.AgentDtos.MemoryScene;
import com.mojing.novel.writing.AgentDtos.MemorySceneEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChapterCompressionSkillTest {
    @Test
    void mergesTimeAndLocationChangesInsideTheSameContinuousFight() {
        ChapterCompressionSkill skill = new ChapterCompressionSkill();
        MemoryScene morning = new MemoryScene(1, "山中交战", "HARD", "哈基米与对手持续交战",
                "上午", List.of("大山"), List.of("哈基米", "XXX"), "击败对方", "双方持续交战",
                List.of(new MemorySceneEvent("上午", "大山", "两人一路打入大山")), "战斗未结束");
        MemoryScene afternoon = new MemoryScene(2, "河边交战", "SOFT", "哈基米与对手持续交战",
                "下午", List.of("河边"), List.of("哈基米", "XXX"), "击败对方", "双方继续交战",
                List.of(new MemorySceneEvent("下午", "河边", "两人转移到河边继续战斗")), "战斗仍未结束");
        ChapterMemoryContent content = new ChapterMemoryContent("chapter-compression-v1", "两人持续交战",
                "上午至下午", List.of("大山", "河边"), List.of(), List.of(morning, afternoon),
                List.of(), List.of(), List.of(), List.of(), "战斗推动双方矛盾升级", "MEDIUM");

        ChapterMemoryContent normalized = skill.normalize(content);

        assertThat(normalized.scenes()).hasSize(1);
        assertThat(normalized.scenes().getFirst().locations()).containsExactly("大山", "河边");
        assertThat(normalized.scenes().getFirst().subEvents()).hasSize(2);
        assertThat(normalized.scenes().getFirst().timeSpan()).contains("上午", "下午");
    }

    @Test
    void keepsHardBoundaryAsANewScene() {
        ChapterCompressionSkill skill = new ChapterCompressionSkill();
        MemoryScene fight = new MemoryScene(1, "战斗", "HARD", "战斗", "上午", List.of("大山"),
                List.of("哈基米"), "获胜", "交战", List.of(), "击败敌人");
        MemoryScene investigation = new MemoryScene(2, "调查", "HARD", "调查失踪案", "三天后", List.of("皇宫"),
                List.of("哈基米"), "调查真相", "受到阻挠", List.of(), "找到线索");
        ChapterMemoryContent content = new ChapterMemoryContent("chapter-compression-v1", "战斗后开始调查",
                "", List.of(), List.of(), List.of(fight, investigation), List.of(), List.of(), List.of(), List.of(), "", "HIGH");

        assertThat(skill.normalize(content).scenes()).hasSize(2);
    }
}
