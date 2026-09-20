package com.mojing.novel.style;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.mojing.novel.style.WritingStyleDtos.*;

@RestController
@RequestMapping("/api/writing-styles")
public class WritingStyleController {
    private final WritingStyleService service;

    public WritingStyleController(WritingStyleService service) { this.service = service; }

    @GetMapping
    public List<StyleResponse> list(@RequestParam(required = false) Long novelId) { return service.list(novelId); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public StyleResponse create(@Valid @RequestBody SaveStyleRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    public StyleResponse update(@PathVariable long id, @Valid @RequestBody SaveStyleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }

    @PostMapping("/assist")
    public AssistStyleResponse assist(@Valid @RequestBody AssistStyleRequest request) { return service.assist(request); }
}
