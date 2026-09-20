package com.mojing.novel.qdrant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.config.QdrantProperties;
import com.mojing.novel.embedding.EmbeddingDtos.EmbeddingResponse;
import com.mojing.novel.embedding.EmbeddingDtos.BatchEmbeddingResponse;
import com.mojing.novel.embedding.EmbeddingService;
import com.mojing.novel.writing.WritingConflictException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mojing.novel.qdrant.VectorMemoryDtos.*;

@Service
public class QdrantMemoryService {
    private static final Logger log = LoggerFactory.getLogger(QdrantMemoryService.class);
    private final QdrantProperties properties;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final HttpClient httpClient;

    public QdrantMemoryService(QdrantProperties properties, ObjectMapper objectMapper,
                               EmbeddingService embeddingService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    public CollectionStatusResponse initializeCollection() {
        validateConfiguration();
        ensureCollection();
        log.info("Qdrant集合已就绪 collection={} vectorSize={} distance=Cosine", properties.collection(), properties.vectorSize());
        return new CollectionStatusResponse(properties.collection(), properties.vectorSize(), "Cosine", true);
    }

    public MemoryWriteResponse upsert(UpsertMemoryRequest request) {
        EmbeddingResponse embedding = embeddingService.embed(request.text());
        validateEmbeddingDimensions(embedding);
        ensureCollection();

        String pointId = UUID.randomUUID().toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("novelId", request.novelId());
        if (request.chapterId() != null) payload.put("chapterId", request.chapterId());
        payload.put("memoryType", request.memoryType().trim());
        payload.put("text", request.text().trim());
        if (request.sourceRevision() != null) payload.put("sourceRevision", request.sourceRevision());
        payload.put("createdAt", Instant.now().toString());

        Map<String, Object> point = new LinkedHashMap<>();
        point.put("id", pointId);
        point.put("vector", embedding.vector());
        point.put("payload", payload);
        sendJson("PUT", collectionUrl() + "/points?wait=true", Map.of("points", List.of(point)), "写入小说记忆");
        return new MemoryWriteResponse(pointId, request.novelId(), request.chapterId(),
                request.memoryType().trim(), embedding.dimensions());
    }

    public MemorySearchResponse search(SearchMemoryRequest request) {
        long started = System.nanoTime();
        EmbeddingResponse embedding = embeddingService.embed(request.text());
        validateEmbeddingDimensions(embedding);
        ensureCollection();

        Map<String, Object> matchNovel = Map.of("key", "novelId", "match", Map.of("value", request.novelId()));
        Map<String, Object> filter = new LinkedHashMap<>();
        List<Map<String, Object>> must = new ArrayList<>();
        must.add(matchNovel);
        if (request.beforeChapterOrder() != null)
            must.add(Map.of("key", "chapterOrder", "range", Map.of("lt", request.beforeChapterOrder())));
        filter.put("must", must);
        if (request.excludeChapterId() != null) {
            filter.put("must_not", List.of(match("chapterId", request.excludeChapterId())));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", embedding.vector());
        body.put("filter", filter);
        body.put("limit", request.limit() == null ? 5 : request.limit());
        body.put("with_payload", true);
        body.put("with_vector", false);
        if (request.scoreThreshold() != null) body.put("score_threshold", request.scoreThreshold());

        JsonNode root = sendJson("POST", collectionUrl() + "/points/query", body, "检索小说记忆");
        JsonNode points = root.path("result").path("points");
        if (!points.isArray()) throw new VectorStoreException("Qdrant检索响应中没有points数组");
        List<MemorySearchHit> hits = new ArrayList<>();
        for (JsonNode point : points) {
            JsonNode payload = point.path("payload");
            hits.add(new MemorySearchHit(point.path("id").asText(), point.path("score").asDouble(),
                    payload.path("novelId").asLong(), nullableLong(payload.get("chapterId")),
                    nullableInteger(payload.get("chapterOrder")), nullableLong(payload.get("memoryId")),
                    payload.path("memoryType").asText(), payload.path("text").asText(),
                    payload.path("importance").asText(""),
                    nullableInteger(payload.get("sourceRevision")), payload.path("createdAt").asText("")));
        }
        log.info("Qdrant小说记忆检索完成 novelId={} excludeChapterId={} beforeChapterOrder={} limit={} threshold={} hits={} elapsedMs={}",
                request.novelId(), request.excludeChapterId(), request.beforeChapterOrder(), request.limit(), request.scoreThreshold(),
                hits.size(), elapsedMs(started));
        return new MemorySearchResponse(properties.collection(), embedding.dimensions(), hits.size(), hits);
    }

    public void upsertStoryBibleMessage(long novelId, long messageId, String text) {
        if (text == null || text.isBlank()) return;
        EmbeddingResponse embedding = embeddingService.embed(text);
        validateEmbeddingDimensions(embedding);
        ensureCollection(properties.storyBibleCollection());
        String stableKey = "story-bible-chat:" + novelId + ":" + messageId;
        String pointId = UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("novelId", novelId);
        payload.put("storyBibleMessageId", messageId);
        payload.put("memoryType", "STORY_BIBLE_CHAT");
        payload.put("text", text.trim());
        payload.put("createdAt", Instant.now().toString());
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("id", pointId); point.put("vector", embedding.vector()); point.put("payload", payload);
        sendJson("PUT", collectionUrl(properties.storyBibleCollection()) + "/points?wait=true",
                Map.of("points", List.of(point)), "写入资料库对话向量");
    }

    public List<MemorySearchHit> searchStoryBibleMessages(long novelId, String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        EmbeddingResponse embedding = embeddingService.embed(query);
        validateEmbeddingDimensions(embedding);
        ensureCollection(properties.storyBibleCollection());
        Map<String, Object> filter = Map.of("must", List.of(match("novelId", novelId)));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", embedding.vector()); body.put("filter", filter);
        body.put("limit", Math.max(1, Math.min(limit, 12))); body.put("score_threshold", 0.42);
        body.put("with_payload", true); body.put("with_vector", false);
        JsonNode points = sendJson("POST", collectionUrl(properties.storyBibleCollection()) + "/points/query",
                body, "检索资料库历史对话").path("result").path("points");
        List<MemorySearchHit> hits = new ArrayList<>();
        if (!points.isArray()) return hits;
        for (JsonNode point : points) {
            JsonNode payload = point.path("payload");
            hits.add(new MemorySearchHit(point.path("id").asText(), point.path("score").asDouble(),
                    novelId, null, null, nullableLong(payload.get("storyBibleMessageId")),
                    "STORY_BIBLE_CHAT", payload.path("text").asText(), "", null,
                    payload.path("createdAt").asText("")));
        }
        return hits;
    }

    public void replaceChapterMemory(long novelId, long chapterId, int chapterOrder, long memoryId,
                                     long sourceRevision, String skillVersion, List<VectorMemoryItem> items) {
        if (items == null || items.isEmpty()) throw new WritingConflictException("章节记忆没有可向量化的内容");
        long started = System.nanoTime();
        log.info("开始替换Qdrant章节记忆 collection={} novelId={} chapterId={} memoryId={} sourceRevision={} items={}",
                properties.collection(), novelId, chapterId, memoryId, sourceRevision, items.size());
        BatchEmbeddingResponse embeddings = embeddingService.embedBatch(items.stream().map(VectorMemoryItem::text).toList());
        if (embeddings.dimensions() != properties.vectorSize())
            throw new WritingConflictException("Embedding返回 " + embeddings.dimensions() + " 维，但Qdrant配置为 " + properties.vectorSize() + " 维");
        ensureCollection();

        List<Map<String, Object>> points = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            VectorMemoryItem item = items.get(index);
            String stableKey = novelId + ":" + chapterId + ":" + item.memoryType() + ":" + index;
            String pointId = UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("novelId", novelId); payload.put("chapterId", chapterId); payload.put("chapterOrder", chapterOrder);
            payload.put("memoryId", memoryId); payload.put("memoryType", item.memoryType()); payload.put("text", item.text());
            payload.put("sourceRevision", sourceRevision); payload.put("skillVersion", skillVersion);
            payload.put("createdAt", Instant.now().toString()); payload.putAll(item.metadata());
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("id", pointId); point.put("vector", embeddings.vectors().get(index)); point.put("payload", payload);
            points.add(point);
        }
        sendJson("PUT", collectionUrl() + "/points?wait=true", Map.of("points", points), "批量写入章节记忆");

        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("must", List.of(match("novelId", novelId), match("chapterId", chapterId)));
        filter.put("must_not", List.of(match("memoryId", memoryId)));
        sendJson("POST", collectionUrl() + "/points/delete?wait=true", Map.of("filter", filter), "清理旧版章节向量");
        log.info("Qdrant章节记忆替换完成 collection={} novelId={} chapterId={} memoryId={} points={} elapsedMs={}",
                properties.collection(), novelId, chapterId, memoryId, points.size(), elapsedMs(started));
    }

    public void deleteChapterMemory(long novelId, long chapterId) {
        validateConfiguration();
        ensureCollection();
        Map<String, Object> filter = Map.of("must", List.of(
                match("novelId", novelId), match("chapterId", chapterId)));
        sendJson("POST", collectionUrl() + "/points/delete?wait=true", Map.of("filter", filter), "删除章节向量");
        log.info("Qdrant章节向量删除完成 collection={} novelId={} chapterId={}",
                properties.collection(), novelId, chapterId);
    }

    private void ensureCollection() {
        ensureCollection(properties.collection());
    }

    private void ensureCollection(String collection) {
        validateCollectionName(collection);
        HttpResponse<String> response = send("GET", collectionUrl(collection), null);
        if (response.statusCode() == 404) {
            log.info("Qdrant集合不存在，开始创建 collection={} vectorSize={} distance=Cosine", collection, properties.vectorSize());
            Map<String, Object> vectors = Map.of("size", properties.vectorSize(), "distance", "Cosine");
            HttpResponse<String> created = send("PUT", collectionUrl(collection), Map.of("vectors", vectors));
            if (!isSuccess(created.statusCode()) && created.statusCode() != 409) {
                throw responseError("创建Qdrant集合", created);
            }
            response = send("GET", collectionUrl(collection), null);
            log.info("Qdrant集合创建完成 collection={}", collection);
        }
        if (!isSuccess(response.statusCode())) throw responseError("读取Qdrant集合", response);

        try {
            JsonNode vectors = objectMapper.readTree(response.body()).path("result").path("config").path("params").path("vectors");
            int actualSize = vectors.path("size").asInt(-1);
            String actualDistance = vectors.path("distance").asText("");
            if (actualSize != properties.vectorSize() || !"Cosine".equalsIgnoreCase(actualDistance)) {
                throw new WritingConflictException("Qdrant集合 " + collection + " 配置不一致：当前为 "
                        + actualSize + "维/" + actualDistance + "，需要 " + properties.vectorSize() + "维/Cosine");
            }
        } catch (WritingConflictException error) {
            throw error;
        } catch (Exception error) {
            throw new VectorStoreException("无法解析Qdrant集合配置", error);
        }
    }

    private JsonNode sendJson(String method, String url, Object body, String operation) {
        HttpResponse<String> response = send(method, url, body);
        if (!isSuccess(response.statusCode())) throw responseError(operation, response);
        try {
            return objectMapper.readTree(response.body());
        } catch (Exception error) {
            throw new VectorStoreException(operation + "成功，但无法解析Qdrant响应", error);
        }
    }

    private HttpResponse<String> send(String method, String url, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(properties.timeout())
                    .header("Accept", "application/json");
            if (properties.hasApiKey()) builder.header("api-key", properties.apiKey());
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new VectorStoreException("Qdrant请求已取消", error);
        } catch (VectorStoreException error) {
            throw error;
        } catch (Exception error) {
            throw new VectorStoreException("暂时无法连接Qdrant：" + properties.baseUrl(), error);
        }
    }

