package com.example.demo_app.domain.phishing;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface PhishingAssistant {

    @SystemMessage("{{systemPrompt}}")
    String chat(@MemoryId String sessionId, @V("systemPrompt") String systemPrompt, @UserMessage String userMessage);

    @SystemMessage("{{systemPrompt}}")
    TokenStream chatStream(@MemoryId String sessionId, @V("systemPrompt") String systemPrompt, @UserMessage String userMessage);
}
