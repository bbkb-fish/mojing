package com.mojing.novel.writing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.mojing.novel.writing.WritingDtos.ChapterResponse;
import static com.mojing.novel.writing.WritingDtos.CreateChapterRequest;
import static com.mojing.novel.writing.WritingDtos.CreateNovelRequest;
import static com.mojing.novel.writing.WritingDtos.NovelResponse;
import static com.mojing.novel.writing.WritingDtos.UpdateChapterRequest;
import static com.mojing.novel.writing.WritingDtos.UpdateNovelRequest;

@RestController
@RequestMapping("/api")
public class WritingController {
    private final WritingService writingService;

    public WritingController(WritingService writingService) {
        this.writingService = writingService;
    }

    @GetMapping("/novels")
    public List<NovelResponse> listNovels() {
        return writingService.listNovels();
    }

    @PostMapping("/novels")
    @ResponseStatus(HttpStatus.CREATED)
    public NovelResponse createNovel(@Valid @RequestBody CreateNovelRequest request) {
        return writingService.createNovel(request);
    }

    @GetMapping("/novels/{novelId}")
    public NovelResponse getNovel(@PathVariable long novelId) {
        return writingService.getNovel(novelId);
    }

    @PatchMapping("/novels/{novelId}")
    public NovelResponse updateNovel(@PathVariable long novelId, @Valid @RequestBody UpdateNovelRequest request) {
        return writingService.updateNovel(novelId, request);
    }

    @GetMapping("/novels/{novelId}/chapters")
    public List<ChapterResponse> listChapters(@PathVariable long novelId) {
        return writingService.listChapters(novelId);
    }

    @PostMapping("/novels/{novelId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public ChapterResponse createChapter(@PathVariable long novelId, @Valid @RequestBody CreateChapterRequest request) {
        return writingService.createChapter(novelId, request);
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterResponse getChapter(@PathVariable long chapterId) {
        return writingService.getChapter(chapterId);
    }

    @PutMapping("/chapters/{chapterId}")
    public ChapterResponse updateChapter(@PathVariable long chapterId, @Valid @RequestBody UpdateChapterRequest request) {
        return writingService.updateChapter(chapterId, request);
    }

    @DeleteMapping("/chapters/{chapterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChapter(@PathVariable long chapterId) {
        writingService.deleteChapter(chapterId);
    }
}
