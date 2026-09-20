package com.mojing.novel.writing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojing.novel.completion.CompletionRequest;
import com.mojing.novel.completion.CompletionResponse;
import com.mojing.novel.completion.NovelCompletionService;
import com.mojing.novel.config.AiProperties;
import com.mojing.novel.config.AiHttpClientFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static com.mojing.novel.writing.EvalDtos.*;

@Service
public class EvalService {
    private static final String GENERATOR_PROMPT_VERSION="quick-v1";
    private static final String JUDGE_PROMPT_VERSION="novel-judge-v1";
    private final EvalCaseRepository caseRepository;
    private final EvalRunRepository runRepository;
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final NovelCompletionService completionService;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EvalService(EvalCaseRepository caseRepository,EvalRunRepository runRepository,
                       NovelRepository novelRepository,ChapterRepository chapterRepository,
                       NovelCompletionService completionService,AiProperties properties,ObjectMapper objectMapper){
        this.caseRepository=caseRepository;this.runRepository=runRepository;this.novelRepository=novelRepository;
        this.chapterRepository=chapterRepository;this.completionService=completionService;this.properties=properties;
        this.objectMapper=objectMapper;this.httpClient=AiHttpClientFactory.create(properties);
    }

    @Transactional(readOnly=true)
    public List<EvalCaseResponse> listCases(long novelId){
        requireNovel(novelId);
        return caseRepository.findByNovelIdOrderByUpdatedAtDesc(novelId).stream().map(this::toCaseResponse).toList();
    }

    @Transactional
    public EvalCaseResponse createCase(long novelId,SaveCaseRequest request){
        requireNovel(novelId);validateChapter(novelId,request.chapterId());validateLengths(request);
        EvalCaseEntity entity=new EvalCaseEntity();entity.setNovelId(novelId);apply(entity,request);
        return toCaseResponse(caseRepository.saveAndFlush(entity));
    }

