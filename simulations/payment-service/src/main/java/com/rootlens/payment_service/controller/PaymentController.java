package com.rootlens.payment_service.controller;

import com.rootlens.payment_service.LogForwarder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private static final String SERVICE = "payment-service";
    private static final Random RANDOM = new Random();

    // Cumulative failure probability thresholds
    private static final double THRESHOLD_DB_TIMEOUT          = 0.15;
    private static final double THRESHOLD_GATEWAY_UNAVAILABLE = 0.23; // +0.08
    private static final double THRESHOLD_FRAUD_DETECTED      = 0.30; // +0.07
    private static final double THRESHOLD_IDEMPOTENCY_CONFLICT = 0.35; // +0.05
    // remaining 0.65 → SUCCESS

    private final LogForwarder logForwarder;

    public PaymentController(LogForwarder logForwarder) {
        this.logForwarder = logForwarder;
    }

    @GetMapping
    public String processPayment() throws InterruptedException {
        long startMs = System.currentTimeMillis();
        double roll = RANDOM.nextDouble();
        boolean willFail = roll < THRESHOLD_IDEMPOTENCY_CONFLICT;

        // Degraded path is slow — simulates DB/gateway timeout behaviour
        if (willFail) {
            Thread.sleep(1200 + (long)(RANDOM.nextDouble() * 3300)); // 1.2–4.5s
        } else {
            Thread.sleep(80 + (long)(RANDOM.nextDouble() * 220));    // 80–300ms
        }

        long elapsedMs = System.currentTimeMillis() - startMs;

        if (roll < THRESHOLD_DB_TIMEOUT) {
            emitFailure("DATABASE_TIMEOUT",
                "Connection pool exhausted: unable to acquire DB connection after 5000ms "
                + "(pool_size=10, queue_depth=47, wait_timeout=5000ms)", elapsedMs);
        }
        if (roll < THRESHOLD_GATEWAY_UNAVAILABLE) {
            emitFailure("PAYMENT_GATEWAY_UNAVAILABLE",
                "Stripe gateway returned 503: Service Unavailable "
                + "(retry attempt 3/3 failed, circuit_breaker=OPEN, next_retry_at=+30s)", elapsedMs);
        }
        if (roll < THRESHOLD_FRAUD_DETECTED) {
            emitFailure("FRAUD_DETECTION_TRIGGERED",
                "Transaction blocked by fraud detection engine: "
                + "risk_score=0.92, threshold=0.80, rule=VELOCITY_CHECK, "
                + "reason=5_txns_in_60s_from_same_device", elapsedMs);
        }
        if (roll < THRESHOLD_IDEMPOTENCY_CONFLICT) {
            String key = "idem_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            emitFailure("IDEMPOTENCY_KEY_CONFLICT",
                "Duplicate payment attempt detected: idempotencyKey=" + key
                + " already processed at epoch=" + (System.currentTimeMillis() / 1000 - 45), elapsedMs);
        }

        // SUCCESS path
        logForwarder.sendInfo(SERVICE, "Payment processed successfully in " + elapsedMs + "ms");
        logForwarder.sendMetric(SERVICE, "payment.processing.latency.ms", (double) elapsedMs);
        logForwarder.sendMetric(SERVICE, "payment.success.rate", 1.0);
        return "Payment processed";
    }

    private void emitFailure(String code, String detail, long elapsedMs) {
        // Log + metrics emitted HERE before throwing — @ExceptionHandler only builds HTTP response
        logForwarder.sendError(SERVICE,
            "[" + code + "] " + detail + " (elapsed=" + elapsedMs + "ms)");
        logForwarder.sendMetric(SERVICE, "payment.processing.latency.ms", (double) elapsedMs);
        logForwarder.sendMetric(SERVICE, "payment.success.rate", 0.0);
        throw new RuntimeException("[" + code + "] " + detail);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handlePaymentError(RuntimeException ex) {
        return ResponseEntity.internalServerError()
                .body(Map.of("error", ex.getMessage(), "service", SERVICE));
    }
}
