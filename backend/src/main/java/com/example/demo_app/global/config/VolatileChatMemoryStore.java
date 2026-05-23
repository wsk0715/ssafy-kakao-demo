package com.example.demo_app.global.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 세션별 대화 내역을 메모리에 저장하고, 외부에서 직접 수정할 수 있게 해주는 커스텀 스토어
 */
@Component
public class VolatileChatMemoryStore implements ChatMemoryStore {

    private final Map<Object, List<ChatMessage>> map = new ConcurrentHashMap<>();

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return map.getOrDefault(memoryId, new ArrayList<>());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        map.put(memoryId, new ArrayList<>(messages));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        map.remove(memoryId);
    }

    /**
     * 특정 세션의 대화 내역을 직접 가져와서 수정하기 위한 유틸리티
     */
    public List<ChatMessage> getMessagesMutable(String sessionId) {
        return map.get(sessionId);
    }
}