    @Transactional
    public EvalCaseResponse updateCase(long caseId,SaveCaseRequest request){
        EvalCaseEntity entity=requireCase(caseId);
        if(request.version()==null||!Objects.equals(request.version(),entity.getVersion())) throw new WritingConflictException("评测用例已变化，请刷新后重试");
        validateChapter(entity.getNovelId(),request.chapterId());validateLengths(request);apply(entity,request);
        return toCaseResponse(caseRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteCase(long caseId){caseRepository.delete(requireCase(caseId));}

    @Transactional
    public EvalRunResponse runCase(long caseId,RunCaseRequest request){
        EvalCaseEntity testCase=requireCase(caseId);
        if(!Boolean.TRUE.equals(testCase.getEnabled())) throw new WritingConflictException("该评测用例已停用");
        long started=System.nanoTime();
        EvalRunEntity run=new EvalRunEntity();run.setCaseId(caseId);run.setStatus("RUNNING");
        run.setGeneratorPromptVersion(GENERATOR_PROMPT_VERSION);run.setJudgePromptVersion(JUDGE_PROMPT_VERSION);
        run.setGeneratorModel(properties.configured()?properties.model():"demo");runRepository.saveAndFlush(run);
        try{
            CompletionResponse generated=completionService.complete(new CompletionRequest(null,null,testCase.getInputContext(),testCase.getMaxLength(),testCase.getInstruction(),null,null,false));
            run.setGeneratedText(generated.completion());run.setGeneratorModel(generated.model());
            run.setPromptTokens(generated.promptTokens());run.setCompletionTokens(generated.completionTokens());run.setTotalTokens(generated.totalTokens());
            RuleResult rules=evaluateRules(testCase,generated.completion());run.setRuleScore(rules.score());
            run.setViolationsJson(writeJson(rules.violations()));
            JudgeResult judge=request.normalizedUseJudge()?judge(testCase,generated.completion(),rules):new JudgeResult(rules.score(),"已跳过 LLM Judge，仅使用确定性规则评分。",0,0,0);
            run.setJudgeScore(judge.score());run.setJudgeFeedback(judge.feedback());
            run.setPromptTokens(run.getPromptTokens()+judge.promptTokens());run.setCompletionTokens(run.getCompletionTokens()+judge.completionTokens());run.setTotalTokens(run.getTotalTokens()+judge.totalTokens());
            int overall=(int)Math.round(rules.score()*0.6+judge.score()*0.4);run.setOverallScore(overall);
            run.setPassed(overall>=75&&rules.blockingViolations()==0);run.setStatus("COMPLETED");run.setDurationMs(elapsed(started));
        }catch(Exception error){
            run.setStatus("FAILED");run.setPassed(false);run.setDurationMs(elapsed(started));run.setErrorMessage(limit(error.getMessage(),2000));
        }
        return toRunResponse(runRepository.saveAndFlush(run));
    }

    @Transactional(readOnly=true)
    public List<EvalRunResponse> listRuns(long caseId){requireCase(caseId);return runRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(this::toRunResponse).toList();}

    @Transactional
    public List<EvalRunResponse> runAll(long novelId,RunCaseRequest request){
        requireNovel(novelId);
        return caseRepository.findByNovelIdOrderByUpdatedAtDesc(novelId).stream()
                .filter(item->Boolean.TRUE.equals(item.getEnabled()))
                .map(item->runCase(item.getId(),request)).toList();
    }

    private RuleResult evaluateRules(EvalCaseEntity testCase,String text){
        List<String> violations=new ArrayList<>();int blocking=0;int length=text.replaceAll("\\s","").length();
        if(length<testCase.getMinLength()){violations.add("生成内容过短："+length+" 字，最低要求 "+testCase.getMinLength()+" 字");blocking++;}
        if(length>testCase.getMaxLength()){violations.add("生成内容过长："+length+" 字，最高要求 "+testCase.getMaxLength()+" 字");blocking++;}
        for(String name:splitTerms(testCase.getExpectedCharacters())) if(!text.contains(name)){violations.add("预期人物未出现："+name);blocking++;}
        for(String term:splitTerms(testCase.getRequiredTerms())) if(!text.contains(term)){violations.add("缺少必需内容："+term);blocking++;}
        for(String term:splitTerms(testCase.getForbiddenTerms())) if(text.contains(term)){violations.add("出现禁用内容："+term);blocking++;}
        String compact=text.replaceAll("\\s+","");String context=testCase.getInputContext().replaceAll("\\s+","");
        if(compact.length()>=30&&context.contains(compact)){violations.add("生成结果大段重复了输入原文");blocking++;}
        return new RuleResult(Math.max(0,100-violations.size()*18),violations,blocking);
    }

    private JudgeResult judge(EvalCaseEntity testCase,String generated,RuleResult rules)throws Exception{
        if(!properties.configured()){
            int score=Math.max(0,Math.min(100,rules.score()-2));
            return new JudgeResult(score,"演示模式：依据长度、人物和关键词约束给出模拟 Judge 评分。配置 API Key 后将评估连贯性、文风和指令遵循。",0,0,0);
        }
        String system="""
                你是网络小说生成质量评测器。请从上下文连贯性、人物与设定一致性、语言自然度、情节推进、作者指令遵循五方面评分。
                只输出合法 JSON：{\"score\":0到100整数,\"feedback\":\"简洁总结\",\"issues\":[\"具体问题\"]}。不要因为个人文风偏好过度扣分。
                """;
        String user="【输入上下文】\n"+testCase.getInputContext()+"\n【作者要求】\n"+nullToEmpty(testCase.getInstruction())+
                "\n【生成结果】\n"+generated+"\n【规则检查】\n"+String.join("；",rules.violations());
        Map<String,Object> payload=Map.of("model",properties.model(),"temperature",0.1,"max_tokens",800,
                "messages",List.of(Map.of("role","system","content",system),Map.of("role","user","content",user)));
        HttpRequest httpRequest=HttpRequest.newBuilder(URI.create(properties.chatCompletionsUrl())).timeout(properties.timeout())
                .header("Authorization","Bearer "+properties.apiKey()).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload),StandardCharsets.UTF_8)).build();
        HttpResponse<String> response=httpClient.send(httpRequest,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Judge 调用失败，状态码："+response.statusCode());
        JsonNode root=objectMapper.readTree(response.body());JsonNode usage=root.path("usage");
        String content=extractContent(root.path("choices").path(0).path("message").path("content"));
        JudgeOutput output=objectMapper.readValue(extractJson(content),JudgeOutput.class);
        String feedback=output.feedback()+(output.issues()==null||output.issues().isEmpty()?"":" 问题："+String.join("；",output.issues()));
        return new JudgeResult(output.score(),feedback,usage.path("prompt_tokens").asInt(0),usage.path("completion_tokens").asInt(0),usage.path("total_tokens").asInt(0));
    }

