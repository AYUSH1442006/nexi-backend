package com.example.demo.controllers;

import com.example.demo.models.Task;
import com.example.demo.models.User;
import com.example.demo.repositories.BidRepository;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
//@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private BidRepository bidRepo;

    @Autowired
    private UserRepository userRepo;

    // Get dashboard statistics for current user
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            // Get task counts
            List<Task> postedTasks = taskRepo.findByPosterId(user.getId());
            List<Task> assignedTasks = taskRepo.findByAssignedTo(user.getId());
            
            long openTasks = postedTasks.stream()
                .filter(t -> t.getStatus().equals("OPEN"))
                .count();
            
            long activeTasks = assignedTasks.stream()
                .filter(t -> t.getStatus().equals("ASSIGNED") || t.getStatus().equals("IN_PROGRESS"))
                .count();
            
            long completedTasks = assignedTasks.stream()
                .filter(t -> t.getStatus().equals("COMPLETED"))
                .count();

            // Get bid counts
            long myBids = bidRepo.findByBidderId(user.getId()).size();
            long pendingBids = bidRepo.findByBidderId(user.getId()).stream()
                .filter(b -> b.getStatus().equals("PENDING"))
                .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("postedTasks", postedTasks.size());
            stats.put("openTasks", openTasks);
            stats.put("assignedTasks", assignedTasks.size());
            stats.put("activeTasks", activeTasks);
            stats.put("completedTasks", completedTasks);
            stats.put("myBids", myBids);
            stats.put("pendingBids", pendingBids);
            stats.put("rating", user.getRating());
            stats.put("totalReviews", user.getTotalReviews());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get recent activity
    @GetMapping("/activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            List<Task> recentPosted = taskRepo.findByPosterId(user.getId());
            List<Task> recentAssigned = taskRepo.findByAssignedTo(user.getId());

            // Sort by updatedAt and limit to 5 most recent
            recentPosted = recentPosted.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(5)
                .toList();

            recentAssigned = recentAssigned.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(5)
                .toList();

            Map<String, Object> activity = new HashMap<>();
            activity.put("recentPostedTasks", recentPosted);
            activity.put("recentAssignedTasks", recentAssigned);

            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get platform-wide statistics (public)
    @GetMapping("/platform-stats")
    public ResponseEntity<?> getPlatformStats() {
        try {
            long totalUsers = userRepo.count();
            long totalTasks = taskRepo.count();
            long openTasks = taskRepo.findByStatus("OPEN").size();
            long completedTasks = taskRepo.findByStatus("COMPLETED").size();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("totalTasks", totalTasks);
            stats.put("openTasks", openTasks);
            stats.put("completedTasks", completedTasks);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}