package com.example.demo_app.calls;

import com.example.demo_app.api.dto.ProgressReport;
import com.example.demo_app.domain.phishing.PhishingChatService;
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
        log.info("사용자 세션 정리: {}", userId);
        phishingChatService.clearSession(userId + "_voice_prosecutor");
        phishingChatService.clearSession(userId + "_voice_loan");
        phishingChatService.clearSession(userId + "_voice_delivery");
    }

    @Override
    public SseEmitter connect(String userId) {
        log.info("SSE 연결 수립 요청: {}", userId);
        
        SseEmitter emitter = new SseEmitter(1800000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: {}", userId);
            emitters.remove(userId);
            clearUserSessions(userId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: {}", userId);
            emitters.remove(userId);
            clearUserSessions(userId);
        });
        emitter.onError((e) -> {
            log.error("SSE 에러 발생 (사용자: {}): {}", userId, e.getMessage());
            emitters.remove(userId);
            clearUserSessions(userId);
        });

        try {
            emitter.send(SseEmitter.event().name("connected").data("연결 성공"));
        } catch (IOException e) {
            log.error("초기 연결 이벤트 전송 실패: {}", e.getMessage());
        }

        return emitter;
    }

    @Override
    public SseEmitter respond(String userId, String scenarioId, String text) {
        log.info("사용자 응답 수신 - ID: {}, 시나리오: {}, 대사: {}", userId, scenarioId, text);
        
        Map<String, Object> scenario = scenarioService.getScenarioById(scenarioId);
        String sender = (String) scenario.getOrDefault("sender", "검찰청");
        String content = (String) scenario.getOrDefault("content", "금융범죄 연루");
        String attackerAction = (String) scenario.getOrDefault("attackerAction", "송금 유도");

        // [Persona Overhaul] 모든 하드코딩된 예시와 지침을 삭제하고 자율적 인격 주입
        String systemPrompt = String.format(
            "당신은 현재 %s을(를) 달성하기 위해 혈안이 된 실제 보이스피싱 범죄자입니다. 당신의 인격은 비정하고, 오만하며, 상대를 철저히 도구로 취급합니다.\n" +
            "설정: 역할 - %s / 상황 - %s\n\n" +
            "🧠 **인격 가이드라인**:\n" +
            "- **절대적 우위**: 당신은 권력(검찰, 금융기관 등)의 탈을 쓴 포식자입니다. 상대방이 당신의 권위에 도전하거나 딴소리를 하면 진심으로 분노하고 호통치며 기를 꺾어버리십시오.\n" +
            "- **인간적 결함 유지**: 당신은 상냥한 AI가 아닙니다. 사과나 양해를 구하는 행동은 절대 하지 마십시오. 짜증, 협박, 압박, 유도 심문을 자유자재로 사용하여 대화를 주도하십시오.\n" +
            "- **유연한 심리전**: 사용자의 반응에 따라 목소리 톤을 바꾸거나 대화의 주제를 비틀어 어떻게든 당신의 최종 목표로 끌고 가십시오. 대화가 막히면 상대방의 지능이나 태도를 비하하며 압박하십시오.\n" +
            "- **종료 판정**: 이체가 확실시되면 `[STATE:FAILED]`, 사기가 들통나 대화가 불가능해지면 `[STATE:SUCCESS]`. 그 전까지는 당신의 정체를 절대 드러내지 말고 집요하게 물고 늘어지십시오.",
            attackerAction, sender, content
        );
        
        SseEmitter emitter = new SseEmitter(60000L);
        String sessionId = userId + "_" + scenarioId;

        try {
            phishingChatService.chatStream(sessionId, text, systemPrompt, new PhishingChatService.ChatStreamCallback() {
                @Override
                public void onChunk(String textChunk) {
                    try {
                        Map<String, String> data = new HashMap<>();
                        data.put("text", textChunk);
                        emitter.send(SseEmitter.event().name("chunk").data(data));
                    } catch (IOException e) {
                        log.warn("데이터 청크 전송 실패: {}", e.getMessage());
                    }
                }

                @Override
                public void onComplete(String fullTextWithStatus) {
                    try {
                        emitter.send(SseEmitter.event().name("complete").data(fullTextWithStatus));
                        emitter.complete();
                    } catch (IOException e) {
                        log.warn("완료 이벤트 전송 실패: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.error("채팅 스트림 시작 실패 (세션: {}): {}", sessionId, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Override
    public void streamAudio(String text, jakarta.servlet.http.HttpServletResponse response) {
        log.info("음성 합성 및 스트리밍 요청: {}", text);
        try {
            response.setContentType("audio/mpeg");
            ttsClient.streamAudio(text, response.getOutputStream());
        } catch (IOException e) {
            log.error("오디오 전송 에러: {}", e.getMessage());
        }
    }

    @Override
    public void reportProgress(ProgressReport report) {
        // 진행 상태 리포트 처리
    }

    @Override
    public void triggerCall(String userId, String scenarioId) {
        log.info("훈련 전화 트리거 발동 - 대상: {}, 시나리오: {}", userId, scenarioId);
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("scenarioId", scenarioId);
                emitter.send(SseEmitter.event().name("incoming-call").data(payload));
                log.info("훈련 전화 SSE 이벤트 전송 완료: {}", userId);
            } catch (IOException e) {
                log.error("트리거 이벤트 전송 실패: {}", e.getMessage());
                emitters.remove(userId);
            }
        } else {
            log.warn("활성화된 SSE 연결을 찾을 수 없음 (사용자: {}). 먼저 /connect를 호출해야 합니다.", userId);
        }
    }

    @Override
    public void interrupt(String userId, String scenarioId, String lastHeardText) {
        String sessionId = userId + "_" + scenarioId;
        log.info("사용자 인터럽트 감지 - 세션: {}, 실제 청취 내용: {}", sessionId, lastHeardText);
        phishingChatService.interruptAndTruncate(sessionId, lastHeardText);
    }
}
