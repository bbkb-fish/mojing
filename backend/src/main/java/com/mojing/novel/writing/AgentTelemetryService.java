package com.mojing.novel.writing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentTelemetryService {
    private final AgentRunRepository runRepository;
    private final AiCallLogRepository callRepository;

    public AgentTelemetryService(AgentRunRepository runRepository, AiCallLogRepository callRepository) {
        this.runRepository = runRepository;
        this.callRepository = callRepository;
    }

    @Transactional
    public long startRun(long novelId, long chapterId, String operation, String guidance,
                         String providerMode, String model) {
        AgentRunEntity run = new AgentRunEntity();
        run.setNovelId(novelId); run.setChapterId(chapterId); run.setOperation(operation);
        run.setGuidance(guidance); run.setProviderMode(providerMode); run.setModel(model);
        run.setStatus("RUNNING"); run.setCurrentStep(operation); run.setTotalDurationMs(0L);
        return runRepository.saveAndFlush(run).getId();
    }

    @Transactional
    public void recordCall(long runId, String stepType, String model, String status,
                           int promptTokens, int completionTokens, int totalTokens,
                           long durationMs, int retryCount, String errorMessage) {
        AiCallLogEntity call = new AiCallLogEntity();
        call.setRunId(runId); call.setStepType(stepType); call.setModel(model); call.setStatus(status);
        call.setPromptTokens(promptTokens); call.setCompletionTokens(completionTokens); call.setTotalTokens(totalTokens);
        call.setDurationMs(durationMs); call.setRetryCount(retryCount); call.setErrorMessage(limit(errorMessage));
        callRepository.saveAndFlush(call);
    }

    @Transactional
    public void finishRun(long runId, String currentStep, long durationMs) {
        AgentRunEntity run = requireRun(runId);
        run.setStatus("COMPLETED"); run.setCurrentStep(currentStep); run.setTotalDurationMs(durationMs);
        run.setErrorMessage(null); runRepository.saveAndFlush(run);
    }

    @Transactional
    public void failRun(long runId, String currentStep, long durationMs, String errorMessage) {
        AgentRunEntity run = requireRun(runId);
        run.setStatus("FAILED"); run.setCurrentStep(currentStep); run.setTotalDurationMs(durationMs);
        run.setErrorMessage(limit(errorMessage)); runRepository.saveAndFlush(run);
    }

    private AgentRunEntity requireRun(long id) {
        return runRepository.findById(id).orElseThrow(() -> new WritingNotFoundException("Agent任务不存在：" + id));
    }

    private String limit(String value) {
        if (value == null || value.isBlank()) return null;
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
