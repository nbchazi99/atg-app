package com.atg.api.repo;

import com.atg.api.model.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    List<PushToken> findByUserId(Long userId);

    void deleteByToken(String token);
}
