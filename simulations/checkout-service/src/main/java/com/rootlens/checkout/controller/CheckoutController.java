package com.rootlens.checkout.controller;

import com.rootlens.checkout.LogForwarder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private static final String SERVICE = "checkout-service";
    private static final Random RANDOM = new Random();
    private static final String[] PRODUCT_NAMES = {
        "Pro Subscription", "Enterprise License", "API Credits Bundle",
        "Storage Upgrade", "Analytics Add-on"
    };

    private final RestTemplate restTemplate;
    private final LogForwarder logForwarder;

    @Value("${services.payment.url:http://localhost:8082}")
    private String paymentUrl;

    @Value("${rootlens.simulation.auto-trigger.enabled:false}")
    private boolean autoTriggerEnabled;

    public CheckoutController(RestTemplate restTemplate, LogForwarder logForwarder) {
        this.restTemplate = restTemplate;
        this.logForwarder = logForwarder;
    }

    @GetMapping
    public ResponseEntity<?> checkout() {
        return executeCheckout(randomOrderId(), randomAmount());
    }

    @PostMapping
    public ResponseEntity<?> checkoutPost(@RequestBody CheckoutRequest request) {
        String orderId = (request.getOrderId() != null && !request.getOrderId().isBlank())
                ? request.getOrderId()
                : randomOrderId();
        double amount = request.getAmount() > 0 ? request.getAmount() : randomAmount();
        return executeCheckout(orderId, amount);
    }

    // fixedDelay waits for previous execution to finish — prevents queuing under slow payment (up to 4.5s)
    @Scheduled(fixedDelayString = "${rootlens.simulation.auto-trigger.interval-ms:30000}")
    public void scheduledCheckout() {
        if (!autoTriggerEnabled) return;
        executeCheckout("AUTO-" + randomSuffix(), randomAmount());
    }

    private ResponseEntity<?> executeCheckout(String orderId, double amount) {
        long startMs = System.currentTimeMillis();
        String product = PRODUCT_NAMES[RANDOM.nextInt(PRODUCT_NAMES.length)];

        logForwarder.sendWarn(SERVICE,
            "Initiating payment for orderId=" + orderId
            + " amount=$" + String.format("%.2f", amount)
            + " product=" + product);

        try {
            String paymentResponse = restTemplate.getForObject(
                paymentUrl + "/payment", String.class);
            long elapsed = System.currentTimeMillis() - startMs;

            logForwarder.sendInfo(SERVICE,
                "Checkout completed for orderId=" + orderId
                + " amount=$" + String.format("%.2f", amount)
                + " in " + elapsed + "ms");

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "orderId", orderId,
                "amount", amount,
                "product", product,
                "paymentResponse", paymentResponse != null ? paymentResponse : ""
            ));

        } catch (RestClientResponseException e) {
            // Caught before RestClientException — subclass, carries HTTP status + body
            long elapsed = System.currentTimeMillis() - startMs;
            logForwarder.sendError(SERVICE,
                "Checkout FAILED for orderId=" + orderId
                + " amount=$" + String.format("%.2f", amount)
                + " after " + elapsed + "ms: HTTP " + e.getStatusCode().value()
                + " — " + e.getResponseBodyAsString());
            return ResponseEntity.status(503)
                    .body(Map.of(
                        "error", "Payment service error",
                        "httpStatus", e.getStatusCode().value(),
                        "orderId", orderId,
                        "service", SERVICE
                    ));

        } catch (RestClientException e) {
            // Connection error — no HTTP response available
            long elapsed = System.currentTimeMillis() - startMs;
            logForwarder.sendError(SERVICE,
                "Checkout FAILED for orderId=" + orderId
                + " after " + elapsed + "ms: " + e.getMessage());
            return ResponseEntity.status(503)
                    .body(Map.of(
                        "error", "Payment service unavailable",
                        "orderId", orderId,
                        "service", SERVICE
                    ));
        }
    }

    private static String randomOrderId() {
        return "ORD-" + randomSuffix();
    }

    private static String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static double randomAmount() {
        return Math.round((10.0 + RANDOM.nextDouble() * 490.0) * 100.0) / 100.0;
    }

    static class CheckoutRequest {
        private String orderId;
        private double amount;
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }
}
