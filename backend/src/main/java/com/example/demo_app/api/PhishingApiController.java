package com.example.demo_app.api;

import com.example.demo_app.api.dto.*;
import com.example.demo_app.domain.logs.TrainingLogService;
import com.example.demo_app.domain.scenarios.ScenarioService;
import com.example.demo_app.domain.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PhishingApiController {

    private final UserService userService;
    private final ScenarioService scenarioService;
    private final TrainingLogService trainingLogService;

    @PostMapping("/profiling")
    public String saveProfiling(
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody UserProfilingRequest request) {
        log.info("API: Save profiling for user: {}", userId);
        return userService.saveProfiling(userId, request);
    }

    @GetMapping("/scenarios")
    public List<Map<String, Object>> getScenarios() {
        log.info("API: Fetching all scenarios");
        return scenarioService.getScenarios();
    }

    @GetMapping("/scenarios/{id}")
    public Map<String, Object> getScenarioById(@PathVariable("id") String id) {
        log.info("API: Fetching scenario by id: {}", id);
        return scenarioService.getScenarioById(id);
    }

    @PostMapping("/training/log")
    public Map<String, Object> recordTrainingLog(
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody LogPayloadRequest request) {
        log.info("API: Recording training log for user: {}", userId);
        return trainingLogService.recordTrainingLog(userId, request);
    }

    @GetMapping("/reports/monthly")
    public Map<String, Object> getMonthlyReport(@RequestParam(value = "userId", defaultValue = "demo_user") String userId) {
        log.info("API: Generating monthly report for user: {}", userId);
        return trainingLogService.getMonthlyReport(userId);
    }

    @GetMapping("/profiling/questions")
    public List<ProfilingQuestion> getQuestions() {
        log.info("API: Fetching profiling questions");
        return userService.getQuestions();
    }

    @PostMapping("/profiling/analyze")
    public ProfilingResultResponse analyzeRisk(
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody Map<String, String> answers) {
        log.info("API: Analyzing risk for user: {}", userId);
        return userService.analyzeRisk(userId, answers);
    }
}
