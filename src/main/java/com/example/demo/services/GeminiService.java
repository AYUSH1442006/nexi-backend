package com.example.demo.services;

import com.example.demo.models.Bid;
import com.example.demo.models.Task;
import com.example.demo.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;
    private final String apiKey;

    public GeminiService(WebClient.Builder builder, @Value("${gemini.api.key}") String geminiKey) {
        this.webClient = builder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.apiKey = geminiKey;
        
        // Log API key status on startup
        if (apiKey != null && !apiKey.isEmpty()) {
            logger.info("✓ Gemini API Key loaded successfully");
        } else {
            logger.error("✗ GEMINI_API_KEY is not set! AI features will not work.");
        }
    }

    public String generateExplanation(User user, Task task, Bid bid, double score) {

        // Check if API key is available
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("Gemini API key is not configured");
            return "AI explanation unavailable at the moment.";
        }

        String prompt = String.format(
            "Explain in simple language why this bid is ranked with score %.2f.\n\n" +
            "User rating: %s\n" +
            "User skills: %s\n" +
            "Task required skills: %s\n" +
            "Bid amount: %s\n" +
            "Task budget: %s\n" +
            "Tasks completed: %s\n\n" +
            "Give a short, friendly explanation (1-2 lines).",
            score,
            user.getRating(),
            user.getSkills(),
            task.getRequiredSkills(),
            bid.getBidAmount(),
            task.getBudget(),
            user.getTasksCompleted()
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        try {
            logger.info("Calling Gemini API for bid explanation...");
            
            Map response = webClient.post()
                .uri("/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response == null) {
                logger.error("Gemini API returned null response");
                return "AI explanation unavailable at the moment.";
            }

            logger.info("Gemini API response received: {}", response.keySet());

            // Check for errors
            if (response.containsKey("error")) {
                Map error = (Map) response.get("error");
                logger.error("Gemini API error: {}", error);
                
                String errorMessage = error.get("message") != null ? 
                    error.get("message").toString() : "Unknown error";
                logger.error("Error message: {}", errorMessage);
                
                return "AI explanation unavailable at the moment.";
            }

            // Check for candidates
            if (!response.containsKey("candidates")) {
                logger.error("Gemini API response missing candidates. Response: {}", response);
                return "AI explanation unavailable at the moment.";
            }

            // Extract the text response
            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates.isEmpty()) {
                logger.error("Gemini API returned empty candidates list");
                return "AI explanation unavailable at the moment.";
            }

            Map candidate = (Map) candidates.get(0);
            Map content = (Map) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map part = (Map) parts.get(0);
            String text = part.get("text").toString();

            logger.info("✓ Successfully generated AI explanation");
            return text;

        } catch (WebClientResponseException e) {
            logger.error("Gemini API HTTP error - Status: {}, Body: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            
            // Specific error messages for common issues
            if (e.getStatusCode().value() == 400) {
                logger.error("Bad request - check model name and request format");
            } else if (e.getStatusCode().value() == 401) {
                logger.error("Unauthorized - check API key validity");
            } else if (e.getStatusCode().value() == 429) {
                logger.error("Rate limit exceeded - too many requests");
            }
            
            return "AI explanation unavailable at the moment.";
            
        } catch (Exception e) {
            logger.error("Unexpected error calling Gemini API: {}", e.getMessage(), e);
            return "AI explanation unavailable at the moment.";
        }
    }
}