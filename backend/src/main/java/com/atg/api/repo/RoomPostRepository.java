package com.atg.api.repo;

import com.atg.api.model.RoomPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomPostRepository extends JpaRepository<RoomPost, Long> {
    List<RoomPost> findAllByOrderByCreatedAtDesc();
    List<RoomPost> findByOpenOrderByCreatedAtDesc(boolean open);
}