    private void apply(EvalCaseEntity entity,SaveCaseRequest r){entity.setChapterId(r.chapterId());entity.setName(r.name().trim());entity.setInputContext(r.inputContext().trim());entity.setInstruction(r.normalized(r.instruction()));entity.setExpectedCharacters(r.normalized(r.expectedCharacters()));entity.setRequiredTerms(r.normalized(r.requiredTerms()));entity.setForbiddenTerms(r.normalized(r.forbiddenTerms()));entity.setMinLength(r.normalizedMinLength());entity.setMaxLength(r.normalizedMaxLength());entity.setEnabled(r.normalizedEnabled());}
    private void validateLengths(SaveCaseRequest r){if(r.normalizedMinLength()>r.normalizedMaxLength())throw new WritingConflictException("最低长度不能大于最高长度");}
    private void validateChapter(long novelId,Long chapterId){if(chapterId==null)return;ChapterEntity c=chapterRepository.findById(chapterId).orElseThrow(()->new WritingNotFoundException("章节不存在："+chapterId));if(!c.getNovelId().equals(novelId))throw new WritingConflictException("章节不属于当前小说");}
    private void requireNovel(long id){if(!novelRepository.existsById(id))throw new WritingNotFoundException("小说不存在："+id);}
    private EvalCaseEntity requireCase(long id){return caseRepository.findById(id).orElseThrow(()->new WritingNotFoundException("评测用例不存在："+id));}
    private List<String> splitTerms(String value){if(value==null||value.isBlank())return List.of();return Arrays.stream(value.split("[,，;；\\n]")).map(String::trim).filter(s->!s.isBlank()).distinct().toList();}
    private String writeJson(Object value){try{return objectMapper.writeValueAsString(value);}catch(Exception e){return "[]";}}
    private List<String> readViolations(String value){try{return value==null?List.of():objectMapper.readValue(value,objectMapper.getTypeFactory().constructCollectionType(List.class,String.class));}catch(Exception e){return List.of("评测结果读取失败");}}
    private EvalCaseResponse toCaseResponse(EvalCaseEntity e){EvalRunResponse latest=runRepository.findByCaseIdOrderByCreatedAtDesc(e.getId()).stream().findFirst().map(this::toRunResponse).orElse(null);return new EvalCaseResponse(e.getId(),e.getNovelId(),e.getChapterId(),e.getName(),e.getInputContext(),e.getInstruction(),e.getExpectedCharacters(),e.getRequiredTerms(),e.getForbiddenTerms(),e.getMinLength(),e.getMaxLength(),e.getEnabled(),e.getVersion(),e.getCreatedAt(),e.getUpdatedAt(),latest);}
    private EvalRunResponse toRunResponse(EvalRunEntity r){return new EvalRunResponse(r.getId(),r.getCaseId(),r.getStatus(),r.getGeneratorModel(),r.getGeneratorPromptVersion(),r.getJudgePromptVersion(),r.getGeneratedText(),r.getRuleScore(),r.getJudgeScore(),r.getOverallScore(),r.getPassed(),readViolations(r.getViolationsJson()),r.getJudgeFeedback(),r.getPromptTokens(),r.getCompletionTokens(),r.getTotalTokens(),r.getDurationMs(),r.getErrorMessage(),r.getCreatedAt());}
    private String extractContent(JsonNode node){if(node.isTextual())return node.asText();if(!node.isArray())return "";StringBuilder b=new StringBuilder();for(JsonNode p:node){if(p.isTextual())b.append(p.asText());else if(p.path("text").isTextual())b.append(p.path("text").asText());}return b.toString();}
    private String extractJson(String text){int start=text.indexOf('{'),end=text.lastIndexOf('}');if(start<0||end<start)throw new IllegalStateException("Judge 未返回合法 JSON");return text.substring(start,end+1);}
    private String nullToEmpty(String v){return v==null?"":v;} private long elapsed(long s){return Duration.ofNanos(System.nanoTime()-s).toMillis();} private String limit(String s,int n){if(s==null)return "未知错误";return s.length()<=n?s:s.substring(0,n);}
    private record RuleResult(int score,List<String> violations,int blockingViolations){} private record JudgeResult(int score,String feedback,int promptTokens,int completionTokens,int totalTokens){}
}
