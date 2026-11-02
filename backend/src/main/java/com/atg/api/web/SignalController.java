package com.atg.api.web;

import com.atg.api.model.Signal;
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
        if (symbol != null && !symbol.isBlank()) {
            return signals.findBySymbolOrderByCreatedAtDesc(symbol.toUpperCase(Locale.ROOT));
        }
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            Signal.Status parsed;
            try {
                parsed = Signal.Status.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown signal status");
            }
            return signals.findByStatusOrderByCreatedAtDesc(parsed);
        }
        return signals.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public Signal one(@PathVariable Long id) {
        return signals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signal not found"));
    }
}
