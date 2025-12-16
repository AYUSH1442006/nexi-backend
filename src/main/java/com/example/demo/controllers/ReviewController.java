package com.example.demo.controllers;

import com.example.demo.models.Review;
import com.example.demo.models.Task;
import com.example.demo.models.User;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    @PostMapping("/submit")
    public ResponseEntity<?> submitReview(@RequestBody Review review) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User reviewer = userRepo.findByEmail(email);
            
            if (reviewer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            if (review.getRating() < 1 || review.getRating() > 5) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Rating must be between 1 and 5"));
            }

            Optional<Task> taskOpt = taskRepo.findById(review.getTaskId());
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }

            Task task = taskOpt.get();

            if (!task.getStatus().equals("COMPLETED")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Can only review completed tasks"));
            }

            boolean isPoster = task.getPosterId().equals(reviewer.getId());
            boolean isAssigned = task.getAssignedTo() != null && task.getAssignedTo().equals(reviewer.getId());

            if (!isPoster && !isAssigned) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only review tasks you're involved in"));
            }

            String reviewedUserId = isPoster ? task.getAssignedTo() : task.getPosterId();
            Optional<User> reviewedUserOpt = userRepo.findById(reviewedUserId);
            
            if (!reviewedUserOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Reviewed user not found"));
            }

            User reviewedUser = reviewedUserOpt.get();

            review.setReviewerId(reviewer.getId());
            review.setReviewerName(reviewer.getName());
            review.setReviewedUserId(reviewedUser.getId());
            review.setReviewedUserName(reviewedUser.getName());
            review.setCreatedAt(LocalDateTime.now().toString());

            Review savedReview = reviewRepo.save(review);

            List<Review> userReviews = reviewRepo.findByReviewedUserId(reviewedUser.getId());
            double totalRating = 0;
            for (Review r : userReviews) {
                totalRating += r.getRating();
            }
            double avgRating = totalRating / userReviews.size();

            reviewedUser.setRating(Math.round(avgRating * 10.0) / 10.0);
            reviewedUser.setTotalReviews(userReviews.size());
            userRepo.save(reviewedUser);

            return ResponseEntity.ok(Map.of(
                "message", "Review submitted successfully",
                "review", savedReview
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getReviewsForUser(@PathVariable String userId) {
        try {
            List<Review> reviews = reviewRepo.findByReviewedUserId(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getReviewsForTask(@PathVariable String taskId) {
        try {
            List<Review> reviews = reviewRepo.findByTaskId(taskId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable String reviewId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            Optional<Review> reviewOpt = reviewRepo.findById(reviewId);
            if (!reviewOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Review not found"));
            }

            Review review = reviewOpt.get();

            if (!review.getReviewerId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only delete your own reviews"));
            }

            reviewRepo.deleteById(reviewId);

            Optional<User> reviewedUserOpt = userRepo.findById(review.getReviewedUserId());
            if (reviewedUserOpt.isPresent()) {
                User reviewedUser = reviewedUserOpt.get();
                List<Review> userReviews = reviewRepo.findByReviewedUserId(reviewedUser.getId());
                
                if (userReviews.isEmpty()) {
                    reviewedUser.setRating(0.0);
                    reviewedUser.setTotalReviews(0);
                } else {
                    double totalRating = 0;
                    for (Review r : userReviews) {
                        totalRating += r.getRating();
                    }
                    double avgRating = totalRating / userReviews.size();
                    reviewedUser.setRating(Math.round(avgRating * 10.0) / 10.0);
                    reviewedUser.setTotalReviews(userReviews.size());
                }
                
                userRepo.save(reviewedUser);
            }

            return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}