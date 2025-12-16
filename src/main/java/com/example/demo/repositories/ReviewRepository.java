package com.example.demo.repositories;

import com.example.demo.models.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByReviewedUserId(String userId);
    List<Review> findByTaskId(String taskId);
}