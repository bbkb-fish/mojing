package com.mojing.novel.embedding;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import static com.mojing.novel.embedding.EmbeddingDtos.*;

@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {
    private final EmbeddingService service;
    public EmbeddingController(EmbeddingService service) { this.service = service; }

    @PostMapping("/test")
    public EmbeddingResponse test(@Valid @RequestBody EmbeddingRequest request) {
        return service.embed(request.text());
    }
}
