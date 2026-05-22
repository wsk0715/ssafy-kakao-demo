package com.example.demo_app.api;

import com.example.demo_app.api.dto.*;
import com.example.demo_app.domain.logs.TrainingLogService;
import com.example.demo_app.domain.scenarios.ScenarioService;
import com.example.demo_app.domain.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PhishingApiController implements PhishingApi {

    private final UserService userService;
    private final ScenarioService scenarioService;
    private final TrainingLogService trainingLogService;

    @Override
    public String saveProfiling(String userId, UserProfilingRequest request) {
        log.info("API: Save profiling for user: {}", userId);
        return userService.saveProfiling(userId, request);
    }

    @Override
    public List<Map<String, Object>> getScenarios() {
        log.info("API: Fetching all scenarios");
        return scenarioService.getScenarios();
    }

    @Override
    public Map<String, Object> getScenarioById(String id) {
        log.info("API: Fetching scenario by id: {}", id);
        return scenarioService.getScenarioById(id);
    }

    @Override
    public Map<String, Object> recordTrainingLog(String userId, LogPayloadRequest request) {
        log.info("API: Recording training log for user: {}", userId);
        return trainingLogService.recordTrainingLog(userId, request);
    }

    @Override
    public Map<String, Object> getMonthlyReport(String userId) {
        log.info("API: Generating monthly report for user: {}", userId);
        return trainingLogService.getMonthlyReport(userId);
    }

    @Override
    public List<ProfilingQuestion> getQuestions() {
        log.info("API: Fetching profiling questions");
        return userService.getQuestions();
    }

    @Override
    public ProfilingResultResponse analyzeRisk(String userId, Map<String, String> answers) {
        log.info("API: Analyzing risk for user: {}", userId);
        return userService.analyzeRisk(userId, answers);
    }
}
