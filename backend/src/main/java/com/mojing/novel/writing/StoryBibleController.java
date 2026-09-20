package com.mojing.novel.writing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.mojing.novel.writing.AgentDtos.*;

@RestController
@RequestMapping("/api")
public class StoryBibleController {
    private final StoryBibleService service;
    private final NovelAgentService agentService;
    private final CharacterHistoryService characterHistoryService;
    private final StoryBibleChatService chatService;
    public StoryBibleController(StoryBibleService service, NovelAgentService agentService,
                                CharacterHistoryService characterHistoryService,
                                StoryBibleChatService chatService) {
        this.service = service;
        this.agentService = agentService;
        this.characterHistoryService = characterHistoryService;
        this.chatService = chatService;
    }

    @PostMapping("/novels/{novelId}/story-bible/assist")
    public StoryBibleAssistResponse assist(@PathVariable long novelId,
                                           @Valid @RequestBody StoryBibleAssistRequest request) {
        return agentService.assistStoryBible(novelId, request);
    }

    @GetMapping("/novels/{novelId}/story-bible/chat")
    public StoryBibleChatConversationResponse getChat(@PathVariable long novelId) {
        return chatService.conversation(novelId);
    }

    @PostMapping("/novels/{novelId}/story-bible/chat/messages")
    public StoryBibleChatConversationResponse chat(@PathVariable long novelId,
                                                    @Valid @RequestBody StoryBibleChatRequest request) {
        return chatService.chat(novelId, request);
    }

    @PostMapping("/story-bible/chat/messages/{messageId}/suggestions/{suggestionIndex}/apply")
    public StoryBibleApplySuggestionResponse applySuggestion(@PathVariable long messageId,
                                                              @PathVariable int suggestionIndex) {
        return chatService.apply(messageId, suggestionIndex);
    }

    @GetMapping("/novels/{novelId}/characters")
    public List<CharacterResponse> listCharacters(@PathVariable long novelId) { return service.listCharacters(novelId); }
    @PostMapping("/novels/{novelId}/characters") @ResponseStatus(HttpStatus.CREATED)
    public CharacterResponse createCharacter(@PathVariable long novelId, @Valid @RequestBody CharacterRequest request) { return service.createCharacter(novelId, request); }
    @PutMapping("/characters/{id}")
    public CharacterResponse updateCharacter(@PathVariable long id, @Valid @RequestBody CharacterRequest request) { return service.updateCharacter(id, request); }
    @DeleteMapping("/characters/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCharacter(@PathVariable long id) { service.deleteCharacter(id); }
    @GetMapping("/characters/{id}/history")
    public List<CharacterHistoryResponse> listCharacterHistory(@PathVariable long id) {
        return characterHistoryService.list(id);
    }

    @GetMapping("/novels/{novelId}/organizations")
    public List<OrganizationResponse> listOrganizations(@PathVariable long novelId) { return service.listOrganizations(novelId); }
    @PostMapping("/novels/{novelId}/organizations") @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createOrganization(@PathVariable long novelId, @Valid @RequestBody OrganizationRequest request) { return service.createOrganization(novelId, request); }
    @PutMapping("/organizations/{id}")
    public OrganizationResponse updateOrganization(@PathVariable long id, @Valid @RequestBody OrganizationRequest request) { return service.updateOrganization(id, request); }
    @DeleteMapping("/organizations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrganization(@PathVariable long id) { service.deleteOrganization(id); }

    @GetMapping("/novels/{novelId}/world-settings")
    public List<WorldSettingResponse> listWorldSettings(@PathVariable long novelId) { return service.listWorldSettings(novelId); }
    @PostMapping("/novels/{novelId}/world-settings") @ResponseStatus(HttpStatus.CREATED)
    public WorldSettingResponse createWorldSetting(@PathVariable long novelId, @Valid @RequestBody WorldSettingRequest request) { return service.createWorldSetting(novelId, request); }
    @PutMapping("/world-settings/{id}")
    public WorldSettingResponse updateWorldSetting(@PathVariable long id, @Valid @RequestBody WorldSettingRequest request) { return service.updateWorldSetting(id, request); }
    @DeleteMapping("/world-settings/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorldSetting(@PathVariable long id) { service.deleteWorldSetting(id); }

    @GetMapping("/novels/{novelId}/story-volumes")
    public List<StoryVolumeResponse> listStoryVolumes(@PathVariable long novelId) {
        return service.listStoryVolumes(novelId);
    }
    @PostMapping("/novels/{novelId}/story-volumes") @ResponseStatus(HttpStatus.CREATED)
    public StoryVolumeResponse createStoryVolume(@PathVariable long novelId,
                                                  @Valid @RequestBody StoryVolumeRequest request) {
        return service.createStoryVolume(novelId, request);
    }
    @PostMapping("/novels/{novelId}/story-volumes/generate")
    public StoryVolumeGenerateResponse generateStoryVolume(@PathVariable long novelId,
                                                            @Valid @RequestBody StoryVolumeGenerateRequest request) {
        return agentService.generateStoryVolume(novelId, request);
    }
    @PutMapping("/story-volumes/{id}")
    public StoryVolumeResponse updateStoryVolume(@PathVariable long id,
                                                  @Valid @RequestBody StoryVolumeRequest request) {
        return service.updateStoryVolume(id, request);
    }
    @DeleteMapping("/story-volumes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStoryVolume(@PathVariable long id) { service.deleteStoryVolume(id); }

    @GetMapping("/story-volumes/{volumeId}/parts")
    public List<StoryPartResponse> listStoryParts(@PathVariable long volumeId) {
        return service.listStoryParts(volumeId);
    }
    @PostMapping("/story-volumes/{volumeId}/parts") @ResponseStatus(HttpStatus.CREATED)
    public StoryPartResponse createStoryPart(@PathVariable long volumeId,
                                              @Valid @RequestBody StoryPartRequest request) {
        return service.createStoryPart(volumeId, request);
    }
    @PutMapping("/story-parts/{id}")
    public StoryPartResponse updateStoryPart(@PathVariable long id,
                                              @Valid @RequestBody StoryPartRequest request) {
        return service.updateStoryPart(id, request);
    }
    @DeleteMapping("/story-parts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStoryPart(@PathVariable long id) { service.deleteStoryPart(id); }
}
