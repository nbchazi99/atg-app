package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomsFragment extends Fragment {

    private FeedAdapter adapter;
    private SwipeRefreshLayout swipe;
    private View empty;
    private TextView roomName, roomPreview, roomTime, roomBadge;
    private MaterialButton chipOpen, chipClosed, chipAll;
    private String filter = "open";
    private long mainRoomId = -1;
    private String mainRoomName = "ATG Trading Floor";
    private final Map<Long, Models.Room> roomById = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rooms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView title = view.findViewById(R.id.tvAppTitle);
        title.setText("Rooms");
        TextView avatar = view.findViewById(R.id.tvAvatar);
        avatar.setText(ApiClient.session().initials());
        avatar.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProfileActivity.class)));
        view.findViewById(R.id.btnBell).setOnClickListener(v ->
                Ui.alert(requireContext(), "Notifications", "You are all caught up."));

        roomName = view.findViewById(R.id.tvMainRoomName);
        roomPreview = view.findViewById(R.id.tvMainRoomPreview);
        roomTime = view.findViewById(R.id.tvMainRoomTime);
        roomBadge = view.findViewById(R.id.tvMainRoomBadge);

        chipOpen = view.findViewById(R.id.btnFeedOpen);
        chipClosed = view.findViewById(R.id.btnFeedClosed);
        chipAll = view.findViewById(R.id.btnFeedAll);
        chipOpen.setOnClickListener(v -> choose(chipOpen, "open"));
        chipClosed.setOnClickListener(v -> choose(chipClosed, "closed"));
        chipAll.setOnClickListener(v -> choose(chipAll, "all"));

        RecyclerView list = view.findViewById(R.id.rvFeed);
        adapter = new FeedAdapter(item -> {
            long id = item.roomId > 0 ? item.roomId : mainRoomId;
            openRoom(id, item.roomName == null ? mainRoomName : item.roomName);
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        view.findViewById(R.id.cardMainRoom).setOnClickListener(v -> openRoom(mainRoomId, mainRoomName));

        empty = view.findViewById(R.id.roomsEmpty);
        swipe = view.findViewById(R.id.swipeRooms);
        swipe.setColorSchemeResources(R.color.primary);
        swipe.setOnRefreshListener(this::load);

        choose(chipOpen, "open");
    }

    private void openRoom(long id, String name) {
        if (id <= 0) return;
        Models.Room room = roomById.get(id);
        if (room != null && room.locked) {
            String plan = Ui.membershipLabel(room.requiredMembership);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(room.name)
                    .setMessage(plan + " members only. Buy the membership from your profile, attach your payment proof, and the administrator unlocks the room after reviewing it.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("View memberships", (d, w) ->
                            startActivity(new Intent(requireContext(), ProfileActivity.class)))
                    .show();
            return;
        }
        RoomDetailActivity.start(requireContext(), id, name);
    }

    private void choose(MaterialButton chip, String value) {
        filter = value;
        Ui.setChip(chipOpen, chip == chipOpen);
        Ui.setChip(chipClosed, chip == chipClosed);
        Ui.setChip(chipAll, chip == chipAll);
        load();
    }

    private void load() {
        ApiClient.api().rooms().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.Room>> call, @NonNull Response<List<Models.Room>> response) {
                if (!isAdded() || response.body() == null || response.body().isEmpty()) return;
                roomById.clear();
                for (Models.Room r : response.body()) roomById.put(r.id, r);
                Models.Room room = response.body().get(0);
                mainRoomId = room.id;
                mainRoomName = room.name;
                roomName.setText(room.name);
                roomPreview.setText(room.preview == null ? "Tap to open the room" : room.preview);
                roomTime.setText(Ui.timeAgo(room.lastActivityAt));
                roomBadge.setText(room.unreadCount >= 99 ? "99+" : String.valueOf(room.unreadCount));
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.Room>> call, @NonNull Throwable t) {
            }
        });

        String requested = filter;
        ApiClient.api().feed(requested).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.FeedItem>> call,
                                   @NonNull Response<List<Models.FeedItem>> response) {
                if (!isAdded() || !requested.equals(filter)) return;
                swipe.setRefreshing(false);
                List<Models.FeedItem> items = response.body();
                if (!response.isSuccessful() || items == null) {
                    Ui.toast(requireContext(), ApiClient.errorMessage(response));
                    return;
                }
                adapter.setItems(items);
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.FeedItem>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Ui.toast(requireContext(), ApiClient.connectionMessage(t));
            }
        });
    }
}
