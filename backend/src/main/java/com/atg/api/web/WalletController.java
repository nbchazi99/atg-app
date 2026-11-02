package com.atg.api.web;

import com.atg.api.model.*;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.*;
import com.atg.api.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WalletController extends BaseController {

    private final WalletTransactionRepository transactions;
    private final DepositRequestRepository deposits;
    private final WithdrawalRequestRepository withdrawals;
    private final PurchaseRequestRepository purchases;
    private final WalletService wallet;
    private final RealtimeHub realtime;

    public WalletController(UserRepository users, WalletTransactionRepository transactions,
                            DepositRequestRepository deposits, WithdrawalRequestRepository withdrawals,
                            PurchaseRequestRepository purchases, WalletService wallet,
                            RealtimeHub realtime) {
        super(users);
        this.transactions = transactions;
        this.deposits = deposits;
        this.withdrawals = withdrawals;
        this.purchases = purchases;
        this.wallet = wallet;
        this.realtime = realtime;
    }

    @GetMapping("/wallet")
    public Map<String, Object> wallet() {
        return Map.of(
                "transactions", transactions.findByUserIdOrderByCreatedAtDesc(me().getId()),
                "pendingDeposits", deposits.findByUserIdOrderByCreatedAtDesc(me().getId()).stream()
                        .filter(d -> d.getStatus() == DepositRequest.Status.PENDING).count(),
                "pendingWithdrawals", withdrawals.findByUserIdOrderByCreatedAtDesc(me().getId()).stream()
                        .filter(w -> w.getStatus() == WithdrawalRequest.Status.PENDING).count()
        );
    }

    @GetMapping("/deposits")
    public List<DepositRequest> deposits() {
        return deposits.findByUserIdOrderByCreatedAtDesc(me().getId());
    }

    @PostMapping("/deposits")
    public DepositRequest createDeposit(@Valid @RequestBody Dtos.DepositReq req) {
        User user = me();
        DepositRequest created = wallet.createDeposit(user, req);
        realtime.publishWalletChange(user.getId(), "deposit-created");
        return created;
    }

    @GetMapping("/withdrawals")
    public List<WithdrawalRequest> withdrawals() {
        return withdrawals.findByUserIdOrderByCreatedAtDesc(me().getId());
    }

    @GetMapping("/withdrawals/{id}")
    public WithdrawalRequest withdrawal(@PathVariable Long id) {
        WithdrawalRequest w = withdrawals.findById(id)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Not found"));
        if (!w.getUserId().equals(me().getId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not your request");
        }
        return w;
    }

    @PostMapping("/withdrawals")
    public WithdrawalRequest createWithdrawal(@Valid @RequestBody Dtos.WithdrawReq req) {
        User user = me();
        WithdrawalRequest created = wallet.createWithdrawal(user, req);
        realtime.publishWalletChange(user.getId(), "withdrawal-created");
        return created;
    }

    @GetMapping("/purchases")
    public List<PurchaseRequest> purchases() {
        return purchases.findByUserIdOrderByCreatedAtDesc(me().getId());
    }

    @PostMapping("/purchases")
    public PurchaseRequest createPurchase(@Valid @RequestBody Dtos.PurchaseReq req) {
        User user = me();
        PurchaseRequest created = wallet.createPurchase(user, req);
        realtime.publishWalletChange(user.getId(), "purchase-created");
        return created;
    }
}
