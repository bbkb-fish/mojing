package com.mojing.novel.qdrant;

import com.mojing.novel.writing.NovelAccessService;
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
    private final NovelAccessService novelAccessService;

    public VectorMemoryController(QdrantMemoryService service, NovelAccessService novelAccessService) {
        this.service = service;
        this.novelAccessService = novelAccessService;
    }

    @PostMapping("/initialize")
    public CollectionStatusResponse initialize() {
        return service.initializeCollection();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemoryWriteResponse upsert(@Valid @RequestBody UpsertMemoryRequest request) {
        if (request.chapterId() == null) novelAccessService.requireNovel(request.novelId());
        else novelAccessService.requireNovelChapter(request.novelId(), request.chapterId());
        return service.upsert(request);
    }

    @PostMapping("/search")
    public MemorySearchResponse search(@Valid @RequestBody SearchMemoryRequest request) {
        novelAccessService.requireNovel(request.novelId());
        if (request.excludeChapterId() != null) novelAccessService.requireChapter(request.excludeChapterId());
        return service.search(request);
    }
}
