package com.example.demo_app.calls;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class TtsClient {

    @Value("${tts.api-url}")
    private String ttsApiUrl;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * Synthesizes text to speech using Edge-TTS local server.
     * Returns MP3 audio bytes.
     */
    public byte[] synthesizeSpeech(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Empty text received for TTS synthesis.");
            return new byte[0];
        }

        try {
            log.info("Requesting Edge-TTS audio generation for text: '{}'", text);
            byte[] audioBytes = restClient.get()
                    .uri(ttsApiUrl + "/generate?text={text}", text)
                    .retrieve()
                    .body(byte[].class);

            if (audioBytes != null && audioBytes.length > 0) {
                log.info("Successfully received synthesized speech bytes. Size: {} bytes", audioBytes.length);
                return audioBytes;
            }
            throw new RuntimeException("Received empty audio response from TTS server.");

        } catch (Exception e) {
            log.error("Edge-TTS API call failed: {}", e.getMessage());
            throw new RuntimeException("TTS synthesis failed: " + e.getMessage(), e);
        }
    }
}
