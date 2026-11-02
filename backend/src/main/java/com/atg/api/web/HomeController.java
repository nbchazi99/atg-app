package com.atg.api.web;

import com.atg.api.model.AppSettings;
import com.atg.api.model.Signal;
import com.atg.api.model.User;
import com.atg.api.repo.MarketPairRepository;
import com.atg.api.repo.SignalRepository;
import com.atg.api.repo.UserRepository;
import com.atg.api.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HomeController extends BaseController {

    private final MarketPairRepository markets;
    private final SignalRepository signals;
    private final WalletService wallet;

    public HomeController(UserRepository users, MarketPairRepository markets,
                          SignalRepository signals, WalletService wallet) {
        super(users);
        this.markets = markets;
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
        res.put("liveCount", signals.countByStatus(Signal.Status.LIVE));
        res.put("fact", s.getFact());
        res.put("promotionTitle", s.getPromotionTitle());
        res.put("promotionBody", s.getPromotionBody());
        List<String> tickerSymbols = List.of("XAUUSD", "EURUSD", "GBPUSD", "USDJPY");
        res.put("tickers", tickerSymbols.stream()
                .map(symbol -> markets.findBySymbolIgnoreCase(symbol).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList());
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
