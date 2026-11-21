package com.atg.api.web;

import com.atg.api.model.Signal;
import com.atg.api.model.User;
import com.atg.api.repo.SignalRepository;
import com.atg.api.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/signals")
public class SignalController extends BaseController {

    private final SignalRepository signals;

    public SignalController(UserRepository users, SignalRepository signals) {
        super(users);
        this.signals = signals;
    }

    @GetMapping
    public List<Signal> list(@RequestParam(required = false) String status,
                             @RequestParam(required = false) String symbol) {
        User user = me();
        List<Signal> results;
        if (symbol != null && !symbol.isBlank()) {
            results = signals.findBySymbolOrderByCreatedAtDesc(symbol.toUpperCase(Locale.ROOT));
        } else if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            Signal.Status parsed;
            try {
                parsed = Signal.Status.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw bad("Unknown signal status");
            }
            results = signals.findByStatusOrderByCreatedAtDesc(parsed);
        } else {
            results = signals.findAllByOrderByCreatedAtDesc();
        }
        return results.stream()
                .filter(signal -> MembershipAccess.canAccess(
                        user, MembershipAccess.requiredForSignal(signal)))
                .toList();
    }

    @GetMapping("/{id}")
    public Signal one(@PathVariable Long id) {
        Signal signal = signals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signal not found"));
        MembershipAccess.requireAccess(me(), MembershipAccess.requiredForSignal(signal), "Signal");
        return signal;
    }
}
