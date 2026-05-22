package com.example.demo_app.calls;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/calls")
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CallController {

    // Store active SSE emitters per user
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam(value = "userId", defaultValue = "demo_user") String userId) {
        log.info("Client connected to SSE: {}", userId);
        
        // Create emitter with 30-minute timeout
        SseEmitter emitter = new SseEmitter(1800000L);
        emitters.put(userId, emitter);

        // Clean up registry on completion/timeout/error
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", userId);
            emitters.remove(userId);
        });
        emitter.onError((ex) -> {
            log.error("SSE connection error for user: {}", userId, ex);
            emitters.remove(userId);
        });

        // Send initial connect event to prevent browser connection timeout
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE session established for user: " + userId));
        } catch (IOException e) {
            log.error("Failed to send initial connect event for user: {}", userId, e);
            emitters.remove(userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @PostMapping("/trigger")
    public String triggerCall(
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestParam(value = "scenarioId", defaultValue = "voice_prosecutor") String scenarioId) {
        
        log.info("Triggering simulated call for user: {}, scenario: {}", userId, scenarioId);
        SseEmitter emitter = emitters.get(userId);
        
        if (emitter == null) {
            log.warn("No active SSE session found for user: {}", userId);
            return "Fail: User not connected";
        }

        try {
            // Send JSON trigger payload
            String payload = String.format("{\"scenarioId\":\"%s\"}", scenarioId);
            emitter.send(SseEmitter.event()
                    .name("incoming-call")
                    .data(payload));
            log.info("Successfully pushed incoming-call event to user: {}", userId);
            return "Success";
        } catch (IOException e) {
            log.error("Failed to send incoming-call event for user: {}", userId, e);
            emitters.remove(userId);
            emitter.completeWithError(e);
            return "Fail: Connection error";
        }
    }

    @PostMapping("/progress")
    public String reportProgress(
            @RequestParam(value = "userId", defaultValue = "demo_user") String userId,
            @RequestBody ProgressReport report) {
        
        log.info("[PROGRESS REPORT] User: {}, Status: {}, CurrentStep: {}", 
                userId, report.getStatus(), report.getCurrentStep());
        return "Logged";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressReport {
        private String status;
        private int currentStep;
    }
}
