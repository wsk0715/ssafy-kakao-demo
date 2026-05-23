package com.example.demo_app.calls;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PhishingChatService {

    public interface ChatStreamCallback {
        void onChunk(String text);
        void onComplete(String fullText);
    }

    @Value("${upstage.api-key:}")
    private String upstageApiKey;

    @Value("${upstage.api-url:https://api.upstage.ai/v1}")
    private String upstageApiUrl;

    @Value("${upstage.model-name:solar-1-mini-chat}")
    private String upstageModelName;

    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();
    private ChatModel model;
    private StreamingChatModel streamingModel;

    private ChatModel getModel() {
        if (model == null) {
            String apiKey = getApiKey();
            String maskedKey = apiKey.length() > 6 ? apiKey.substring(0, 6) + "..." : "...";
            log.info("Initializing Upstage Solar LLM via OpenAiChatModel. URL: {}, Model: {}, Key: {}", 
                    upstageApiUrl, upstageModelName, maskedKey);
            
            model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(upstageApiUrl)
                    .modelName(upstageModelName)
                    .build();
        }
        return model;
    }

    private StreamingChatModel getStreamingModel() {
        if (streamingModel == null) {
            String apiKey = getApiKey();
            String maskedKey = apiKey.length() > 6 ? apiKey.substring(0, 6) + "..." : "...";
            log.info("Initializing Upstage Solar Streaming LLM via OpenAiStreamingChatModel. URL: {}, Model: {}, Key: {}", 
                    upstageApiUrl, upstageModelName, maskedKey);
            
            streamingModel = OpenAiStreamingChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(upstageApiUrl)
                    .modelName(upstageModelName)
                    .build();
        }
        return streamingModel;
    }

    private String getApiKey() {
        String apiKey = upstageApiKey;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("UPSTAGE_API_KEY");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("UPSTAGE_API_KEY environment variable or upstage.api-key configuration is missing.");
        }
        return apiKey;
    }

    public void startSession(String sessionId, String systemPrompt) {
        log.info("Starting phishing chat session: {}", sessionId);
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        chatMemory.add(new SystemMessage(systemPrompt));
        sessionMemories.put(sessionId, chatMemory);
    }

    public String chat(String sessionId, String userInput, String defaultSystemPrompt) {
        ChatMemory chatMemory = sessionMemories.get(sessionId);
        if (chatMemory == null) {
            log.warn("Session {} not found in memory. Re-initializing session.", sessionId);
            startSession(sessionId, defaultSystemPrompt);
            chatMemory = sessionMemories.get(sessionId);
        }

        chatMemory.add(new UserMessage(userInput));

        try {
            log.info("Sending chat request to Upstage Solar API for session: {}", sessionId);
            ChatRequest request = ChatRequest.builder()
                    .messages(chatMemory.messages())
                    .build();
            ChatResponse response = getModel().chat(request);
            AiMessage aiMessage = response.aiMessage();
            chatMemory.add(aiMessage);
            log.info("Received response from Upstage Solar: {}", aiMessage.text());
            return aiMessage.text();
        } catch (Exception e) {
            log.error("Error during LLM chat generation: {}", e.getMessage(), e);
            return "네? 전선에 이상이 있나요? 잘 안 들립니다. 다시 말씀해 주십시오.";
        }
    }

    public void chatStream(String sessionId, String userInput, String defaultSystemPrompt, ChatStreamCallback callback) {
        ChatMemory chatMemory = sessionMemories.get(sessionId);
        if (chatMemory == null) {
            log.warn("Session {} not found in memory. Re-initializing session.", sessionId);
            startSession(sessionId, defaultSystemPrompt);
            chatMemory = sessionMemories.get(sessionId);
        }

        chatMemory.add(new UserMessage(userInput));
        
        StringBuilder sentenceBuffer = new StringBuilder();
        StringBuilder fullReplyBuffer = new StringBuilder();
        ChatMemory finalChatMemory = chatMemory;

        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(chatMemory.messages())
                    .build();
            
            log.info("Starting streaming chat request to Upstage Solar for session: {}", sessionId);
            getStreamingModel().chat(request, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                    sentenceBuffer.append(token);
                    fullReplyBuffer.append(token);
                    
                    String currentText = sentenceBuffer.toString();
                    if (isSentenceBoundary(currentText)) {
                        callback.onChunk(currentText.trim());
                        sentenceBuffer.setLength(0); // clear buffer
                    }
                }

                @Override
                public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                    String remaining = sentenceBuffer.toString().trim();
                    if (!remaining.isEmpty()) {
                        callback.onChunk(remaining);
                    }
                    finalChatMemory.add(response.aiMessage());
                    callback.onComplete(fullReplyBuffer.toString());
                    log.info("Streaming complete for session: {}. Full response length: {}", sessionId, fullReplyBuffer.length());
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Streaming error in session: {}", sessionId, error);
                    callback.onChunk("통화 신호에 문제가 발생했습니다. 다시 말씀해주세요.");
                }
            });
        } catch (Exception e) {
            log.error("Failed to start LLM streaming for session: {}", sessionId, e);
            callback.onChunk("통화 연결이 지연되고 있습니다.");
        }
    }

    private boolean isSentenceBoundary(String text) {
        if (text.isEmpty()) return false;
        char lastChar = text.charAt(text.length() - 1);
        // Check standard sentence boundaries
        if (lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == '\n') {
            return true;
        }
        // Heuristic: if sentence grows too long without punctuation, split at space
        // Increased threshold from 45 to 75 to prevent unnatural Korean mid-phrase splits (e.g. '악화될' ... '수 있습니다')
        if (text.length() > 75 && lastChar == ' ') {
            return true;
        }
        return false;
    }

    public void clearSession(String sessionId) {
        log.info("Clearing phishing chat session memory for: {}", sessionId);
        sessionMemories.remove(sessionId);
    }
}
