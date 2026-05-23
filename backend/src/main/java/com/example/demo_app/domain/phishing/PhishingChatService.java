package com.example.demo_app.domain.phishing;

import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhishingChatService {

    public interface ChatStreamCallback {
        void onChunk(String text);
        void onComplete(String fullText);
    }

    private final PhishingAssistant assistant;
    private final com.example.demo_app.global.config.VolatileChatMemoryStore memoryStore;

    private final java.util.Map<String, java.util.concurrent.atomic.AtomicBoolean> activeStreams = new java.util.concurrent.ConcurrentHashMap<>();

    public void startSession(String sessionId, String systemPrompt) {
        log.info("Starting phishing chat session: {}", sessionId);
    }

    private void cancelPreviousStream(String sessionId) {
        java.util.concurrent.atomic.AtomicBoolean status = activeStreams.get(sessionId);
        if (status != null) {
            log.info("Cancelling previous active stream for session: {}", sessionId);
            status.set(false);
        }
        activeStreams.put(sessionId, new java.util.concurrent.atomic.AtomicBoolean(true));
    }

    public String chat(String sessionId, String userInput, String defaultSystemPrompt) {
        cancelPreviousStream(sessionId);
        try {
            log.info("Sending chat request to AI Assistant for session: {}", sessionId);
            String response = assistant.chat(sessionId, defaultSystemPrompt, userInput);
            log.info("Received response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error during LLM chat generation: {}", e.getMessage(), e);
            return "네? 잘 안 들립니다. 다시 말씀해 주십시오.";
        }
    }

    /**
     * [Latency Optimized] 실시간 스트리밍 대화 생성
     */
    public void chatStream(String sessionId, String userInput, String defaultSystemPrompt, ChatStreamCallback callback) {
        cancelPreviousStream(sessionId);
        java.util.concurrent.atomic.AtomicBoolean currentStreamStatus = activeStreams.get(sessionId);

        StringBuilder sentenceBuffer = new StringBuilder();
        StringBuilder fullReplyBuffer = new StringBuilder();

        try {
            log.info("Starting streaming chat via AI Assistant for session: {}", sessionId);
            TokenStream tokenStream = assistant.chatStream(sessionId, defaultSystemPrompt, userInput);

            tokenStream
                .onPartialResponse(token -> {
                    if (!currentStreamStatus.get()) return;
                    
                    sentenceBuffer.append(token);
                    fullReplyBuffer.append(token);
                    
                    String currentText = sentenceBuffer.toString();
                    
                    // [Optimization] 조기 문장 경계 감지 (입니다, 세요 등) 시 즉시 전송
                    if (isProactiveSentenceBoundary(currentText)) {
                        String chunk = currentText.trim();
                        if (!chunk.isEmpty()) {
                            callback.onChunk(chunk);
                        }
                        sentenceBuffer.setLength(0);
                    }
                })
                .onCompleteResponse(response -> {
                    if (!currentStreamStatus.get()) return;
                    
                    String fullText = fullReplyBuffer.toString();
                    String resultStatus = "PROCEEDING";
                    
                    if (fullText.contains("[STATE:SUCCESS]")) {
                        resultStatus = "SUCCESS";
                        fullText = fullText.replace("[STATE:SUCCESS]", "").trim();
                    } else if (fullText.contains("[STATE:FAILED]")) {
                        resultStatus = "FAILED";
                        fullText = fullText.replace("[STATE:FAILED]", "").trim();
                    }
                    
                    String remaining = sentenceBuffer.toString().replace("[STATE:SUCCESS]", "").replace("[STATE:FAILED]", "").trim();
                    if (!remaining.isEmpty()) {
                        callback.onChunk(remaining);
                    }
                    
                    callback.onComplete(resultStatus + "|" + fullText);
                    log.info("Streaming complete for session: {}. Status: {}", sessionId, resultStatus);
                    activeStreams.remove(sessionId, currentStreamStatus);
                })
                .onError(error -> {
                    if (!currentStreamStatus.get()) return;
                    log.error("Streaming error in session: {}", sessionId, error);
                    callback.onChunk("통화 신호에 문제가 발생했습니다. 다시 말씀해주세요.");
                    activeStreams.remove(sessionId, currentStreamStatus);
                })
                .start();

        } catch (Exception e) {
            log.error("Failed to start LLM streaming for session: {}", sessionId, e);
            callback.onChunk("통화 연결이 지연되고 있습니다.");
            activeStreams.remove(sessionId, currentStreamStatus);
        }
    }

    /**
     * 한국어 특성을 고려한 조기 문장 절삭 로직.
     * 마침표가 없더라도 대화의 마디가 끝나는 패턴을 감지하여 TTS 지연을 최소화함.
     */
    private boolean isProactiveSentenceBoundary(String text) {
        if (text == null || text.length() < 5) return false;
        
        char lastChar = text.charAt(text.length() - 1);
        
        // 1. 명시적 종결자 (. ? ! \n)
        if (lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == '\n') {
            return true;
        }
        
        // 2. 종결 어미 패턴 감지 (입니다, 하세요, 죠, 까요 등) + 공백
        // 사용자가 말을 듣기 시작하는 타이밍을 앞당김
        if (text.endsWith(" ")) {
            String trimmed = text.trim();
            if (trimmed.endsWith("습니다") || 
                trimmed.endsWith("해요") || 
                trimmed.endsWith("게요") || 
                trimmed.endsWith("까요") || 
                trimmed.endsWith("네요") || 
                trimmed.endsWith("시오") || 
                trimmed.endsWith("세요") || 
                trimmed.endsWith("이죠") || 
                trimmed.endsWith("니까") ||
                trimmed.endsWith("합니까") ||
                trimmed.endsWith("니까요")) {
                return true;
            }
        }

        // 3. 안전 임계치 (너무 길어지면 강제 전송)
        return text.length() > 75;
    }

    public void clearSession(String sessionId) {
        log.info("Explicitly clearing and cancelling session: {}", sessionId);
        java.util.concurrent.atomic.AtomicBoolean status = activeStreams.get(sessionId);
        if (status != null) {
            status.set(false);
            activeStreams.remove(sessionId);
        }
        memoryStore.deleteMessages(sessionId);
    }

    public void interruptAndTruncate(String sessionId, String lastHeardText) {
        log.info("Interrupting session {}. User heard up to: [{}]", sessionId, lastHeardText);
        cancelPreviousStream(sessionId);

        java.util.List<dev.langchain4j.data.message.ChatMessage> messages = memoryStore.getMessages(sessionId);
        if (messages == null || messages.isEmpty()) return;

        int lastIndex = messages.size() - 1;
        dev.langchain4j.data.message.ChatMessage lastMessage = messages.get(lastIndex);
        
        if (lastMessage instanceof dev.langchain4j.data.message.AiMessage aiMessage) {
            String fullText = aiMessage.text();
            if (fullText.startsWith(lastHeardText)) {
                log.info("Truncating AI message from [{}] to [{}]", fullText, lastHeardText);
                messages.set(lastIndex, dev.langchain4j.data.message.AiMessage.from(lastHeardText));
                memoryStore.updateMessages(sessionId, messages);
            } else {
                log.warn("Reported heard text mismatch. Full: [{}], Heard: [{}]", fullText, lastHeardText);
            }
        }
    }
}
