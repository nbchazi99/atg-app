package com.atg.api.repo;

import com.atg.api.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    List<PurchaseRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();
}
