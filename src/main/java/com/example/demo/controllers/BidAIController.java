package com.example.demo.controllers;

import com.example.demo.models.Bid;
import com.example.demo.models.Task;
import com.example.demo.models.User;
import com.example.demo.repositories.BidRepository;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.BidRankingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class BidAIController {

    @Autowired
    private BidRepository bidRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BidRankingService rankingService;

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

        // 3️⃣ Score + Local AI explanation
        for (Bid bid : bids) {

            User user = userRepo.findById(bid.getBidderId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            double score = rankingService.calculateScore(user, task, bid);

            String aiReason = generateLocalExplanation(user, task, bid, score);

            Map<String, Object> result = new HashMap<>();
            result.put("bidId", bid.getId());
            result.put("amount", bid.getBidAmount());
            result.put("aiScore", score);
            result.put("aiReason", aiReason);
            result.put("bidderName", user.getName());
            result.put("bidderRating", user.getRating());

            // 🔥 BONUS FEATURE
            result.put("confidence", Math.min(100, Math.round(score * 4)));

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

    // ================== LOCAL AI LOGIC ==================

    private String generateLocalExplanation(User user, Task task, Bid bid, double score) {

        List<String> reasons = new ArrayList<>();

        // 💰 Price logic
        if (bid.getBidAmount() <= task.getBudget()) {
            reasons.add("Bid price fits within the task budget");
        } else {
            reasons.add("Bid price exceeds the task budget");
        }

        // ⭐ Rating logic
        if (user.getRating() >= 4.5) {
            reasons.add("Tasker has an excellent rating");
        } else if (user.getRating() >= 3.5) {
            reasons.add("Tasker has a good rating");
        } else if (user.getRating() > 0) {
            reasons.add("Tasker has a low rating");
        } else {
            reasons.add("Tasker is new with no ratings");
        }

        // 🧠 Experience logic
        if (user.getTasksCompleted() >= 20) {
            reasons.add("Highly experienced with many completed tasks");
        } else if (user.getTasksCompleted() >= 5) {
            reasons.add("Moderate task experience");
        } else if (user.getTasksCompleted() > 0) {
            reasons.add("Limited task experience");
        } else {
            reasons.add("No completed tasks yet");
        }

        // 🎯 Final verdict
        if (score >= 20) {
            reasons.add("Overall this bid is highly recommended");
        } else if (score >= 12) {
            reasons.add("This bid is a good option");
        } else {
            reasons.add("This bid is less suitable compared to others");
        }

        return String.join(". ", reasons) + ".";
    }
}
