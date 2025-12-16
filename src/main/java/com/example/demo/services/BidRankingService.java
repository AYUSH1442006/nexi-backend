package com.example.demo.services;

import com.example.demo.models.Bid;
import com.example.demo.models.Task;
import com.example.demo.models.User;
import org.springframework.stereotype.Service;

@Service
public class BidRankingService {

    public double calculateScore(User user, Task task, Bid bid) {

        double score = 0;

        /* 1️⃣ USER RATING (30 points) */
        score += (user.getRating() / 5.0) * 30;

        /* 2️⃣ SKILL MATCH (25 points) */
        if (user.getSkills() != null &&
            task.getRequiredSkills() != null &&
            !task.getRequiredSkills().isEmpty()) {

            long matchedSkills = user.getSkills().stream()
                    .filter(task.getRequiredSkills()::contains)
                    .count();

            double skillMatchRatio =
                    (double) matchedSkills / task.getRequiredSkills().size();

            score += skillMatchRatio * 25;
        }

        /* 3️⃣ PRICE FAIRNESS (20 points) */
        double bidAmount = bid.getBidAmount();
        double budget = task.getBudget();

        if (bidAmount > 0) {
            double priceRatio = budget / bidAmount;
            score += Math.min(priceRatio, 1.2) * 20;
        }

        /* 4️⃣ EXPERIENCE (15 points) */
        score += Math.min(user.getTasksCompleted(), 20) * 0.75;

        /* 5️⃣ LOCATION MATCH (10 points) */
        if (user.getLocation() != null && task.getLocation() != null) {

            double latDiff = Math.abs(
                    user.getLocation().getLat() - task.getLocation().getLat()
            );
            double lngDiff = Math.abs(
                    user.getLocation().getLng() - task.getLocation().getLng()
            );

            // ~10 km radius
            if (latDiff < 0.1 && lngDiff < 0.1) {
                score += 10;
            }
        }

        return Math.round(score * 100.0) / 100.0;
    }
}
