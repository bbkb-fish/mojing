package com.mojing.novel.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.completion.AiProviderException;
import com.mojing.novel.config.EmbeddingProperties;
import com.mojing.novel.writing.WritingConflictException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mojing.novel.embedding.EmbeddingDtos.*;

@Service
public class EmbeddingService {
    private static final int PROVIDER_BATCH_LIMIT = 10;
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmbeddingService(EmbeddingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    public EmbeddingResponse embed(String text) {
        BatchEmbeddingResponse batch = embedBatch(List.of(text));
        return new EmbeddingResponse(batch.model(), batch.dimensions(), batch.vectors().getFirst(),
                batch.promptTokens(), batch.totalTokens());
    }

    public BatchEmbeddingResponse embedBatch(List<String> texts) {
        if (!properties.configured()) {
            throw new WritingConflictException("尚未配置 EMBEDDING_API_KEY，无法调用向量模型");
        }
        if (texts == null || texts.isEmpty()) throw new WritingConflictException("批量向量化文本不能为空");
        if (texts.size() > 64) throw new WritingConflictException("单次批量向量化不能超过64段文本");
        long started = System.nanoTime();
        int batchCount = (texts.size() + PROVIDER_BATCH_LIMIT - 1) / PROVIDER_BATCH_LIMIT;
        log.info("开始批量Embedding model={} count={} batches={} batchLimit={} dimensions={}",
                properties.model(), texts.size(), batchCount, PROVIDER_BATCH_LIMIT, properties.dimensions());
        try {
            List<String> normalized = texts.stream().map(text -> text == null ? "" : text.trim()).toList();
            if (normalized.stream().anyMatch(String::isBlank)) throw new WritingConflictException("批量向量化文本不能包含空内容");
            List<List<String>> providerBatches = partitionForProvider(normalized);
            List<List<Double>> vectors = new ArrayList<>(normalized.size());
            int promptTokens = 0;
            int totalTokens = 0;
            String responseModel = properties.model();
            for (int index = 0; index < providerBatches.size(); index++) {
                int batchNo = index + 1;
                int offset = index * PROVIDER_BATCH_LIMIT;
                List<String> batchTexts = providerBatches.get(index);
                log.info("调用Embedding分批请求 batch={}/{} offset={} count={}", batchNo, batchCount, offset, batchTexts.size());
                ProviderBatch batch = requestBatch(batchTexts, batchNo, batchCount);
                vectors.addAll(batch.vectors());
                promptTokens += batch.promptTokens();
                totalTokens += batch.totalTokens();
                responseModel = batch.model();
            }
            BatchEmbeddingResponse result = new BatchEmbeddingResponse(responseModel, properties.dimensions(),
                    List.copyOf(vectors), promptTokens, totalTokens);
            log.info("批量Embedding完成 model={} count={} batches={} dimensions={} promptTokens={} totalTokens={} elapsedMs={}",
                    result.model(), result.vectors().size(), batchCount, result.dimensions(), result.promptTokens(),
                    result.totalTokens(), elapsedMs(started));
            return result;
        } catch (AiProviderException | WritingConflictException error) {
            log.warn("批量Embedding失败 model={} count={} elapsedMs={} reason={}", properties.model(), texts.size(),
                    elapsedMs(started), error.getMessage());
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            log.warn("批量Embedding已取消 model={} count={} elapsedMs={}", properties.model(), texts.size(), elapsedMs(started));
            throw new AiProviderException("Embedding请求已取消", error);
        } catch (Exception error) {
            log.warn("批量Embedding连接失败 model={} count={} elapsedMs={} errorType={}", properties.model(), texts.size(),
                    elapsedMs(started), error.getClass().getSimpleName());
            throw new AiProviderException("暂时无法连接Embedding服务", error);
        }
    }

    private ProviderBatch requestBatch(List<String> texts, int batchNo, int batchCount) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.model());
        payload.put("input", texts);
        payload.put("dimensions", properties.dimensions());
        payload.put("encoding_format", "float");

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.embeddingsUrl()))
                .timeout(properties.timeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiProviderException("Embedding服务第 " + batchNo + "/" + batchCount
                    + " 批调用失败，状态码：" + response.statusCode() + "，响应：" + safeMessage(response.body()));
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.path("data");
        if (!data.isArray() || data.size() != texts.size()) {
            throw new AiProviderException("Embedding第 " + batchNo + "/" + batchCount + " 批返回向量数量为 "
                    + data.size() + "，预期 " + texts.size());
        }
        List<IndexedVector> indexed = new ArrayList<>(data.size());
        for (int position = 0; position < data.size(); position++) {
            JsonNode item = data.path(position);
            JsonNode embeddingNode = item.path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) throw new AiProviderException("Embedding服务没有返回向量");
            List<Double> vector = new ArrayList<>(embeddingNode.size());
            embeddingNode.forEach(value -> vector.add(value.asDouble()));
            if (vector.size() != properties.dimensions()) {
                throw new AiProviderException("Embedding返回维度为 " + vector.size()
                        + "，与配置的 " + properties.dimensions() + " 维不一致");
            }
            indexed.add(new IndexedVector(item.path("index").asInt(position), List.copyOf(vector)));
        }
        indexed.sort(Comparator.comparingInt(IndexedVector::index));
        JsonNode usage = root.path("usage");
        return new ProviderBatch(root.path("model").asText(properties.model()),
                indexed.stream().map(IndexedVector::vector).toList(),
                usage.path("prompt_tokens").asInt(0), usage.path("total_tokens").asInt(0));
    }

    static List<List<String>> partitionForProvider(List<String> texts) {
        List<List<String>> batches = new ArrayList<>();
        for (int offset = 0; offset < texts.size(); offset += PROVIDER_BATCH_LIMIT) {
            batches.add(List.copyOf(texts.subList(offset, Math.min(texts.size(), offset + PROVIDER_BATCH_LIMIT))));
        }
        return List.copyOf(batches);
    }

    private record IndexedVector(int index, List<Double> vector) {}
    private record ProviderBatch(String model, List<List<Double>> vectors, int promptTokens, int totalTokens) {}
    private long elapsedMs(long started) { return (System.nanoTime() - started) / 1_000_000; }

    private String safeMessage(String body) {
        if (body == null || body.isBlank()) return "无响应内容";
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }
}
