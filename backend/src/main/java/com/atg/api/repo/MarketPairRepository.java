package com.atg.api.repo;

import com.atg.api.model.MarketPair;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class MarketPairRepository extends AbstractFirestoreStore<MarketPair> {

    public MarketPairRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "market_pairs", MarketPair.class);
    }

    public List<MarketPair> findByAssetClassOrderBySymbolAsc(MarketPair.AssetClass assetClass) {
        List<MarketPair> pairs = queryWhere("assetClass", assetClass.name());
        pairs.sort(Comparator.comparing(MarketPair::getSymbol));
        return pairs;
    }

    public Optional<MarketPair> findBySymbolIgnoreCase(String symbol) {
        if (symbol == null) return Optional.empty();
        List<MarketPair> matches = queryWhere("symbol", symbol.toUpperCase());
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public List<MarketPair> findAllByOrderBySymbolAsc() {
        List<MarketPair> pairs = findAll();
        pairs.sort(Comparator.comparing(MarketPair::getSymbol));
        return pairs;
    }
}
