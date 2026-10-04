package com.mojing.novel.writing;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.mojing.novel.writing.AgentDtos.*;

@RestController
@RequestMapping("/api/novels/{novelId}/project")
public class NovelProjectController {
    private final NovelProjectService projectService;
    private final NovelAgentService agentService;
    private final NovelProjectChatService chatService;
    private final NovelAccessService novelAccessService;

    public NovelProjectController(NovelProjectService projectService, NovelAgentService agentService,
                                  NovelProjectChatService chatService, NovelAccessService novelAccessService) {
        this.projectService = projectService;
        this.agentService = agentService;
        this.chatService = chatService;
        this.novelAccessService = novelAccessService;
    }

    @GetMapping
    public NovelProjectResponse get(@PathVariable long novelId) { novelAccessService.requireNovel(novelId); return projectService.get(novelId); }

    @PutMapping
    public NovelProjectResponse save(@PathVariable long novelId,
                                     @Valid @RequestBody NovelProjectSaveRequest request) {
        novelAccessService.requireNovel(novelId); return projectService.save(novelId, request);
    }

    @PostMapping("/generate")
    public NovelProjectGenerateResponse generate(@PathVariable long novelId,
                                                 @Valid @RequestBody NovelProjectGenerateRequest request) {
        novelAccessService.requireNovel(novelId); return agentService.generateNovelProject(novelId, request);
    }

    @GetMapping("/chat")
    public NovelProjectChatConversationResponse chat(@PathVariable long novelId) { novelAccessService.requireNovel(novelId); return chatService.conversation(novelId); }

    @PostMapping("/chat/messages")
    public NovelProjectChatConversationResponse chat(@PathVariable long novelId,
                                                      @Valid @RequestBody NovelProjectChatRequest request) {
        novelAccessService.requireNovel(novelId); return chatService.chat(novelId, request);
    }

    @PostMapping("/chat/messages/{messageId}/apply")
    public NovelProjectChatApplyResponse apply(@PathVariable long novelId, @PathVariable long messageId) {
        novelAccessService.requireNovel(novelId); return chatService.apply(novelId, messageId);
    }
}
