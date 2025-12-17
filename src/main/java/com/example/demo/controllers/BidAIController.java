package com.example.demo.controllers;

import com.example.demo.models.Bid;
import com.example.demo.models.Task;
import com.example.demo.models.User;
import com.example.demo.repositories.BidRepository;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.BidRankingService;
import com.example.demo.services.GeminiService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
//@CrossOrigin(origins = "*")
public class BidAIController {

    @Autowired
    private BidRepository bidRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BidRankingService rankingService;

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/rank-bids/{taskId}")
    public ResponseEntity<?> rankBids(@PathVariable String taskId) {

        // 1️⃣ Fetch task
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // 2️⃣ Fetch bids
        List<Bid> bids = bidRepo.findByTaskId(taskId);

        if (bids.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> rankedBids = new ArrayList<>();

        // 3️⃣ Score + Gemini explanation
        for (Bid bid : bids) {

            User user = userRepo.findById(bid.getBidderId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            double score = rankingService.calculateScore(user, task, bid);

            String aiReason;
            try {
                aiReason = geminiService.generateExplanation(
                        user,
                        task,
                        bid,
                        score
                );
            } catch (Exception e) {
                aiReason = "AI explanation unavailable.";
            }

            Map<String, Object> result = new HashMap<>();
            result.put("bidId", bid.getId());
            result.put("amount", bid.getBidAmount());
            result.put("aiScore", score);
            result.put("aiReason", aiReason);
            result.put("bidderName", user.getName());
            result.put("bidderRating", user.getRating());

            rankedBids.add(result);
        }

        // 4️⃣ Sort by AI score (highest first)
        rankedBids.sort((a, b) ->
                Double.compare(
                        (double) b.get("aiScore"),
                        (double) a.get("aiScore")
                )
        );

        // 5️⃣ Return ranked bids
        return ResponseEntity.ok(rankedBids);
    }
}
