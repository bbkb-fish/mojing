package com.mojing.novel.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingBatchingTest {
    @Test
    void splitsTwentyThreeTextsIntoTenTenAndThreeWithoutChangingOrder() {
        List<String> texts = IntStream.range(0, 23).mapToObj(i -> "文本" + i).toList();

        List<List<String>> batches = EmbeddingService.partitionForProvider(texts);

        assertThat(batches).extracting(List::size).containsExactly(10, 10, 3);
        assertThat(batches.stream().flatMap(List::stream).toList()).containsExactlyElementsOf(texts);
    }
}
