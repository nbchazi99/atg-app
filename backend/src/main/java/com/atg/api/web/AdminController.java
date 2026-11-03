package com.atg.api.web;

import com.atg.api.config.JwtService;
import com.atg.api.model.*;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import com.atg.api.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {

    private final WalletService wallet;
    private final WalletTransactionRepository transactions;
    private final DepositRequestRepository deposits;
    private final WithdrawalRequestRepository withdrawals;
    private final PurchaseRequestRepository purchases;
    private final SignalRepository signals;
    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final RealtimeHub realtime;
    private final JwtService jwt;
    private final PasswordEncoder encoder;

    public AdminController(UserRepository users, WalletService wallet,
                           WalletTransactionRepository transactions, DepositRequestRepository deposits,
                           WithdrawalRequestRepository withdrawals, PurchaseRequestRepository purchases,
                           SignalRepository signals, CourseRepository courses, LessonRepository lessons,
                           RealtimeHub realtime, JwtService jwt, PasswordEncoder encoder) {
        super(users);
        this.wallet = wallet;
        this.transactions = transactions;
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.purchases = purchases;
        this.signals = signals;
        this.courses = courses;
        this.lessons = lessons;
        this.realtime = realtime;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @PutMapping("/account")
    public Dtos.AuthRes updateAccount(@Valid @RequestBody Dtos.AdminAccountReq req) {
        User admin = me();
        if (!encoder.matches(req.currentPassword(), admin.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current password is incorrect");
        }
        String email = req.email() == null || req.email().isBlank() ? null : req.email().trim().toLowerCase();
        if (email != null && !email.equalsIgnoreCase(admin.getEmail())) {
            if (users.existsByEmailIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
            }
            admin.setEmail(email);
        }
        if (req.password() != null && !req.password().isBlank()) {
            admin.setPasswordHash(encoder.encode(req.password()));
        }
        users.save(admin);
        return new Dtos.AuthRes(jwt.generate(admin.getEmail(), admin.getRole().name()),
                Dtos.UserView.of(admin));
    }

    @GetMapping("/users")
    public List<Dtos.UserView> users() {
        return users.findAll().stream().map(Dtos.UserView::of).toList();
    }

    @PatchMapping("/users/{id}/membership")
    public Dtos.UserView setMembership(@PathVariable Long id, @RequestBody Dtos.MembershipReq req) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setMembership(User.Membership.valueOf(req.membership().toUpperCase(Locale.ROOT)));
        users.save(user);
        realtime.publish(RealtimeHub.USERS, Map.of("userId", user.getId(), "kind", "membership"));
        realtime.publish(RealtimeHub.homeScope(user.getId()), Map.of("kind", "membership"));
        realtime.publish(RealtimeHub.ROOMS, Map.of("kind", "membership"));
        return Dtos.UserView.of(user);
    }

    @GetMapping("/requests")
    public Map<String, Object> requests() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("deposits", deposits.findAllByOrderByCreatedAtDesc());
        res.put("withdrawals", withdrawals.findAllByOrderByCreatedAtDesc());
        res.put("purchases", purchases.findAllByOrderByCreatedAtDesc());
        res.put("transactions", transactions.findAll());
        return res;
    }

    @PostMapping("/deposits/{id}/decide")
    public DepositRequest decideDeposit(@PathVariable Long id, @RequestBody Dtos.DecisionReq req) {
        DepositRequest decided = wallet.decideDeposit(id, req.approve(), req.note());
        realtime.publishWalletChange(decided.getUserId(), "deposit-decided");
        return decided;
    }

    @PostMapping("/withdrawals/{id}/decide")
    public WithdrawalRequest decideWithdrawal(@PathVariable Long id, @RequestBody Dtos.DecisionReq req) {
        WithdrawalRequest decided = wallet.decideWithdrawal(id, req.approve(), req.note());
        realtime.publishWalletChange(decided.getUserId(), "withdrawal-decided");
        return decided;
    }

    @PostMapping("/purchases/{id}/decide")
    public PurchaseRequest decidePurchase(@PathVariable Long id, @RequestBody Dtos.DecisionReq req) {
        PurchaseRequest decided = wallet.decidePurchase(id, req.approve(), req.note());
        realtime.publishWalletChange(decided.getUserId(), "purchase-decided");
        realtime.publish(RealtimeHub.USERS, Map.of("userId", decided.getUserId(), "kind", "purchase"));
        realtime.publish(RealtimeHub.ROOMS, Map.of("kind", "purchase"));
        realtime.publish(RealtimeHub.COURSES, Map.of("kind", "purchase"));
        return decided;
    }

    @GetMapping("/signals")
    public List<Signal> allSignals() {
        return signals.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/signals")
    public Signal createSignal(@RequestBody Dtos.SignalReq req) {
        Signal s = new Signal();
        apply(s, req);
        Signal saved = signals.save(s);
        realtime.publish(RealtimeHub.SIGNALS, Map.of("signalId", saved.getId(), "kind", "created"));
        realtime.publish(RealtimeHub.HOME, Map.of("kind", "signals"));
        return saved;
    }

    @PutMapping("/signals/{id}")
    public Signal updateSignal(@PathVariable Long id, @RequestBody Dtos.SignalReq req) {
        Signal s = signals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Signal not found"));
        apply(s, req);
        Signal saved = signals.save(s);
        realtime.publish(RealtimeHub.SIGNALS, Map.of("signalId", saved.getId(), "kind", "updated"));
        realtime.publish(RealtimeHub.HOME, Map.of("kind", "signals"));
        return saved;
    }

    @DeleteMapping("/signals/{id}")
    public Map<String, Object> deleteSignal(@PathVariable Long id) {
        signals.deleteById(id);
        realtime.publish(RealtimeHub.SIGNALS, Map.of("signalId", id, "kind", "deleted"));
        realtime.publish(RealtimeHub.HOME, Map.of("kind", "signals"));
        return Map.of("deleted", true);
    }

    private void apply(Signal s, Dtos.SignalReq req) {
        if (req.title() != null) s.setTitle(req.title());
        if (req.subtitle() != null) s.setSubtitle(req.subtitle());
        if (req.symbol() != null) s.setSymbol(req.symbol().toUpperCase(Locale.ROOT));
        if (req.side() != null) s.setSide(Signal.Side.valueOf(req.side().toUpperCase(Locale.ROOT)));
        if (req.status() != null) s.setStatus(Signal.Status.valueOf(req.status().toUpperCase(Locale.ROOT)));
        if (req.roomLabel() != null) s.setRoomLabel(req.roomLabel());
        if (req.entry() != null) s.setEntry(req.entry());
        if (req.stopLoss() != null) s.setStopLoss(req.stopLoss());
        if (req.targets() != null) s.setTargets(req.targets());
        if (req.ruleName() != null) s.setRuleName(req.ruleName());
        if (req.participants() != null) s.setParticipants(req.participants());
    }

    @PostMapping("/courses")
    public Course createCourse(@RequestBody Dtos.CourseReq req) {
        Course c = new Course();
        c.setTitle(req.title());
        c.setSubtitle(req.subtitle());
        c.setDescription(req.description());
        c.setPrice(req.price() == null ? BigDecimal.ZERO : req.price());
        c.setLevel(req.level() == null ? "Beginner" : req.level());
        c.setImageUrl(req.imageUrl());
        courses.save(c);
        if (req.lessons() != null) {
            int i = 0;
            for (Dtos.LessonReq lr : req.lessons()) {
                Lesson l = new Lesson();
                l.setCourseId(c.getId());
                l.setTitle(lr.title());
                l.setDurationMin(lr.durationMin() == null ? 10 : lr.durationMin());
                l.setContent(lr.content());
                l.setVideoUrl(lr.videoUrl());
                l.setOrderIndex(i++);
                lessons.save(l);
            }
        }
        c.setLessonsCount((int) lessons.countByCourseId(c.getId()));
        Course saved = courses.save(c);
        realtime.publish(RealtimeHub.COURSES, Map.of("courseId", saved.getId(), "kind", "created"));
        return saved;
    }

    @DeleteMapping("/courses/{id}")
    public Map<String, Object> deleteCourse(@PathVariable Long id) {
        lessons.findByCourseIdOrderByOrderIndexAsc(id).forEach(lessons::delete);
        courses.deleteById(id);
        realtime.publish(RealtimeHub.COURSES, Map.of("courseId", id, "kind", "deleted"));
        return Map.of("deleted", true);
    }

    @GetMapping("/settings")
    public AppSettings getSettings() {
        return wallet.settings();
    }

    @PutMapping("/settings")
    public AppSettings updateSettings(@RequestBody Dtos.SettingsReq req) {
        AppSettings saved = wallet.updateSettings(req);
        realtime.publish(RealtimeHub.SETTINGS, Map.of("kind", "settings"));
        realtime.publish(RealtimeHub.HOME, Map.of("kind", "settings"));
        return saved;
    }
}
