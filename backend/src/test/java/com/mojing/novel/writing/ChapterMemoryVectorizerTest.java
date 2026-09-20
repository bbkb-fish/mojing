package com.mojing.novel.writing;

import com.mojing.novel.qdrant.QdrantMemoryService;
import com.mojing.novel.qdrant.VectorMemoryDtos.VectorMemoryItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.mojing.novel.writing.AgentDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChapterMemoryVectorizerTest {
    @Test
    void createsStorylineSceneCharacterAndFactVectors() {
        QdrantMemoryService qdrant = mock(QdrantMemoryService.class);
        ChapterMemoryVectorizer vectorizer = new ChapterMemoryVectorizer(qdrant);
        ChapterMemoryEntity memory = mock(ChapterMemoryEntity.class);
        when(memory.getNovelId()).thenReturn(1L); when(memory.getChapterId()).thenReturn(2L);
        when(memory.getChapterNo()).thenReturn(3); when(memory.getId()).thenReturn(4L);
        when(memory.getSourceChapterVersion()).thenReturn(5L);
        MemoryScene scene = new MemoryScene(1, "持续交战", "HARD", "持续交战", "上午至下午",
                List.of("大山", "河边"), List.of("哈基米", "XXX"), "击败对方", "双方交战",
                List.of(new MemorySceneEvent("上午", "大山", "打入山中"), new MemorySceneEvent("下午", "河边", "转移到河边")), "尚未分出胜负");
        ChapterMemoryContent content = new ChapterMemoryContent("chapter-compression-v1", "哈基米与XXX持续交战",
                "上午至下午", List.of("大山", "河边"),
                List.of(new MemoryCharacter("哈基米", "主角", "持续战斗", "体力下降", "发现对手弱点", false)),
                List.of(scene), List.of(new MemoryImportantFact("UNRESOLVED_THREAD", "双方尚未分出胜负", "HIGH")),
                List.of(), List.of(), List.of(), "推动冲突", "HIGH");

        vectorizer.replace(memory, content);

        @SuppressWarnings("unchecked") ArgumentCaptor<List<VectorMemoryItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(qdrant).replaceChapterMemory(eq(1L), eq(2L), eq(3), eq(4L), eq(5L),
                eq("chapter-compression-v1"), captor.capture());
        assertThat(captor.getValue()).extracting(VectorMemoryItem::memoryType)
                .containsExactly("CHAPTER_STORYLINE", "SCENE", "CHARACTER_HISTORY", "UNRESOLVED_THREAD");
        assertThat(captor.getValue().get(1).text()).contains("上午", "大山", "下午", "河边");
    }
}
