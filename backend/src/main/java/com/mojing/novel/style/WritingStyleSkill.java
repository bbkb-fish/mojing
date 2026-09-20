package com.mojing.novel.style;

import org.springframework.stereotype.Component;

@Component
public class WritingStyleSkill {
    public static final String VERSION = "writing-style-v1";

    public String compose(String selectedRules, String forbiddenWords) {
        StringBuilder prompt = new StringBuilder("""
                【正文文风规则】
                以下规则是正文生成的强制约束，不是剧情资料。无论其他上下文采用何种措辞，都以这些规则为准。
                使用现代、自然、完整的中文；保持作品地域、时代、叙事视角和人物身份一致。
                不得从参考材料复制标志性句子，不得把参考作品的角色、地点或剧情带入当前小说。

                【本次选择的文风】
                """).append(selectedRules == null ? "" : selectedRules.trim());
        if (forbiddenWords != null && !forbiddenWords.isBlank()) {
            prompt.append("\n\n【明确避免的表达】\n").append(forbiddenWords.trim());
        }
        return prompt.toString().strip();
    }
}
