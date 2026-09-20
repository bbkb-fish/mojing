package com.mojing.novel.completion;

import com.mojing.novel.qdrant.QdrantMemoryService;
import com.mojing.novel.qdrant.VectorMemoryDtos.MemorySearchHit;
import com.mojing.novel.qdrant.VectorMemoryDtos.MemorySearchResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagContextServiceTest {

    @Test
    void retrievesPreviousChapterMemoriesAndKeepsDiverseResults() {
        QdrantMemoryService qdrant = mock(QdrantMemoryService.class);
        when(qdrant.search(any())).thenReturn(new MemorySearchResponse("novel_memory", 1024, 3, List.of(
                hit("a", .82, 2, "SCENE", "哈基米在河边遭到黑衣人追杀", "MEDIUM"),
                hit("b", .79, 3, "FORESHADOWING", "黑衣人的令牌刻有玄字", "HIGH"),
                hit("c", .78, 3, "FORESHADOWING", "黑衣人的令牌刻有玄字", "HIGH")
        )));

        RagContextService.RagContext result = new RagContextService(qdrant)
                .retrieve(7L, 9L, "哈基米再次看见那枚令牌。", "继续调查黑衣人的身份");

        assertThat(result.debugInfo().retrievedCount()).isEqualTo(3);
        assertThat(result.debugInfo().usedCount()).isEqualTo(2);
        assertThat(result.promptContext()).contains("第2章/SCENE", "第3章/FORESHADOWING");
        ArgumentCaptor<com.mojing.novel.qdrant.VectorMemoryDtos.SearchMemoryRequest> captor =
                ArgumentCaptor.forClass(com.mojing.novel.qdrant.VectorMemoryDtos.SearchMemoryRequest.class);
        verify(qdrant).search(captor.capture());
        assertThat(captor.getValue().excludeChapterId()).isEqualTo(9L);
        assertThat(captor.getValue().beforeChapterOrder()).isNull();
        assertThat(captor.getValue().limit()).isEqualTo(8);
    }

    @Test
    void degradesGracefullyWhenVectorStoreIsUnavailable() {
        QdrantMemoryService qdrant = mock(QdrantMemoryService.class);
        when(qdrant.search(any())).thenThrow(new RuntimeException("offline"));

        RagContextService.RagContext result = new RagContextService(qdrant)
                .retrieve(7L, 9L, "正文", "");

        assertThat(result.promptContext()).isEmpty();
        assertThat(result.debugInfo().status()).isEqualTo("UNAVAILABLE");
    }

    @Test
    void filtersMemoriesWhoseChaptersHaveAlreadyBeenDeleted() {
        QdrantMemoryService qdrant = mock(QdrantMemoryService.class);
        when(qdrant.search(any())).thenReturn(new MemorySearchResponse("novel_memory", 1024, 2, List.of(
                hit("valid", .82, 1, "SCENE", "仍然存在的第一章剧情", "MEDIUM"),
                new MemorySearchHit("orphan", .91, 7L, 404L, 1, 11L,
                        "SCENE", "已删除章节的残留剧情", "HIGH", 1, "2026-08-28T00:00:00Z")
        )));

        RagContextService.RagContext result = new RagContextService(qdrant)
                .retrieveOlderChapters(7L, 9L, 2, Set.of(1L), "当前正文", "讨论后续");

        assertThat(result.promptContext()).contains("仍然存在的第一章剧情");
        assertThat(result.promptContext()).doesNotContain("已删除章节的残留剧情");
        assertThat(result.debugInfo().retrievedCount()).isEqualTo(1);
    }

    private MemorySearchHit hit(String id, double score, int chapterOrder, String type,
                                String text, String importance) {
        return new MemorySearchHit(id, score, 7L, (long) chapterOrder, chapterOrder, 11L,
                type, text, importance, 1, "2026-08-28T00:00:00Z");
    }
}
