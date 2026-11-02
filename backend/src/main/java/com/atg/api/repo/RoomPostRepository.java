package com.atg.api.repo;

import com.atg.api.model.RoomPost;
import com.atg.api.store.AbstractFirestoreStore;
import com.atg.api.store.IdGenerator;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class RoomPostRepository extends AbstractFirestoreStore<RoomPost> {

    public RoomPostRepository(Firestore db, IdGenerator ids) {
        super(db, ids, "room_posts", RoomPost.class);
    }

    public List<RoomPost> findAllByOrderByCreatedAtDesc() {
        List<RoomPost> posts = findAll();
        posts.sort(Comparator.comparing(RoomPost::getCreatedAt).reversed());
        return posts;
    }

    public List<RoomPost> findByOpenOrderByCreatedAtDesc(boolean open) {
        List<RoomPost> posts = queryWhere("open", open);
        posts.sort(Comparator.comparing(RoomPost::getCreatedAt).reversed());
        return posts;
    }
}
