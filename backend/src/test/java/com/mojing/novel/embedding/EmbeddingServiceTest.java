package com.mojing.novel.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.config.EmbeddingProperties;
import com.mojing.novel.writing.WritingConflictException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class EmbeddingServiceTest {
    @Test
    void sendsTextAndReturnsFloatVector() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/v1/embeddings", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"model":"text-embedding-v4","data":[{"embedding":[0.12,-0.34,0.56]}],
                     "usage":{"prompt_tokens":8,"total_tokens":8}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            EmbeddingProperties properties = new EmbeddingProperties("test-key",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                    "text-embedding-v4", 3, Duration.ofSeconds(5));
            var result = new EmbeddingService(properties, new ObjectMapper()).embed("黑衣人在雨夜追杀哈基米");
            assertThat(result.dimensions()).isEqualTo(3);
            assertThat(result.vector()).containsExactly(0.12, -0.34, 0.56);
            assertThat(result.totalTokens()).isEqualTo(8);
            assertThat(requestBody.get()).contains("黑衣人在雨夜追杀哈基米", "\"dimensions\":3");
        } finally { server.stop(0); }
    }

    @Test
    void explainsMissingApiKey() {
        EmbeddingProperties properties = new EmbeddingProperties("", "http://127.0.0.1:1/v1",
                "text-embedding-v4", 1024, Duration.ofSeconds(1));
        assertThatThrownBy(() -> new EmbeddingService(properties, new ObjectMapper()).embed("测试"))
                .isInstanceOf(WritingConflictException.class)
                .hasMessageContaining("EMBEDDING_API_KEY");
    }

    @Test
    void splitsMoreThanTenTextsAndKeepsVectorOrder() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requestCount = new AtomicInteger();
        List<Integer> batchSizes = new ArrayList<>();
        server.createContext("/v1/embeddings", exchange -> {
            JsonNode input = mapper.readTree(exchange.getRequestBody()).path("input");
            requestCount.incrementAndGet();
            batchSizes.add(input.size());
            var data = mapper.createArrayNode();
            for (int position = input.size() - 1; position >= 0; position--) {
                int value = Integer.parseInt(input.path(position).asText().substring(2));
                var item = mapper.createObjectNode();
                item.put("index", position);
                item.putArray("embedding").add(value).add(-value).add(value + 0.5);
                data.add(item);
            }
            var responseBody = mapper.createObjectNode();
            responseBody.put("model", "text-embedding-v4");
            responseBody.set("data", data);
            responseBody.putObject("usage").put("prompt_tokens", input.size()).put("total_tokens", input.size() + 1);
            byte[] response = mapper.writeValueAsBytes(responseBody);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            EmbeddingProperties properties = new EmbeddingProperties("test-key",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                    "text-embedding-v4", 3, Duration.ofSeconds(5));
            List<String> texts = java.util.stream.IntStream.range(0, 23).mapToObj(i -> "文本" + i).toList();

            var result = new EmbeddingService(properties, mapper).embedBatch(texts);

            assertThat(requestCount).hasValue(3);
            assertThat(batchSizes).containsExactly(10, 10, 3);
            assertThat(result.vectors()).hasSize(23);
            assertThat(result.vectors()).extracting(vector -> vector.getFirst())
                    .containsExactlyElementsOf(java.util.stream.IntStream.range(0, 23).mapToObj(i -> (double) i).toList());
            assertThat(result.promptTokens()).isEqualTo(23);
            assertThat(result.totalTokens()).isEqualTo(26);
        } finally { server.stop(0); }
    }
}