    private void validateConfiguration() {
        validateCollectionName(properties.collection());
        validateCollectionName(properties.storyBibleCollection());
        if (properties.collection().equals(properties.storyBibleCollection())) {
            throw new WritingConflictException("章节记忆与资料库对话必须使用不同的Qdrant集合");
        }
    }

    private void validateCollectionName(String collection) {
        if (collection == null || !collection.matches("[A-Za-z0-9_-]{1,255}"))
            throw new WritingConflictException("Qdrant集合名只能包含字母、数字、下划线和连字符");
    }

    private void validateEmbeddingDimensions(EmbeddingResponse embedding) {
        validateConfiguration();
        if (embedding.dimensions() != properties.vectorSize()) {
            throw new WritingConflictException("Embedding返回 " + embedding.dimensions()
                    + " 维，但Qdrant配置为 " + properties.vectorSize() + " 维");
        }
    }

    private String collectionUrl() { return properties.baseUrl() + "/collections/" + properties.collection(); }
    private String collectionUrl(String collection) { return properties.baseUrl() + "/collections/" + collection; }
    private Map<String, Object> match(String key, Object value) { return Map.of("key", key, "match", Map.of("value", value)); }
    private long elapsedMs(long started) { return (System.nanoTime() - started) / 1_000_000; }
    private boolean isSuccess(int status) { return status >= 200 && status < 300; }
    private Long nullableLong(JsonNode node) { return node == null || node.isNull() || node.isMissingNode() ? null : node.asLong(); }
    private Integer nullableInteger(JsonNode node) { return node == null || node.isNull() || node.isMissingNode() ? null : node.asInt(); }

    private VectorStoreException responseError(String operation, HttpResponse<String> response) {
        return new VectorStoreException(operation + "失败，状态码：" + response.statusCode()
                + "，响应：" + safeMessage(response.body()));
    }

    private String safeMessage(String body) {
        if (body == null || body.isBlank()) return "无响应内容";
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }
}
