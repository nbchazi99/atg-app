package com.atg.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Thin client for the Dischub gateway (dischub-API-Integration-Redirect.md).
 *
 * <p>Dischub exposes no webhook signature, so nothing coming back from a
 * callback is trusted: {@link #fetchStatus} re-queries the provider
 * server-to-server with our own API key and that response is the only thing
 * that can settle a payment.</p>
 */
@Service
public class DischubClient {

    private static final Logger log = LoggerFactory.getLogger(DischubClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final String recipient;
    private final RestClient http;
    private final ObjectMapper json;

    public record OrderCreated(boolean ok, String message, String ecocashStatus) {
    }

    public record ProviderStatus(boolean found, String status, String reason,
                                 String currency, BigDecimal amount, String sender,
                                 String paymentMethod, String timestamp, String raw,
                                 String message) {
        public boolean isSuccess() { return found && "success".equalsIgnoreCase(status); }
        public boolean isPending() { return found && "pending".equalsIgnoreCase(status); }
        public boolean isFailed() { return found && "failed".equalsIgnoreCase(status); }
    }

    public DischubClient(
            @Value("${app.dischub.base-url:https://dischub.co.zw}") String baseUrl,
            @Value("${app.dischub.api-key:}") String apiKey,
            @Value("${app.dischub.recipient:}") String recipient,
            ObjectMapper json) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.recipient = recipient == null ? "" : recipient.trim();
        this.json = json;

        java.net.http.HttpClient jdkClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkClient);
        factory.setReadTimeout(Duration.ofSeconds(25));
        this.http = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(factory)
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty() && !recipient.isEmpty();
    }

    /**
     * Creates an order through the NON-redirect integration (ecocash.md):
     * an EcoCash prompt is pushed to the customer's handset, the customer
     * approves it there, and we poll for the status — the customer never
     * leaves the app. This integration supports neither {@code redirect_url}
     * nor {@code callback_url} and has no test environment, so no {@code mode}
     * is sent.
     *
     * @param ecocashNumber EcoCash number that receives the payment prompt
     */
    public OrderCreated createOrder(String orderId, String sender, BigDecimal amount,
                                    String ecocashNumber) {
        if (!isConfigured()) {
            return new OrderCreated(false,
                    "Payment gateway is not configured. Set DISCHUB_API_KEY and DISCHUB_RECIPIENT.",
                    null);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", orderId);
        body.put("sender", sender);
        body.put("recipient", recipient);
        body.put("amount", amount);
        body.put("currency", "USD");
        body.put("payment_method", "ecocash");
        body.put("payment_details", Map.of("ecocash_number", ecocashNumber));

        try {
            JsonNode res = post("/api/orders/create/", body);
            String status = text(res, "status");
            String message = text(res, "message");
            boolean ok = "success".equalsIgnoreCase(status);
            if (!ok) log.warn("Dischub order {} rejected: {}", orderId, message);
            return new OrderCreated(ok, message, text(res, "ecocash_status"));
        } catch (Exception e) {
            log.error("Dischub order {} call failed", orderId, e);
            return new OrderCreated(false,
                    "Could not reach the payment gateway: " + e.getMessage(), null);
        }
    }

    public ProviderStatus fetchStatus(String orderId) {
        if (!isConfigured()) {
            return new ProviderStatus(false, null, null, null, null, null, null, null, null,
                    "Payment gateway is not configured. Set DISCHUB_API_KEY and DISCHUB_RECIPIENT.");
        }
        Map<String, Object> body = Map.of("order_id", orderId, "recipient", recipient);
        try {
            JsonNode res = post("/api/payment/status/3/step/", body);
            if (res.has("order_id")) {
                BigDecimal amount = null;
                String rawAmount = text(res, "amount");
                if (rawAmount != null && !rawAmount.isBlank()) {
                    try {
                        amount = new BigDecimal(rawAmount.trim());
                    } catch (NumberFormatException ignored) {
                        // keep null; caller treats an unparsable amount as a mismatch
                    }
                }
                return new ProviderStatus(true, text(res, "status"), text(res, "status_reason"),
                        text(res, "currency"), amount, text(res, "sender"),
                        text(res, "payment_method"), text(res, "timestamp"),
                        res.toString(), null);
            }
            String message = text(res, "message");
            if (message != null && message.toLowerCase().contains("no order found")) {
                return new ProviderStatus(false, null, null, null, null, null, null, null, null,
                        "Order not found at the gateway");
            }
            log.warn("Dischub status query for {} returned an error: {}", orderId, message);
            return new ProviderStatus(false, null, null, null, null, null, null, null,
                    res.toString(),
                    message == null ? "Unexpected gateway response" : message);
        } catch (Exception e) {
            // 404 = the order never reached Dischub (failed creation or test-era
            // records); that is an expected state, not an infrastructure error.
            if (String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT).contains("no order found")) {
                log.debug("Dischub has no order {}", orderId);
                return new ProviderStatus(false, null, null, null, null, null, null, null, null,
                        "Order not found at the gateway");
            }
            log.error("Dischub status query for {} failed", orderId, e);
            return new ProviderStatus(false, null, null, null, null, null, null, null, null,
                    "Could not reach the payment gateway: " + e.getMessage());
        }
    }

    private JsonNode post(String path, Map<String, Object> body) throws Exception {
        String payload = json.writeValueAsString(body);
        String response = http.post()
                .uri(URI.create(baseUrl + path))
                .header("X-Api-Key", apiKey)
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(String.class);
        return json.readTree(response == null ? "{}" : response);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private static String stripTrailingSlash(String url) {
        String value = url == null || url.isBlank() ? "https://dischub.co.zw" : url.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
