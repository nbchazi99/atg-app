package com.atg.api.web;

import com.atg.api.model.PaymentTransaction;
import com.atg.api.model.User;
import com.atg.api.repo.UserRepository;
import com.atg.api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController extends BaseController {

    private final PaymentService payments;

    public PaymentController(UserRepository users, PaymentService payments) {
        super(users);
        this.payments = payments;
    }

    /** Creates a gateway order; the EcoCash prompt is pushed to the customer's phone. */
    @PostMapping("/intents")
    public Map<String, Object> createIntent(@Valid @RequestBody Dtos.PaymentIntentReq req) {
        User user = me();
        PaymentService.Checkout checkout = payments.createIntent(user, req);
        return Map.of(
                "order", checkout.order(),
                "ecocashStatus", checkout.ecocashStatus() == null
                        ? "" : checkout.ecocashStatus());
    }

    /**
     * Status the client polls after returning from checkout. Reads trigger a
     * live server-side verification, so the frontend never decides anything.
     */
    @GetMapping("/{orderId}")
    public PaymentTransaction status(@PathVariable String orderId) {
        return payments.statusFor(me(), orderId);
    }

    @GetMapping("/history")
    public List<PaymentTransaction> history() {
        return payments.history(me());
    }

    /**
     * Dischub callback. Unauthenticated by design (the provider posts to it),
     * but it only ever triggers a server-to-server re-verification — a forged
     * callback cannot settle anything.
     */
    @PostMapping("/webhook/dischub")
    public Map<String, String> webhook(@RequestBody(required = false) String body) {
        payments.handleCallback(body);
        return Map.of("status", "ok");
    }
}
