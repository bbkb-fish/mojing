package com.mojing.novel.writing;

import com.mojing.novel.qdrant.QdrantMemoryService;
import com.mojing.novel.style.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.mojing.novel.writing.WritingDtos.ChapterResponse;
import static com.mojing.novel.writing.WritingDtos.CreateChapterRequest;
import static com.mojing.novel.writing.WritingDtos.CreateNovelRequest;
import static com.mojing.novel.writing.WritingDtos.NovelResponse;
import static com.mojing.novel.writing.WritingDtos.UpdateChapterRequest;
import static com.mojing.novel.writing.WritingDtos.UpdateNovelRequest;

@Service
public class WritingService {
    private static final Logger log = LoggerFactory.getLogger(WritingService.class);
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final QdrantMemoryService qdrantMemoryService;
    private final CurrentUserProvider currentUserProvider;

    public WritingService(NovelRepository novelRepository, ChapterRepository chapterRepository,
                          QdrantMemoryService qdrantMemoryService,
                          CurrentUserProvider currentUserProvider) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.qdrantMemoryService = qdrantMemoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<NovelResponse> listNovels() {
        return novelRepository.findByOwnerUserIdOrderByUpdatedAtDesc(currentUserProvider.currentUserId())
                .stream().map(this::toNovelResponse).toList();
    }

    @Transactional(readOnly = true)
    public NovelResponse getNovel(long novelId) {
        return toNovelResponse(requireNovel(novelId));
    }

    @Transactional
    public NovelResponse createNovel(CreateNovelRequest request) {
        NovelEntity novel = new NovelEntity();
        novel.setOwnerUserId(currentUserProvider.currentUserId());
        novel.setTitle(request.title().trim());
        novel.setDescription(normalizeNullable(request.description()));
        novel.setOutline(normalizeNullable(request.outline()));
        return toNovelResponse(novelRepository.saveAndFlush(novel));
    }

    @Transactional
    public NovelResponse updateNovel(long novelId, UpdateNovelRequest request) {
        NovelEntity novel = requireNovel(novelId);
        checkVersion(novel.getVersion(), request.version(), "小说已在其他页面被修改，请刷新后重试");
        novel.setTitle(request.title().trim());
        novel.setDescription(normalizeNullable(request.description()));
        novel.setOutline(normalizeNullable(request.outline()));
        return toNovelResponse(novelRepository.saveAndFlush(novel));
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> listChapters(long novelId) {
        requireNovel(novelId);
        return chapterRepository.findByNovelIdOrderByChapterNoAsc(novelId)
                .stream().map(this::toChapterResponse).toList();
    }

    @Transactional(readOnly = true)
    public ChapterResponse getChapter(long chapterId) {
        return toChapterResponse(requireChapter(chapterId));
    }

    @Transactional
    public ChapterResponse createChapter(long novelId, CreateChapterRequest request) {
        NovelEntity novel = requireNovel(novelId);
        ChapterEntity chapter = new ChapterEntity();
        String content = normalizeContent(request.content());
        chapter.setNovelId(novelId);
        chapter.setChapterNo(request.chapterNo() == null
                ? chapterRepository.findMaxChapterNo(novelId) + 1
                : request.chapterNo());
        chapter.setTitle(request.title().trim());
        chapter.setContent(content);
        chapter.setWordCount(countWords(content));
        chapter.setStatus(content.isBlank() ? "UNPLANNED" : "WRITING");
        ChapterEntity saved = chapterRepository.saveAndFlush(chapter);
        refreshNovelWords(novel);
        return toChapterResponse(saved);
    }

    @Transactional
    public ChapterResponse updateChapter(long chapterId, UpdateChapterRequest request) {
        ChapterEntity chapter = requireChapter(chapterId);
        checkVersion(chapter.getVersion(), request.version(), "当前章节已在其他页面被修改，请刷新后重试");
        String content = normalizeContent(request.content());
        String title = request.title().trim();
        boolean contentChanged = !Objects.equals(chapter.getTitle(), title) || !Objects.equals(chapter.getContent(), content);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setWordCount(countWords(content));
        if (contentChanged) {
            chapter.setContentVersion(chapter.getContentVersion() + 1);
            chapter.setStatus(content.isBlank() && "PLANNED".equals(chapter.getStatus()) ? "PLANNED"
                    : content.isBlank() ? "UNPLANNED" : "WRITING");
            chapter.setCompletedAt(null);
        }
        ChapterEntity saved = chapterRepository.saveAndFlush(chapter);
        refreshNovelWords(requireNovel(chapter.getNovelId()));
        return toChapterResponse(saved);
    }

    @Transactional
    public void deleteChapter(long chapterId) {
        ChapterEntity chapter = requireChapter(chapterId);
        NovelEntity novel = requireNovel(chapter.getNovelId());
        chapterRepository.delete(chapter);
        chapterRepository.flush();
        refreshNovelWords(novel);
        try {
            qdrantMemoryService.deleteChapterMemory(chapter.getNovelId(), chapterId);
        } catch (RuntimeException error) {
            log.warn("章节已删除，但Qdrant向量清理失败 novelId={} chapterId={} reason={}",
                    chapter.getNovelId(), chapterId, error.getMessage());
        }
    }

    private NovelEntity requireNovel(long novelId) {
        return novelRepository.findByIdAndOwnerUserId(novelId, currentUserProvider.currentUserId())
                .orElseThrow(() -> new WritingNotFoundException("小说不存在或无权访问"));
    }

    private ChapterEntity requireChapter(long chapterId) {
        ChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new WritingNotFoundException("章节不存在或无权访问"));
        requireNovel(chapter.getNovelId());
        return chapter;
    }

    private void refreshNovelWords(NovelEntity novel) {
        novel.setTotalWords(Math.toIntExact(chapterRepository.sumWords(novel.getId())));
        novelRepository.saveAndFlush(novel);
    }

    private void checkVersion(Long currentVersion, Long requestVersion, String message) {
        if (!Objects.equals(currentVersion, requestVersion)) {
            throw new WritingConflictException(message);
        }
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    private int countWords(String content) {
        String compact = content.replaceAll("\\s", "");
        return compact.codePointCount(0, compact.length());
    }

    private NovelResponse toNovelResponse(NovelEntity novel) {
        return new NovelResponse(novel.getId(), novel.getTitle(), novel.getDescription(), novel.getOutline(), novel.getCoverUrl(),
                novel.getStatus(), novel.getTotalWords(), novel.getVersion(), novel.getCreatedAt(), novel.getUpdatedAt());
    }

    private ChapterResponse toChapterResponse(ChapterEntity chapter) {
        return new ChapterResponse(chapter.getId(), chapter.getNovelId(), chapter.getChapterNo(), chapter.getTitle(),
                chapter.getContent(), chapter.getWordCount(), normalizeChapterStatus(chapter), chapter.getContentVersion(),
                chapter.getCompletedAt(), chapter.getVersion(), chapter.getCreatedAt(), chapter.getUpdatedAt());
    }

    private String normalizeChapterStatus(ChapterEntity chapter) {
        if (chapter.getStatus() == null || "DRAFT".equals(chapter.getStatus()))
            return chapter.getContent().isBlank() ? "UNPLANNED" : "WRITING";
        return chapter.getStatus();
    }
}
