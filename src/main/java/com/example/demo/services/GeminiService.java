package com.example.demo.services;

import com.example.demo.models.Bid;
import com.example.demo.models.Task;
import com.example.demo.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public String generateExplanation(User user, Task task, Bid bid, double score) {

        String prompt = """
        Explain in simple language why this bid is ranked with score %.2f.

        User rating: %s
        User skills: %s
        Task required skills: %s
        Bid amount: %s
        Task budget: %s
        Tasks completed: %s

        Give a short, friendly explanation (1–2 lines).
        """.formatted(
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
           Map response = webClient.post()
        .uri("/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey)
        .header("Content-Type", "application/json")
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(Map.class)
        .block();



            if (response == null) {
                logger.error("Gemini API returned null response");
                return "AI explanation unavailable at the moment.";
            }

            if (response.containsKey("error")) {
                Map error = (Map) response.get("error");
                logger.error("Gemini API error: {}", error);
                return "AI explanation unavailable at the moment.";
            }

            if (!response.containsKey("candidates")) {
                logger.error("Gemini API response missing candidates: {}", response);
                return "AI explanation unavailable at the moment.";
            }

            Map candidate = (Map) ((List<?>) response.get("candidates")).get(0);
            Map content = (Map) candidate.get("content");
            Map part = (Map) ((List<?>) content.get("parts")).get(0);

            return part.get("text").toString();

        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            return "AI explanation unavailable at the moment.";
        }
    }
}
