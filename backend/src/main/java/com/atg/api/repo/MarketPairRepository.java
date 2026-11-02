package com.atg.api.repo;

import com.atg.api.model.MarketPair;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MarketPairRepository extends JpaRepository<MarketPair, Long> {
    List<MarketPair> findByAssetClassOrderBySymbolAsc(MarketPair.AssetClass assetClass);
    Optional<MarketPair> findBySymbolIgnoreCase(String symbol);
    List<MarketPair> findAllByOrderBySymbolAsc();
}
