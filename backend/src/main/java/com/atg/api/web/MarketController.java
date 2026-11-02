package com.atg.api.web;

import com.atg.api.model.Favourite;
import com.atg.api.model.MarketPair;
import com.atg.api.model.Signal;
import com.atg.api.repo.FavouriteRepository;
import com.atg.api.repo.MarketPairRepository;
import com.atg.api.repo.SignalRepository;
import com.atg.api.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/markets")
public class MarketController extends BaseController {

    private final MarketPairRepository markets;
    private final FavouriteRepository favourites;
    private final SignalRepository signals;

    public MarketController(UserRepository users, MarketPairRepository markets,
                            FavouriteRepository favourites, SignalRepository signals) {
        super(users);
        this.markets = markets;
        this.favourites = favourites;
        this.signals = signals;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("ok", true);
    }

    @GetMapping
    public List<MarketPair> list(@RequestParam(defaultValue = "forex") String category) {
        String c = category.toLowerCase(Locale.ROOT);
        if (c.equals("favourites")) {
            List<String> symbols = favourites.findByUserId(me().getId()).stream().map(Favourite::getSymbol).toList();
            return markets.findAllByOrderBySymbolAsc().stream()
                    .filter(p -> symbols.contains(p.getSymbol()))
                    .toList();
        }
        try {
            MarketPair.AssetClass assetClass = MarketPair.AssetClass.valueOf(c.toUpperCase(Locale.ROOT));
            return markets.findByAssetClassOrderBySymbolAsc(assetClass);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown market category");
        }
    }

    @GetMapping("/{symbol}")
    public Map<String, Object> one(@PathVariable String symbol) {
        MarketPair pair = markets.findBySymbolIgnoreCase(symbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pair not found"));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("pair", pair);
        res.put("calls", signals.findBySymbolOrderByCreatedAtDesc(pair.getSymbol()));
        res.put("favourite", favourites.findByUserIdAndSymbol(me().getId(), pair.getSymbol()).isPresent());
        return res;
    }

    @PostMapping("/{symbol}/favourite")
    public Map<String, Object> toggleFavourite(@PathVariable String symbol) {
        String upper = symbol.toUpperCase(Locale.ROOT);
        var existing = favourites.findByUserIdAndSymbol(me().getId(), upper);
        boolean favourite;
        if (existing.isPresent()) {
            favourites.delete(existing.get());
            favourite = false;
        } else {
            Favourite f = new Favourite();
            f.setUserId(me().getId());
            f.setSymbol(upper);
            favourites.save(f);
            favourite = true;
        }
        return Map.of("favourite", favourite);
    }
}
