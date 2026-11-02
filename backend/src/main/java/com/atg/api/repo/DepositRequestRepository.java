package com.atg.api.repo;

import com.atg.api.model.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    List<DepositRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<DepositRequest> findAllByOrderByCreatedAtDesc();
}
