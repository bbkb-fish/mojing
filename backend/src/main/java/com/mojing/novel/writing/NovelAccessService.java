package com.mojing.novel.writing;

import com.mojing.novel.style.CurrentUserProvider;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NovelAccessService {
    private final EntityManager entityManager;
    private final CurrentUserProvider currentUserProvider;

    public NovelAccessService(EntityManager entityManager, CurrentUserProvider currentUserProvider) {
        this.entityManager = entityManager;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public void requireNovel(long id) { require("SELECT COUNT(*) FROM novel n WHERE n.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireChapter(long id) { require("SELECT COUNT(*) FROM chapter x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireNovelChapter(long novelId, long chapterId) {
        long count = ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM chapter x JOIN novel n ON n.id=x.novel_id WHERE n.id=:novelId AND x.id=:chapterId AND n.owner_user_id=:userId")
                .setParameter("novelId", novelId).setParameter("chapterId", chapterId)
                .setParameter("userId", currentUserProvider.currentUserId()).getSingleResult()).longValue();
        if (count == 0) denied();
    }
    @Transactional(readOnly = true)
    public void requireRun(long id) { require("SELECT COUNT(*) FROM agent_run x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireDirectorMessage(long id) { require("SELECT COUNT(*) FROM chapter_director_message x JOIN chapter_director_session s ON s.id=x.session_id JOIN novel n ON n.id=s.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireRevision(long id) { require("SELECT COUNT(*) FROM chapter_revision x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireConsistencyIssue(long id) { require("SELECT COUNT(*) FROM consistency_issue x JOIN consistency_report r ON r.id=x.report_id JOIN novel n ON n.id=r.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireChapterMemory(long id) { require("SELECT COUNT(*) FROM chapter_memory x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireEvalCase(long id) { require("SELECT COUNT(*) FROM eval_case x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireStoryBibleMessage(long id) { require("SELECT COUNT(*) FROM story_bible_chat_message x JOIN story_bible_chat_session s ON s.id=x.session_id JOIN novel n ON n.id=s.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireCharacter(long id) { require("SELECT COUNT(*) FROM story_character x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireOrganization(long id) { require("SELECT COUNT(*) FROM story_organization x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireWorldSetting(long id) { require("SELECT COUNT(*) FROM world_setting x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireStoryVolume(long id) { require("SELECT COUNT(*) FROM story_volume x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }
    @Transactional(readOnly = true)
    public void requireStoryPart(long id) { require("SELECT COUNT(*) FROM story_part x JOIN novel n ON n.id=x.novel_id WHERE x.id=:id AND n.owner_user_id=:userId", id); }

    private void require(String sql, long id) {
        long count = ((Number) entityManager.createNativeQuery(sql).setParameter("id", id)
                .setParameter("userId", currentUserProvider.currentUserId()).getSingleResult()).longValue();
        if (count == 0) denied();
    }

    private void denied() { throw new WritingNotFoundException("资源不存在或无权访问"); }
}
