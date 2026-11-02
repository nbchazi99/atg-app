package com.atg.api.web;

import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController extends BaseController {

    private final WalletTransactionRepository transactions;
    private final DepositRequestRepository deposits;
    private final WithdrawalRequestRepository withdrawals;
    private final PurchaseRequestRepository purchases;
    private final EnrollmentRepository enrollments;
    private final RealtimeHub realtime;

    public MeController(UserRepository users, WalletTransactionRepository transactions,
                        DepositRequestRepository deposits, WithdrawalRequestRepository withdrawals,
                        PurchaseRequestRepository purchases, EnrollmentRepository enrollments,
                        RealtimeHub realtime) {
        super(users);
        this.transactions = transactions;
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.purchases = purchases;
        this.enrollments = enrollments;
        this.realtime = realtime;
    }

    @GetMapping
    public Dtos.UserView profile() {
        return Dtos.UserView.of(super.me());
    }

    @PutMapping
    public Dtos.UserView update(@RequestBody Map<String, String> body) {
        User user = me();
        if (body.get("name") != null && !body.get("name").isBlank()) user.setName(body.get("name").trim());
        if (body.get("phone") != null) user.setPhone(body.get("phone").trim());
        users.save(user);
        realtime.publish(RealtimeHub.USERS, java.util.Map.of("userId", user.getId(), "kind", "profile"));
        return Dtos.UserView.of(user);
    }

    @GetMapping("/history")
    public Map<String, Object> history() {
        User user = me();
        return Map.of(
                "transactions", transactions.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "deposits", deposits.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "withdrawals", withdrawals.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "purchases", purchases.findByUserIdOrderByCreatedAtDesc(user.getId()),
                "enrollments", enrollments.findByUserId(user.getId())
        );
    }
}
