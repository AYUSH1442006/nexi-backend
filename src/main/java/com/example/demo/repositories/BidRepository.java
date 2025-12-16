package com.example.demo.repositories;

import com.example.demo.models.Bid;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BidRepository extends MongoRepository<Bid, String> {
    List<Bid> findByTaskId(String taskId);
    List<Bid> findByBidderId(String bidderId);
    List<Bid> findByTaskIdAndStatus(String taskId, String status);
}