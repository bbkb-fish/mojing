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

    public NovelProjectController(NovelProjectService projectService, NovelAgentService agentService,
                                  NovelProjectChatService chatService) {
        this.projectService = projectService;
        this.agentService = agentService;
        this.chatService = chatService;
    }

    @GetMapping
    public NovelProjectResponse get(@PathVariable long novelId) { return projectService.get(novelId); }

    @PutMapping
    public NovelProjectResponse save(@PathVariable long novelId,
                                     @Valid @RequestBody NovelProjectSaveRequest request) {
        return projectService.save(novelId, request);
    }

    @PostMapping("/generate")
    public NovelProjectGenerateResponse generate(@PathVariable long novelId,
                                                 @Valid @RequestBody NovelProjectGenerateRequest request) {
        return agentService.generateNovelProject(novelId, request);
    }

    @GetMapping("/chat")
    public NovelProjectChatConversationResponse chat(@PathVariable long novelId) { return chatService.conversation(novelId); }

    @PostMapping("/chat/messages")
    public NovelProjectChatConversationResponse chat(@PathVariable long novelId,
                                                      @Valid @RequestBody NovelProjectChatRequest request) {
        return chatService.chat(novelId, request);
    }

    @PostMapping("/chat/messages/{messageId}/apply")
    public NovelProjectChatApplyResponse apply(@PathVariable long novelId, @PathVariable long messageId) {
        return chatService.apply(novelId, messageId);
    }
}
