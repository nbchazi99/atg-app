package com.atg.api.web;

import com.atg.api.model.AppSettings;
import com.atg.api.model.Signal;
import com.atg.api.model.User;
import com.atg.api.repo.SignalRepository;
import com.atg.api.repo.UserRepository;
import com.atg.api.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HomeController extends BaseController {

    private final SignalRepository signals;
    private final WalletService wallet;

    public HomeController(UserRepository users, SignalRepository signals, WalletService wallet) {
        super(users);
        this.signals = signals;
        this.wallet = wallet;
    }

    @GetMapping("/home")
    public Map<String, Object> home() {
        User user = me();
        AppSettings s = wallet.settings();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("name", user.getName());
        res.put("points", user.getPoints());
        res.put("membership", user.getMembership().name());
        res.put("liveCount", signals.findByStatusOrderByCreatedAtDesc(Signal.Status.LIVE).stream()
                .filter(signal -> MembershipAccess.canAccess(
                        user, MembershipAccess.requiredForSignal(signal)))
                .count());
        res.put("fact", s.getFact());
        res.put("promotionTitle", s.getPromotionTitle());
        res.put("promotionBody", s.getPromotionBody());
        return res;
    }

    @GetMapping("/settings/public")
    public Map<String, Object> settings() {
        AppSettings s = wallet.settings();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("adminFeePct", s.getAdminFeePct());
        res.put("withdrawFeePct", s.getWithdrawFeePct());
        res.put("minDeposit", s.getMinDeposit());
        res.put("maxDeposit", s.getMaxDeposit());
        res.put("minWithdraw", s.getMinWithdraw());
        res.put("maxWithdraw", s.getMaxWithdraw());
        res.put("fact", s.getFact());
        res.put("promotionTitle", s.getPromotionTitle());
        res.put("promotionBody", s.getPromotionBody());
        res.put("pointsPerReferral", s.getPointsPerReferral());
        res.put("depositMethods", s.getDepositMethods());
        res.put("withdrawalMethods", s.getWithdrawalMethods());
        res.put("derivVipPrice", s.getDerivVipPrice());
        res.put("weltradeVipPrice", s.getWeltradeVipPrice());
        res.put("vipExpiryDays", s.getVipExpiryDays());
        return res;
    }
}
