package com.example.demo_app.calls;

import com.example.demo_app.api.dto.ProgressReport;
import com.example.demo_app.domain.scenarios.ScenarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@Slf4j
public class CallController implements CallApi {

    // Store active SSE emitters per user
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final TtsClient ttsClient;
    private final ScenarioService scenarioService;
    private final PhishingChatService phishingChatService;

    public CallController(TtsClient ttsClient, ScenarioService scenarioService, PhishingChatService phishingChatService) {
        this.ttsClient = ttsClient;
        this.scenarioService = scenarioService;
        this.phishingChatService = phishingChatService;
    }

    private void clearUserSessions(String userId) {
        phishingChatService.clearSession(userId + "_voice_prosecutor");
        phishingChatService.clearSession(userId + "_voice_loan");
    }

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
            clearUserSessions(userId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", userId);
            emitters.remove(userId);
            clearUserSessions(userId);
        });
        emitter.onError((ex) -> {
            log.error("SSE connection error for user: {}", userId, ex);
            emitters.remove(userId);
            clearUserSessions(userId);
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

    @Override
    public String reportProgress(String userId, ProgressReport report) {
        log.info("[PROGRESS REPORT] User: {}, Status: {}, CurrentStep: {}", 
                userId, report.getStatus(), report.getCurrentStep());
        return "Logged";
    }

    @Override
    public org.springframework.http.ResponseEntity<byte[]> streamAudio(String text) {
        if (text == null) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }

        // Clean the text: remove stage directions in parentheses (), brackets [], or braces {}
        String cleaned = text.replaceAll("\\([^)]*\\)", "")
                             .replaceAll("\\[[^]]*\\]", "")
                             .replaceAll("\\{[^}]*\\}", "")
                             .replaceAll("\\s+", " ")
                             .trim();

        // Print clean log output on a single line to prevent "messy" multiline logs
        log.info("REST: Streaming audio synthesis request for text: '{}' (original: '{}')", 
                 cleaned, text.replace("\n", " "));

        if (cleaned.isEmpty()) {
            log.info("Cleaned text is empty. Returning empty audio bytes.");
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType("audio/mpeg"))
                    .body(new byte[0]);
        }

        byte[] audioBytes = ttsClient.synthesizeSpeech(cleaned);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("audio/mpeg"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .body(audioBytes);
    }

    @Override
    public SseEmitter getLlmResponse(String userId, String scenarioId, String text) {
        log.info("REST: Generating LLM phishing response via SSE for user: {}, scenario: {}, input: '{}'", userId, scenarioId, text);
        
        // Fetch scenario context
        Map<String, Object> scenario = scenarioService.getScenarioById(scenarioId);
        String sender = (String) scenario.getOrDefault("sender", "보이스피싱 사기단");
        String content = (String) scenario.getOrDefault("content", "금융 사기 의심 행동 유도");
        String attackerAction = (String) scenario.getOrDefault("attackerAction", "가상계좌 이체 및 카드 정보 탈취");
        
        // Construct system prompt dynamically
        String systemPrompt = String.format(
            "당신은 보이스피싱 가짜 전화를 거는 공격자(Attacker)입니다. 다음 설정에 맞추어 실제 전화 통화처럼 한국어로 자연스럽고 다급하며 압박하는 어조로 상대방(김철수)과 대화하십시오.\n" +
            "- 피싱 역할: %s\n" +
            "- 피싱 내용(거짓 핑계): %s\n" +
            "- 최종 유도 목표(송금 등): %s\n\n" +
            "규칙:\n" +
            "1. 한 번에 여러 질문을 하지 말고 실제 통화처럼 한 문장이나 두 문장 이내로 짧게 한 마디씩만 응답하십시오.\n" +
            "2. 상대방의 실제 개인정보(주민등록번호, 비밀번호 등)를 직접 언급하지 말고 가상 토큰으로 취급하십시오.\n" +
            "3. 상대방이 의심하거나 끊으려 하면 더욱 강압적으로 압박하거나 안심시키려 설득하십시오.\n" +
            "4. 절대로 본인이 AI이거나 챗봇임(예: 챗GPT, 대형 언어 모델 등)을 밝히지 마십시오. 당신은 실제 사람 보이스피싱 사기꾼입니다.\n" +
            "5. 절대로 대괄호나 소괄호 안에 동작 지침, 해설, 지문, 상황 설명(예: '(가짜 금감원 안전 계좌 번호 제공)', '[화난 목소리로]')을 적지 마십시오. 오직 직접 말하는 대사만 한국어로 자연스럽게 출력하십시오.\n" +
            "6. 송금 목표를 실행할 때는 지문으로 지시하지 말고, 실제 존재하는 은행명과 가상의 임의 계좌번호(예: '국민은행 302-1234-5678-90')를 직접 대사로 불러주십시오.",
            sender, content, attackerAction
        );
        
        // Create emitter with 60-second timeout
        SseEmitter emitter = new SseEmitter(60000L);
        String sessionId = userId + "_" + scenarioId;
        java.util.concurrent.atomic.AtomicInteger chunkIndex = new java.util.concurrent.atomic.AtomicInteger(0);

        try {
            phishingChatService.chatStream(sessionId, text, systemPrompt, new PhishingChatService.ChatStreamCallback() {
                @Override
                public void onChunk(String chunkText) {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        data.put("text", chunkText);
                        data.put("index", chunkIndex.getAndIncrement());
                        emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(data));
                    } catch (Exception e) {
                        log.error("Failed to send chunk event via SSE for session {}: {}", sessionId, e.getMessage());
                    }
                }

                @Override
                public void onComplete(String fullText) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(fullText));
                        emitter.complete();
                        log.info("SSE Stream completed successfully for session: {}", sessionId);
                    } catch (Exception e) {
                        log.error("Failed to send complete event via SSE for session {}: {}", sessionId, e.getMessage());
                        emitter.completeWithError(e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to start chatStream for session {}: {}", sessionId, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
