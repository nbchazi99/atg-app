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
        return Map.of(
                "adminFeePct", s.getAdminFeePct(),
                "withdrawFeePct", s.getWithdrawFeePct(),
                "minDeposit", s.getMinDeposit(),
                "maxDeposit", s.getMaxDeposit(),
                "minWithdraw", s.getMinWithdraw(),
                "fact", s.getFact(),
                "promotionTitle", s.getPromotionTitle(),
                "promotionBody", s.getPromotionBody(),
                "pointsPerReferral", s.getPointsPerReferral()
        );
    }
}
