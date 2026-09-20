package com.mojing.novel.completion;

import com.mojing.novel.qdrant.QdrantMemoryService;
import com.mojing.novel.qdrant.VectorMemoryDtos.MemorySearchHit;
import com.mojing.novel.qdrant.VectorMemoryDtos.MemorySearchResponse;
import com.mojing.novel.qdrant.VectorMemoryDtos.SearchMemoryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mojing.novel.completion.CompletionResponse.RagDebugInfo;
import static com.mojing.novel.completion.CompletionResponse.RagMemoryHit;

@Service
public class RagContextService {
    private static final Logger log = LoggerFactory.getLogger(RagContextService.class);
    private static final int RETRIEVAL_LIMIT = 8;
    private static final int USED_LIMIT = 5;
    private static final double SCORE_THRESHOLD = 0.45;
    private static final int QUERY_CONTEXT_LIMIT = 1_200;

    private final QdrantMemoryService qdrantMemoryService;

    public RagContextService(QdrantMemoryService qdrantMemoryService) {
        this.qdrantMemoryService = qdrantMemoryService;
    }

    public RagContext retrieve(Long novelId, Long chapterId, String cursorContext, String instruction) {
        return retrieve(novelId, chapterId, null, cursorContext, instruction, "续写");
    }

    public RagContext retrieveOlderChapters(Long novelId, Long chapterId, Integer beforeChapterOrder,
                                            String cursorContext, String instruction) {
        return retrieveOlderChapters(novelId, chapterId, beforeChapterOrder, null, cursorContext, instruction);
    }

    public RagContext retrieveOlderChapters(Long novelId, Long chapterId, Integer beforeChapterOrder,
                                            Set<Long> allowedChapterIds, String cursorContext, String instruction) {
        if (beforeChapterOrder == null || beforeChapterOrder <= 1) return RagContext.disabled();
        if (allowedChapterIds != null && allowedChapterIds.isEmpty()) return RagContext.disabled();
        return retrieve(novelId, chapterId, beforeChapterOrder, allowedChapterIds,
                cursorContext, instruction, "章节导演");
    }

    private RagContext retrieve(Long novelId, Long chapterId, Integer beforeChapterOrder,
                                String cursorContext, String instruction, String usage) {
        return retrieve(novelId, chapterId, beforeChapterOrder, null, cursorContext, instruction, usage);
    }

    private RagContext retrieve(Long novelId, Long chapterId, Integer beforeChapterOrder,
                                Set<Long> allowedChapterIds, String cursorContext,
                                String instruction, String usage) {
        if (novelId == null || chapterId == null) return RagContext.disabled();
        long started = System.nanoTime();
        String query = buildQuery(cursorContext, instruction);
        try {
            MemorySearchResponse response = qdrantMemoryService.search(new SearchMemoryRequest(
                    novelId, query, RETRIEVAL_LIMIT, SCORE_THRESHOLD, chapterId, beforeChapterOrder));
            List<ScoredHit> ranked = response.hits().stream()
                    .filter(hit -> allowedChapterIds == null || allowedChapterIds.contains(hit.chapterId()))
                    .map(hit -> new ScoredHit(hit, rerankScore(hit)))
                    .sorted(Comparator.comparingDouble(ScoredHit::rankScore).reversed())
                    .toList();
            List<MemorySearchHit> used = selectDiverse(ranked);
            Set<String> usedIds = used.stream().map(MemorySearchHit::pointId)
                    .collect(java.util.stream.Collectors.toSet());
            List<RagMemoryHit> debugHits = ranked.stream().map(item -> toDebugHit(item.hit(), usedIds)).toList();
            long elapsed = elapsedMs(started);
            log.info("{}RAG检索完成 novelId={} chapterId={} beforeChapterOrder={} retrieved={} used={} elapsedMs={}",
                    usage, novelId, chapterId, beforeChapterOrder, ranked.size(), used.size(), elapsed);
            return new RagContext(formatContext(used),
                    new RagDebugInfo(true, "COMPLETED", ranked.size(), used.size(), elapsed, debugHits));
        } catch (RuntimeException error) {
            long elapsed = elapsedMs(started);
            log.warn("{}RAG检索失败，自动降级 novelId={} chapterId={} beforeChapterOrder={} elapsedMs={} reason={}",
                    usage, novelId, chapterId, beforeChapterOrder, elapsed, error.getMessage());
            return new RagContext("", new RagDebugInfo(true, "UNAVAILABLE", 0, 0, elapsed, List.of()));
        }
    }

    private List<MemorySearchHit> selectDiverse(List<ScoredHit> ranked) {
        List<MemorySearchHit> selected = new ArrayList<>();
        Map<String, Integer> typeCounts = new HashMap<>();
        Set<String> normalizedTexts = new LinkedHashSet<>();
        for (ScoredHit item : ranked) {
            MemorySearchHit hit = item.hit();
            String normalizedText = hit.text().replaceAll("\\s+", "").trim();
            if (!normalizedTexts.add(normalizedText)) continue;
            int count = typeCounts.getOrDefault(hit.memoryType(), 0);
            if (count >= 2) continue;
            selected.add(hit);
            typeCounts.put(hit.memoryType(), count + 1);
            if (selected.size() >= USED_LIMIT) break;
        }
        return selected;
    }

    private double rerankScore(MemorySearchHit hit) {
        double score = hit.score();
        if ("HIGH".equalsIgnoreCase(hit.importance())) score += 0.04;
        if (isContinuityFact(hit.memoryType())) score += 0.03;
        return score;
    }

    private boolean isContinuityFact(String memoryType) {
        return switch (memoryType) {
            case "FORESHADOWING", "UNRESOLVED_THREAD", "RELATIONSHIP", "REVEAL", "WORLD_RULE" -> true;
            default -> false;
        };
    }

    private String formatContext(List<MemorySearchHit> memories) {
        if (memories.isEmpty()) return "";
        StringBuilder text = new StringBuilder();
        for (MemorySearchHit memory : memories) {
            text.append("- [第").append(memory.chapterOrder() == null ? "?" : memory.chapterOrder())
                    .append("章/").append(memory.memoryType()).append("] ")
                    .append(memory.text()).append('\n');
        }
        return text.toString().stripTrailing();
    }

    private RagMemoryHit toDebugHit(MemorySearchHit hit, Set<String> usedIds) {
        return new RagMemoryHit(hit.pointId(), hit.score(), hit.chapterId(), hit.chapterOrder(),
                hit.memoryType(), hit.text(), usedIds.contains(hit.pointId()));
    }

    private String buildQuery(String cursorContext, String instruction) {
        String context = cursorContext == null ? "" : cursorContext.trim();
        if (context.length() > QUERY_CONTEXT_LIMIT) context = context.substring(context.length() - QUERY_CONTEXT_LIMIT);
        if (instruction == null || instruction.isBlank()) return context;
        return context + "\n续写要求：" + instruction.trim();
    }

    private long elapsedMs(long started) { return (System.nanoTime() - started) / 1_000_000; }

    private record ScoredHit(MemorySearchHit hit, double rankScore) {}

    public record RagContext(String promptContext, RagDebugInfo debugInfo) {
        private static RagContext disabled() {
            return new RagContext("", new RagDebugInfo(false, "SKIPPED", 0, 0, 0, List.of()));
        }
    }
}
