package hotelBackend.controllers;

import hotelBackend.dtos.PaymentInitiateRequest;
import hotelBackend.dtos.PaymentResponse;
import hotelBackend.exceptions.PaymentProcessingException;
import hotelBackend.response.ErrorResponse;
import hotelBackend.services.PaystackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "${cors.allowed-origins}") // Use the property
@Slf4j
@RequiredArgsConstructor
public class PaymentController {
    private final PaystackService paystackService;

    // Payment initiation endpoint
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentInitiateRequest request) {
        try {
            log.info("Payment initiation request received: {}", request);
            PaymentResponse response = paystackService.initiatePayment(request);
            return ResponseEntity.ok(response);
        } catch (PaymentProcessingException e) {
            log.warn("Payment processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "BOOKING_CONFLICT",
                    e.getMessage(),
                    request.getStartDate(),
                    request.getEndDate()
            ));
        } catch (Exception e) {
            log.error("Unexpected error during payment initiation", e);
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "PAYMENT_ERROR",
                    "An unexpected error occurred"
            ));
        }
    }

    // Payment verification endpoint
    @GetMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestParam("reference") String reference) {
        log.info("Verifying payment for reference: {}", reference);
        try {
            boolean isVerified = paystackService.verifyPayment(reference);
            if (isVerified) {
                return ResponseEntity.ok(Map.of(
                        "status", true,
                        "message", "Payment verified successfully"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "status", false,
                        "message", "Payment verification failed"
                ));
            }
        } catch (Exception e) {
            log.error("Payment verification failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", false,
                    "message", "Payment verification failed: " + e.getMessage()
            ));
        }
    }

    // Success redirect endpoint
    @GetMapping("/success")
    public ResponseEntity<?> paymentSuccess(@RequestParam("reference") String reference) {
        log.info("Payment success for reference: {}", reference);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Payment completed successfully",
                "reference", reference
        ));
    }

    // Cancel redirect endpoint
    @GetMapping("/cancel")
    public ResponseEntity<?> paymentCancelled(@RequestParam("reference") String reference) {
        log.info("Payment cancelled for reference: {}", reference);
        return ResponseEntity.ok(Map.of(
                "status", "cancelled",
                "message", "Payment was cancelled",
                "reference", reference
        ));
    }
}