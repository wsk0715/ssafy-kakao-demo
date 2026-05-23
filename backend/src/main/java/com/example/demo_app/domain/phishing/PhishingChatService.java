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

    // 선언적으로 정의된 AI 서비스 주입
    private final PhishingAssistant assistant;

    public void startSession(String sessionId, String systemPrompt) {
        log.info("Starting phishing chat session: {}", sessionId);
    }

    public String chat(String sessionId, String userInput, String defaultSystemPrompt) {
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

    public void chatStream(String sessionId, String userInput, String defaultSystemPrompt, ChatStreamCallback callback) {
        StringBuilder sentenceBuffer = new StringBuilder();
        StringBuilder fullReplyBuffer = new StringBuilder();

        try {
            log.info("Starting streaming chat via AI Assistant for session: {}", sessionId);
            TokenStream tokenStream = assistant.chatStream(sessionId, defaultSystemPrompt, userInput);

            tokenStream
                .onPartialResponse(token -> {
                    sentenceBuffer.append(token);
                    fullReplyBuffer.append(token);
                    
                    String currentText = sentenceBuffer.toString();
                    if (isSentenceBoundary(currentText)) {
                        callback.onChunk(currentText.trim());
                        sentenceBuffer.setLength(0);
                    }
                })
                .onCompleteResponse(response -> {
                    String remaining = sentenceBuffer.toString().trim();
                    if (!remaining.isEmpty()) {
                        callback.onChunk(remaining);
                    }
                    callback.onComplete(fullReplyBuffer.toString());
                    log.info("Streaming complete for session: {}. Full response length: {}", sessionId, fullReplyBuffer.length());
                })
                .onError(error -> {
                    log.error("Streaming error in session: {}", sessionId, error);
                    callback.onChunk("통화 신호에 문제가 발생했습니다. 다시 말씀해주세요.");
                })
                .start();

        } catch (Exception e) {
            log.error("Failed to start LLM streaming for session: {}", sessionId, e);
            callback.onChunk("통화 연결이 지연되고 있습니다.");
        }
    }

    private boolean isSentenceBoundary(String text) {
        if (text.isEmpty()) return false;
        char lastChar = text.charAt(text.length() - 1);
        if (lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == '\n') {
            return true;
        }
        if (text.length() > 75 && lastChar == ' ') {
            return true;
        }
        return false;
    }

    public void clearSession(String sessionId) {
        log.info("Clearing session is handled by ChatMemoryProvider policies: {}", sessionId);
    }
}
