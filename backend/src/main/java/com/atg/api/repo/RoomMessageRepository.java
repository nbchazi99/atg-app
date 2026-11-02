package com.atg.api.repo;

import com.atg.api.model.RoomMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomMessageRepository extends JpaRepository<RoomMessage, Long> {
    List<RoomMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);
}
