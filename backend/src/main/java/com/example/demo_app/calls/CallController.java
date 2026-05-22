package com.example.demo_app.calls;

import com.example.demo_app.api.dto.ProgressReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Slf4j
public class CallController implements CallApi {

    // Store active SSE emitters per user
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter connect(String userId) {
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

    @Override
    public String triggerCall(String userId, String scenarioId) {
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

    private final TtsClient ttsClient;

    public CallController(TtsClient ttsClient) {
        this.ttsClient = ttsClient;
    }

    @Override
    public String reportProgress(String userId, ProgressReport report) {
        log.info("[PROGRESS REPORT] User: {}, Status: {}, CurrentStep: {}", 
                userId, report.getStatus(), report.getCurrentStep());
        return "Logged";
    }

    @Override
    public org.springframework.http.ResponseEntity<byte[]> streamAudio(String text) {
        log.info("REST: Streaming audio synthesis request for text: '{}'", text);
        byte[] audioBytes = ttsClient.synthesizeSpeech(text);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("audio/mpeg"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .body(audioBytes);
    }
}

