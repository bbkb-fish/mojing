package com.mojing.novel.writing;

import com.mojing.novel.qdrant.QdrantMemoryService;
import com.mojing.novel.qdrant.VectorMemoryDtos.VectorMemoryItem;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mojing.novel.writing.AgentDtos.*;

@Component
public class ChapterMemoryVectorizer {
    private static final Logger log = LoggerFactory.getLogger(ChapterMemoryVectorizer.class);
    private final QdrantMemoryService qdrantMemoryService;

    public ChapterMemoryVectorizer(QdrantMemoryService qdrantMemoryService) {
        this.qdrantMemoryService = qdrantMemoryService;
    }

    public void replace(ChapterMemoryEntity memory, ChapterMemoryContent content) {
        long started = System.nanoTime();
        List<VectorMemoryItem> items = new ArrayList<>();
        items.add(new VectorMemoryItem("CHAPTER_STORYLINE", "第" + memory.getChapterNo() + "章故事线：" + content.summary(),
                Map.of("importance", content.importance())));
        for (MemoryScene scene : content.scenes()) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sceneIndex", scene.sceneIndex()); metadata.put("boundaryType", scene.boundaryType());
            metadata.put("characters", scene.characters()); metadata.put("locations", scene.locations());
            items.add(new VectorMemoryItem("SCENE", sceneText(memory.getChapterNo(), scene), metadata));
        }
        for (MemoryCharacter character : content.characters()) {
            Map<String, Object> metadata = Map.of("characterName", character.name(), "importance", content.importance());
            items.add(new VectorMemoryItem("CHARACTER_HISTORY", characterText(memory.getChapterNo(), character), metadata));
        }
        for (MemoryImportantFact fact : content.importantFacts())
            items.add(new VectorMemoryItem(fact.type(), "第" + memory.getChapterNo() + "章" + fact.content(),
                    Map.of("importance", fact.importance())));
        log.info("开始向量化章节记忆 memoryId={} novelId={} chapterId={} sourceVersion={} items={} scenes={} characters={} facts={}",
                memory.getId(), memory.getNovelId(), memory.getChapterId(), memory.getSourceChapterVersion(),
                items.size(), content.scenes().size(), content.characters().size(), content.importantFacts().size());
        try {
            qdrantMemoryService.replaceChapterMemory(memory.getNovelId(), memory.getChapterId(), memory.getChapterNo(),
                    memory.getId(), memory.getSourceChapterVersion(), content.skillVersion(), items);
            log.info("章节记忆向量化完成 memoryId={} chapterId={} items={} elapsedMs={}", memory.getId(),
                    memory.getChapterId(), items.size(), elapsedMs(started));
        } catch (RuntimeException error) {
            log.error("章节记忆向量化失败 memoryId={} chapterId={} items={} elapsedMs={} reason={}", memory.getId(),
                    memory.getChapterId(), items.size(), elapsedMs(started), error.getMessage());
            throw error;
        }
    }

    private String sceneText(int chapterNo, MemoryScene scene) {
        StringBuilder text = new StringBuilder("第").append(chapterNo).append("章场景").append(scene.sceneIndex())
                .append("：").append(scene.title()).append("。");
        append(text, "时间", scene.timeSpan()); appendList(text, "地点", scene.locations());
        appendList(text, "人物", scene.characters()); append(text, "目标", scene.goal()); append(text, "冲突", scene.conflict());
        for (MemorySceneEvent event : scene.subEvents())
            text.append("子事件：").append(event.time()).append("，").append(event.location()).append("，").append(event.event()).append("。");
        append(text, "结果", scene.result()); return text.toString();
    }

    private String characterText(int chapterNo, MemoryCharacter character) {
        StringBuilder text = new StringBuilder("第").append(chapterNo).append("章角色：").append(character.name()).append("。");
        append(text, "身份", character.role()); append(text, "行为", character.actions());
        append(text, "状态变化", character.stateChange()); append(text, "新获得的信息", character.newKnowledge());
        appendList(text, "人物伏笔", character.foreshadowings());
        return text.toString();
    }

    private void append(StringBuilder text, String label, String value) {
        if (value != null && !value.isBlank()) text.append(label).append("：").append(value).append("。");
    }
    private void appendList(StringBuilder text, String label, List<String> values) {
        if (values != null && !values.isEmpty()) append(text, label, String.join("、", values));
    }
    private long elapsedMs(long started) { return (System.nanoTime() - started) / 1_000_000; }
}
