package com.mojing.novel.qdrant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.config.QdrantProperties;
import com.mojing.novel.embedding.EmbeddingDtos.EmbeddingResponse;
import com.mojing.novel.embedding.EmbeddingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.mojing.novel.qdrant.VectorMemoryDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QdrantMemoryServiceTest {
    @Test
    void createsCollectionAndUpsertsEmbeddedMemory() throws Exception {
        AtomicBoolean collectionExists = new AtomicBoolean(false);
        AtomicReference<String> createBody = new AtomicReference<>();
        AtomicReference<String> upsertBody = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/points")) {
                upsertBody.set(readBody(exchange));
                respond(exchange, 200, "{\"result\":{\"status\":\"completed\"},\"status\":\"ok\"}");
            } else if ("GET".equals(exchange.getRequestMethod()) && !collectionExists.get()) {
                respond(exchange, 404, "{\"status\":{\"error\":\"not found\"}}");
            } else if ("PUT".equals(exchange.getRequestMethod())) {
                createBody.set(readBody(exchange));
                collectionExists.set(true);
                respond(exchange, 200, "{\"result\":true,\"status\":\"ok\"}");
            } else {
                respond(exchange, 200, collectionInfo());
            }
        });
        server.start();
        try {
            EmbeddingService embeddingService = mock(EmbeddingService.class);
            when(embeddingService.embed("哈基米遭到黑衣人追杀"))
                    .thenReturn(new EmbeddingResponse("test", 3, List.of(0.1, 0.2, 0.3), 5, 5));
            QdrantMemoryService service = service(server, embeddingService);

            MemoryWriteResponse result = service.upsert(new UpsertMemoryRequest(
                    7, 12L, "PLOT_EVENT", "哈基米遭到黑衣人追杀", 3));

            assertThat(result.dimensions()).isEqualTo(3);
            assertThat(createBody.get()).contains("\"size\":3", "\"distance\":\"Cosine\"");
            assertThat(upsertBody.get()).contains("\"novelId\":7", "\"chapterId\":12",
                    "\"memoryType\":\"PLOT_EVENT\"", "\"vector\":[0.1,0.2,0.3]");
        } finally { server.stop(0); }
    }

    @Test
    void searchesOnlyInsideRequestedNovel() throws Exception {
        AtomicReference<String> queryBody = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/points/query")) {
                queryBody.set(readBody(exchange));
                respond(exchange, 200, """
                        {"result":{"points":[{"id":"point-1","score":0.91,"payload":{
                          "novelId":7,"chapterId":12,"memoryType":"PLOT_EVENT",
                          "text":"哈基米在雨夜遭到追杀","sourceRevision":3,"createdAt":"2026-08-28T00:00:00Z"
                        }}]},"status":"ok"}
                        """);
            } else {
                respond(exchange, 200, collectionInfo());
            }
        });
        server.start();
        try {
            EmbeddingService embeddingService = mock(EmbeddingService.class);
            when(embeddingService.embed("谁追杀了哈基米"))
                    .thenReturn(new EmbeddingResponse("test", 3, List.of(0.2, 0.1, 0.4), 4, 4));
            QdrantMemoryService service = service(server, embeddingService);

            MemorySearchResponse result = service.search(new SearchMemoryRequest(7,
                    "谁追杀了哈基米", 3, 0.5, 12L, 10));

            assertThat(result.count()).isEqualTo(1);
            assertThat(result.hits().getFirst().text()).isEqualTo("哈基米在雨夜遭到追杀");
            assertThat(result.hits().getFirst().score()).isEqualTo(0.91);
            assertThat(queryBody.get()).contains("\"key\":\"novelId\"", "\"value\":7",
                    "\"must_not\"", "\"key\":\"chapterId\"", "\"value\":12",
                    "\"key\":\"chapterOrder\"", "\"lt\":10",
                    "\"limit\":3", "\"score_threshold\":0.5");
        } finally { server.stop(0); }
    }

    @Test
    void storesStoryBibleChatInDedicatedCollection() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> upsertBody = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            if (requestPath.get().endsWith("/points")) {
                upsertBody.set(readBody(exchange));
                respond(exchange, 200, "{\"result\":{\"status\":\"completed\"},\"status\":\"ok\"}");
            } else {
                respond(exchange, 200, collectionInfo());
            }
        });
        server.start();
        try {
            EmbeddingService embeddingService = mock(EmbeddingService.class);
            when(embeddingService.embed("讨论主角与反派的旧约"))
                    .thenReturn(new EmbeddingResponse("test", 3, List.of(0.3, 0.2, 0.1), 6, 6));
            QdrantMemoryService service = service(server, embeddingService);

            service.upsertStoryBibleMessage(7L, 23L, "讨论主角与反派的旧约");

            assertThat(requestPath.get()).isEqualTo("/collections/novel_story_bible_chat/points");
            assertThat(upsertBody.get()).contains("\"novelId\":7", "\"storyBibleMessageId\":23",
                    "\"memoryType\":\"STORY_BIBLE_CHAT\"");
        } finally { server.stop(0); }
    }

    private QdrantMemoryService service(HttpServer server, EmbeddingService embeddingService) {
        QdrantProperties properties = new QdrantProperties(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-qdrant-key",
                "novel_memory", "novel_story_bible_chat", 3, Duration.ofSeconds(5));
        return new QdrantMemoryService(properties, new ObjectMapper(), embeddingService);
    }

    private HttpServer server(ExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/collections/novel_memory", exchange -> handler.handle(exchange));
        server.createContext("/collections/novel_story_bible_chat", exchange -> handler.handle(exchange));
        return server;
    }

    private static String collectionInfo() {
        return "{\"result\":{\"config\":{\"params\":{\"vectors\":{\"size\":3,\"distance\":\"Cosine\"}}}},\"status\":\"ok\"}";
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler { void handle(HttpExchange exchange) throws IOException; }
}
