package com.atg.api.repo;

import com.atg.api.model.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();
}
