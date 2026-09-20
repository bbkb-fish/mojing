package com.mojing.novel.style;

import java.util.Arrays;
import java.util.Optional;

enum WritingStylePreset {
    NATURAL_WEB("现代自然网文", "自然、清楚、避免套话的现代中文叙事", """
            使用自然流畅的现代中文。叙事清楚，人物行动具体，对话符合身份。
            避免文言文、堆砌辞藻、空泛抒情和常见网文套话。
            """),
    INDUSTRIAL_BRITAIN("工业革命英伦写实", "适合十八世纪末至十九世纪初的英国工业社会", """
            使用现代中文创作，呈现十八世纪末至十九世纪初英国小说的翻译文学质感。
            叙述克制、写实，重视煤烟、机器、工厂、码头、街道、阶层差异和人物细微反应。
            人物对话符合职业、教育和社会阶层；贵族较含蓄，工人更直接，商人重视利益。
            禁止文言文、中国古代制度与称谓、武侠仙侠语言和中国风意象。
            不得使用“在下、此乃、诸位、银两、官府、衙门、客栈、宗门、江湖、拂袖而去、心中暗道、冷哼一声、嘴角勾起一抹弧度”等表达。
            """),
    WESTERN_FANTASY("西方奇幻译文风", "克制而有史诗感的西方奇幻中文译文风格", """
            使用清晰、沉稳的现代中文，保留西方奇幻译文的庄重感，但不要使用文言文。
            重视具体环境、历史重量和人物选择，避免仙侠、宗门、江湖和东方古典称谓。
            """),
    COLD_SUSPENSE("冷峻悬疑", "短促、克制，强调观察、线索和不安感", """
            语言冷静克制，减少直接解释情绪，通过动作、环境和细节制造不安。
            句子以短句和中等长度句为主，不故作玄虚，不使用夸张网文套话。
            """),
    LIGHT_NOVEL("轻小说", "轻快、角色驱动、对话比例较高", """
            使用轻快自然的现代中文，以角色互动和对话推动场景，心理活动简洁有趣。
            避免过度书面化、文言文和沉重说教。
            """),
    CONCISE("简洁白描", "少修辞、重动作与有效信息", """
            使用简洁白描。优先写动作、对话、可观察细节和剧情变化。
            少用比喻、副词、抽象抒情和重复解释，不堆砌形容词。
            """);

    private final String displayName;
    private final String description;
    private final String prompt;

    WritingStylePreset(String displayName, String description, String prompt) {
        this.displayName = displayName;
        this.description = description;
        this.prompt = prompt.strip();
    }

    String styleId() { return "preset:" + name(); }
    String displayName() { return displayName; }
    String description() { return description; }
    String prompt() { return prompt; }

    static Optional<WritingStylePreset> fromStyleId(String styleId) {
        if (styleId == null || !styleId.startsWith("preset:")) return Optional.empty();
        String code = styleId.substring("preset:".length());
        return Arrays.stream(values()).filter(item -> item.name().equals(code)).findFirst();
    }
}
