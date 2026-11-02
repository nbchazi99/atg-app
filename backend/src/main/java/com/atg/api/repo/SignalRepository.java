package com.atg.api.repo;

import com.atg.api.model.Signal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SignalRepository extends JpaRepository<Signal, Long> {
    List<Signal> findByStatusOrderByCreatedAtDesc(Signal.Status status);
    List<Signal> findBySymbolOrderByCreatedAtDesc(String symbol);
    long countByStatus(Signal.Status status);
    List<Signal> findAllByOrderByCreatedAtDesc();
}
