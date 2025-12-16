package com.example.demo.controllers;

import com.example.demo.models.Bid;
import com.example.demo.models.Task;
import com.example.demo.models.User;
import com.example.demo.models.Wallet;
import com.example.demo.models.WalletTransaction;
import com.example.demo.repositories.BidRepository;
import com.example.demo.repositories.TaskRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidController {

    @Autowired
    private BidRepository bidRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WalletRepository walletRepo;

    // Place a bid on a task
    @PostMapping("/place")
    public ResponseEntity<?> placeBid(@RequestBody Bid bid) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            // Get the task
            Optional<Task> taskOpt = taskRepo.findById(bid.getTaskId());
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }

            Task task = taskOpt.get();

            // Can't bid on your own task
            if (task.getPosterId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "You cannot bid on your own task"));
            }

            // Can't bid on assigned/completed tasks
            if (!task.getStatus().equals("OPEN")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "This task is no longer open for bidding"));
            }

            // Set bid details
            bid.setBidderId(user.getId());
            bid.setBidderName(user.getName());
            bid.setBidderEmail(user.getEmail());
            bid.setTaskTitle(task.getTitle());
            bid.setStatus("PENDING");
            bid.setCreatedAt(LocalDateTime.now().toString());

            Bid savedBid = bidRepo.save(bid);

            // Update task bid count
            task.setBidCount(task.getBidCount() + 1);
            taskRepo.save(task);

            return ResponseEntity.ok(savedBid);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get all bids for a task
    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getBidsForTask(@PathVariable String taskId) {
        try {
            List<Bid> bids = bidRepo.findByTaskId(taskId);
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get my bids
    @GetMapping("/my-bids")
    public ResponseEntity<?> getMyBids() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            List<Bid> bids = bidRepo.findByBidderId(user.getId());
            return ResponseEntity.ok(bids);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Accept a bid (assign task)
    @PostMapping("/accept/{bidId}")
    public ResponseEntity<?> acceptBid(@PathVariable String bidId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            // Get the bid
            Optional<Bid> bidOpt = bidRepo.findById(bidId);
            if (!bidOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Bid not found"));
            }

            Bid bid = bidOpt.get();

            // Get the task
            Optional<Task> taskOpt = taskRepo.findById(bid.getTaskId());
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }

            Task task = taskOpt.get();

            // Only task poster can accept bids
            if (!task.getPosterId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the task poster can accept bids"));
            }

            // Accept the bid
            bid.setStatus("ACCEPTED");
            bidRepo.save(bid);

            // Assign task to bidder
            task.setAssignedTo(bid.getBidderId());
            task.setAssignedToName(bid.getBidderName());
            task.setStatus("ASSIGNED");
            task.setUpdatedAt(LocalDateTime.now().toString());
            taskRepo.save(task);

            // Reject all other pending bids
            List<Bid> otherBids = bidRepo.findByTaskIdAndStatus(task.getId(), "PENDING");
            for (Bid otherBid : otherBids) {
                if (!otherBid.getId().equals(bidId)) {
                    otherBid.setStatus("REJECTED");
                    bidRepo.save(otherBid);
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "Bid accepted successfully",
                "bid", bid,
                "task", task
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Pay from wallet after bid is accepted
    @PostMapping("/{bidId}/pay-from-wallet")
    public ResponseEntity<?> payFromWallet(@PathVariable String bidId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            Optional<Bid> bidOpt = bidRepo.findById(bidId);
            if (!bidOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Bid not found"));
            }

            Bid bid = bidOpt.get();

            Optional<Task> taskOpt = taskRepo.findById(bid.getTaskId());
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }

            Task task = taskOpt.get();

            // Get or create wallet
            Wallet wallet = walletRepo.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            // Check balance
            if (wallet.getBalance() < bid.getBidAmount()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Insufficient wallet balance"));
            }

            // Deduct amount from wallet
            wallet.setBalance(wallet.getBalance() - bid.getBidAmount());

            // Create transaction record
            WalletTransaction tx = new WalletTransaction();
            tx.setType("DEBIT");
            tx.setAmount(bid.getBidAmount());
            tx.setReference("Task Payment: " + task.getId());
            tx.setCreatedAt(LocalDateTime.now().toString());

            wallet.getTransactions().add(tx);
            walletRepo.save(wallet);

            // Update bid and task status
            bid.setStatus("PAID");
            task.setStatus("IN_PROGRESS");

            bidRepo.save(bid);
            taskRepo.save(task);

            return ResponseEntity.ok(Map.of(
                "message", "Payment successful",
                "remainingBalance", wallet.getBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Reject a bid
    @PostMapping("/reject/{bidId}")
    public ResponseEntity<?> rejectBid(@PathVariable String bidId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            Optional<Bid> bidOpt = bidRepo.findById(bidId);
            if (!bidOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Bid not found"));
            }

            Bid bid = bidOpt.get();

            Optional<Task> taskOpt = taskRepo.findById(bid.getTaskId());
            if (!taskOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
            }

            Task task = taskOpt.get();

            // Only task poster can reject bids
            if (!task.getPosterId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only the task poster can reject bids"));
            }

            bid.setStatus("REJECTED");
            bidRepo.save(bid);

            return ResponseEntity.ok(Map.of("message", "Bid rejected successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Delete a bid (only by bidder before it's accepted)
    @DeleteMapping("/{bidId}")
    public ResponseEntity<?> deleteBid(@PathVariable String bidId) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            Optional<Bid> bidOpt = bidRepo.findById(bidId);
            if (!bidOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Bid not found"));
            }

            Bid bid = bidOpt.get();

            // Only bidder can delete their own bid
            if (!bid.getBidderId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only delete your own bids"));
            }

            // Can't delete accepted bids
            if (bid.getStatus().equals("ACCEPTED")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot delete an accepted bid"));
            }

            bidRepo.deleteById(bidId);

            // Update task bid count
            Optional<Task> taskOpt = taskRepo.findById(bid.getTaskId());
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                task.setBidCount(Math.max(0, task.getBidCount() - 1));
                taskRepo.save(task);
            }

            return ResponseEntity.ok(Map.of("message", "Bid deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}