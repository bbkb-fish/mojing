package com.mojing.novel.skill;

import com.mojing.novel.writing.AgentDtos.ChapterMemoryContent;
import com.mojing.novel.writing.AgentDtos.MemoryScene;
import com.mojing.novel.writing.AgentDtos.MemorySceneEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class ChapterCompressionSkill {
    public static final String VERSION = "chapter-compression-v1";
    private static final Logger log = LoggerFactory.getLogger(ChapterCompressionSkill.class);
    private final String systemPrompt;

    public ChapterCompressionSkill() {
        this.systemPrompt = read("skills/chapter-compression/v1/system-prompt.md")
                + "\n\n【必须遵守的输出 JSON Schema】\n"
                + read("skills/chapter-compression/v1/output-schema.json");
        log.info("章节压缩Skill加载完成 version={} promptChars={}", VERSION, systemPrompt.length());
    }

    public String version() { return VERSION; }
    public String systemPrompt() { return systemPrompt; }

    public ChapterMemoryContent normalize(ChapterMemoryContent content) {
        int candidateCount = content.scenes().size();
        List<MemoryScene> merged = new ArrayList<>();
        for (MemoryScene raw : content.scenes()) {
            MemoryScene next = normalizedScene(raw, merged.size() + 1);
            if (!merged.isEmpty() && shouldMerge(merged.getLast(), next)) {
                MemoryScene previous = merged.removeLast();
                merged.add(merge(previous, next, merged.size() + 1));
            } else {
                merged.add(next);
            }
        }
        for (int i = 0; i < merged.size(); i++) merged.set(i, withIndex(merged.get(i), i + 1));
        log.info("章节压缩场景归一化完成 version={} candidates={} merged={} removed={}",
                VERSION, candidateCount, merged.size(), candidateCount - merged.size());
        return new ChapterMemoryContent(VERSION, content.summary(), content.timeInfo(), content.locations(),
                content.characters(), List.copyOf(merged), content.importantFacts(), content.keyEvents(),
                content.foreshadowings(), content.unresolvedQuestions(), content.plotProgress(), content.importance(),
                content.identityReveals());
    }

    private boolean shouldMerge(MemoryScene previous, MemoryScene next) {
        return "SOFT".equals(next.boundaryType())
                && !previous.continuityKey().isBlank()
                && previous.continuityKey().equalsIgnoreCase(next.continuityKey());
    }

    private MemoryScene merge(MemoryScene previous, MemoryScene next, int index) {
        List<MemorySceneEvent> events = new ArrayList<>(previous.subEvents());
        events.addAll(next.subEvents());
        return new MemoryScene(index, previous.title(), previous.boundaryType(), previous.continuityKey(),
                join(previous.timeSpan(), next.timeSpan()), union(previous.locations(), next.locations()),
                union(previous.characters(), next.characters()), join(previous.goal(), next.goal()),
                join(previous.conflict(), next.conflict()), List.copyOf(events),
                hasText(next.result()) ? next.result() : previous.result());
    }

    private MemoryScene normalizedScene(MemoryScene scene, int fallbackIndex) {
        return new MemoryScene(scene.sceneIndex() == null || scene.sceneIndex() < 1 ? fallbackIndex : scene.sceneIndex(),
                text(scene.title(), "未命名场景"), text(scene.boundaryType(), fallbackIndex == 1 ? "HARD" : "SOFT"),
                text(scene.continuityKey(), scene.title()), scene.timeSpan(), safe(scene.locations()),
                safe(scene.characters()), scene.goal(), scene.conflict(), safe(scene.subEvents()), scene.result());
    }

    private MemoryScene withIndex(MemoryScene scene, int index) {
        return new MemoryScene(index, scene.title(), index == 1 ? "HARD" : scene.boundaryType(), scene.continuityKey(), scene.timeSpan(),
                scene.locations(), scene.characters(), scene.goal(), scene.conflict(), scene.subEvents(), scene.result());
    }

    private String join(String left, String right) {
        if (!hasText(left)) return right == null ? "" : right;
        if (!hasText(right) || left.equals(right)) return left;
        return left + "；" + right;
    }

    private <T> List<T> safe(List<T> values) { return values == null ? List.of() : List.copyOf(values); }
    private List<String> union(List<String> left, List<String> right) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (left != null) result.addAll(left);
        if (right != null) result.addAll(right);
        return List.copyOf(result);
    }
    private String text(String value, String fallback) { return hasText(value) ? value.strip() : fallback; }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    private String read(String path) {
        try { return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8); }
        catch (Exception error) { throw new IllegalStateException("无法加载章节压缩 Skill：" + path, error); }
    }
}
