package com.mojing.novel.writing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.mojing.novel.writing.EvalDtos.*;

@RestController
@RequestMapping("/api/evals")
public class EvalController {
    private final EvalService service;
    private final NovelAccessService novelAccessService;
    public EvalController(EvalService service, NovelAccessService novelAccessService){this.service=service;this.novelAccessService=novelAccessService;}

    @GetMapping("/novels/{novelId}/cases") public List<EvalCaseResponse> list(@PathVariable long novelId){novelAccessService.requireNovel(novelId);return service.listCases(novelId);}
    @PostMapping("/novels/{novelId}/cases") @ResponseStatus(HttpStatus.CREATED)
    public EvalCaseResponse create(@PathVariable long novelId,@Valid @RequestBody SaveCaseRequest request){novelAccessService.requireNovel(novelId);return service.createCase(novelId,request);}
    @PutMapping("/cases/{caseId}") public EvalCaseResponse update(@PathVariable long caseId,@Valid @RequestBody SaveCaseRequest request){novelAccessService.requireEvalCase(caseId);return service.updateCase(caseId,request);}
    @DeleteMapping("/cases/{caseId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable long caseId){novelAccessService.requireEvalCase(caseId);service.deleteCase(caseId);}
    @PostMapping("/cases/{caseId}/runs") public EvalRunResponse run(@PathVariable long caseId,@RequestBody(required=false) RunCaseRequest request){novelAccessService.requireEvalCase(caseId);return service.runCase(caseId,request==null?new RunCaseRequest(true):request);}
    @GetMapping("/cases/{caseId}/runs") public List<EvalRunResponse> runs(@PathVariable long caseId){novelAccessService.requireEvalCase(caseId);return service.listRuns(caseId);}
    @PostMapping("/novels/{novelId}/runs") public List<EvalRunResponse> runAll(@PathVariable long novelId,@RequestBody(required=false) RunCaseRequest request){novelAccessService.requireNovel(novelId);return service.runAll(novelId,request==null?new RunCaseRequest(true):request);}
}
