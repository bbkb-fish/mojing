package com.mojing.novel.qdrant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.mojing.novel.qdrant.VectorMemoryDtos.*;

@RestController
@RequestMapping("/api/vector-memories")
public class VectorMemoryController {
    private final QdrantMemoryService service;

    public VectorMemoryController(QdrantMemoryService service) {
        this.service = service;
    }

    @PostMapping("/initialize")
    public CollectionStatusResponse initialize() {
        return service.initializeCollection();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemoryWriteResponse upsert(@Valid @RequestBody UpsertMemoryRequest request) {
        return service.upsert(request);
    }

    @PostMapping("/search")
    public MemorySearchResponse search(@Valid @RequestBody SearchMemoryRequest request) {
        return service.search(request);
    }
}
