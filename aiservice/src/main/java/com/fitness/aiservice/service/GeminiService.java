package com.fitness.aiservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient.Builder webClientBuilder;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;


    public String getRecommendations(String details) {
        Map<String, Object> requestBody = Map.of("contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", details)})});
        return webClientBuilder.build()
                .post().uri(geminiApiUrl)
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
