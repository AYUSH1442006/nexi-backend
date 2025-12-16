package com.example.demo.controllers;

import com.example.demo.models.Bid;
import com.example.demo.models.User;
import com.example.demo.models.Wallet;
import com.example.demo.models.WalletTransaction;
import com.example.demo.repositories.BidRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WalletRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Value("${razorpay.key.id:rzp_test_YOUR_KEY_ID}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:YOUR_KEY_SECRET}")
    private String razorpayKeySecret;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }

            String bidId = data.get("bidId").toString();
            Bid bid = bidRepository.findById(bidId).orElse(null);
            
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bid not found"));
            }

            // Verify the bid belongs to the current user
            if (!bid.getBidderId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You are not authorized to pay for this bid"));
            }

            // Verify bid is in ACCEPTED status
            if (!"ACCEPTED".equals(bid.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only accepted bids can be paid. Current status: " + bid.getStatus()));
            }

            // Create Razorpay order
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            
            int amountInPaise = (int) (bid.getBidAmount() * 100);
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "bid_" + bidId);
            orderRequest.put("payment_capture", 1);

            Order order = razorpay.orders.create(orderRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId", razorpayKeyId);
            
            System.out.println("✅ Razorpay order created: " + order.get("id"));
            
            return ResponseEntity.ok(response);
            
        } catch (RazorpayException e) {
            System.err.println("❌ Razorpay error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Razorpay error: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error creating order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "User not found"));
            }

            String orderId = data.get("razorpayOrderId");
            String paymentId = data.get("razorpayPaymentId");
            String signature = data.get("razorpaySignature");
            String bidId = data.get("bidId");
            
            System.out.println("🔍 Verifying payment for bid: " + bidId);
            
            // Verify signature
            String payload = orderId + "|" + paymentId;
            String generatedSignature = calculateHMAC(payload, razorpayKeySecret);
            
            if (!generatedSignature.equals(signature)) {
                System.err.println("❌ Invalid signature!");
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Invalid payment signature"));
            }

            System.out.println("✅ Signature verified!");

            // Payment verified - Update bid status
            Bid bid = bidRepository.findById(bidId).orElse(null);
            
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Bid not found"));
            }

            // Update bid status to PAID
            bid.setStatus("PAID");
            bidRepository.save(bid);
            
            System.out.println("✅ Bid status updated to PAID");

            // Get or create wallet
            Wallet wallet = walletRepository.findByUserId(user.getId()).orElse(null);
            
            if (wallet == null) {
                wallet = new Wallet();
                wallet.setUserId(user.getId());
                wallet.setBalance(0.0);
                wallet = walletRepository.save(wallet);
            }

            // Deduct amount from wallet
            double newBalance = wallet.getBalance() - bid.getBidAmount();
            wallet.setBalance(Math.max(0, newBalance));

            // ✅ Create transaction record matching your WalletTransaction model
            WalletTransaction transaction = new WalletTransaction();
            transaction.setType("DEBIT");
            transaction.setAmount(bid.getBidAmount());
            transaction.setReference("Payment for task: " + bid.getTaskTitle() + " (Payment ID: " + paymentId + ")");
            transaction.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            wallet.getTransactions().add(transaction);
            walletRepository.save(wallet);
            
            System.out.println("✅ Wallet updated. New balance: " + wallet.getBalance());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified successfully");
            response.put("remainingBalance", wallet.getBalance());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Verification error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    private String calculateHMAC(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes());
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}