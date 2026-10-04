package com.mojing.novel.style;

import com.mojing.novel.writing.NovelAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.mojing.novel.style.WritingStyleDtos.*;

@RestController
@RequestMapping("/api/writing-styles")
public class WritingStyleController {
    private final WritingStyleService service;
    private final NovelAccessService novelAccessService;

    public WritingStyleController(WritingStyleService service, NovelAccessService novelAccessService) {
        this.service = service;
        this.novelAccessService = novelAccessService;
    }

    @GetMapping
    public List<StyleResponse> list(@RequestParam(required = false) Long novelId) {
        requireNovel(novelId); return service.list(novelId);
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public StyleResponse create(@Valid @RequestBody SaveStyleRequest request) {
        requireNovel(request.novelId()); return service.create(request);
    }

    @PutMapping("/{id}")
    public StyleResponse update(@PathVariable long id, @Valid @RequestBody SaveStyleRequest request) {
        requireNovel(request.novelId());
        return service.update(id, request);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }

    @PostMapping("/assist")
    public AssistStyleResponse assist(@Valid @RequestBody AssistStyleRequest request) {
        requireNovel(request.novelId()); return service.assist(request);
    }

    private void requireNovel(Long novelId) { if (novelId != null) novelAccessService.requireNovel(novelId); }
}
