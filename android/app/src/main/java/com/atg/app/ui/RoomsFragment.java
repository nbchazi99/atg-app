package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private LinearLayout roomChoices;
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
        roomChoices = view.findViewById(R.id.roomChoices);

        chipOpen = view.findViewById(R.id.btnFeedOpen);
        chipClosed = view.findViewById(R.id.btnFeedClosed);
        chipAll = view.findViewById(R.id.btnFeedAll);
        chipOpen.setOnClickListener(v -> choose(chipOpen, "open"));
        chipClosed.setOnClickListener(v -> choose(chipClosed, "closed"));
        chipAll.setOnClickListener(v -> choose(chipAll, "latest"));
        view.findViewById(R.id.btnStartThread).setOnClickListener(v -> startThread());

        RecyclerView list = view.findViewById(R.id.rvFeed);
        adapter = new FeedAdapter(item -> {
            long id = item.roomId > 0 ? item.roomId : mainRoomId;
            if (item.createdById == null) {
                openRoom(id, item.roomName == null ? mainRoomName : item.roomName);
            } else {
                RoomDetailActivity.startThread(requireContext(), item);
            }
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

    private void startThread() {
        EditText title = new EditText(requireContext());
        title.setHint("Conversation title");
        title.setSingleLine(true);
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText message = new EditText(requireContext());
        message.setHint("What would you like to discuss?");
        message.setMinLines(3);
        message.setMaxLines(5);
        message.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int sidePadding = dp(24);
        form.setPadding(sidePadding, dp(8), sidePadding, 0);
        form.addView(title);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = dp(12);
        form.addView(message, messageParams);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Start a conversation")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Start chat", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String conversationTitle = title.getText().toString().trim();
                    String firstMessage = message.getText().toString().trim();
                    if (conversationTitle.isEmpty()) {
                        title.setError("Add a conversation title");
                        return;
                    }
                    if (firstMessage.isEmpty()) {
                        message.setError("Add your first message");
                        return;
                    }
                    android.widget.Button startButton =
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                    startButton.setEnabled(false);
                    ApiClient.api().createCommunityThread(
                            new Models.ThreadReq(conversationTitle, firstMessage))
                            .enqueue(new Callback<>() {
                                @Override
                                public void onResponse(@NonNull Call<Models.FeedItem> call,
                                                       @NonNull Response<Models.FeedItem> response) {
                                    if (!isAdded()) return;
                                    if (!response.isSuccessful() || response.body() == null) {
                                        startButton.setEnabled(true);
                                        Ui.toast(requireContext(), ApiClient.errorMessage(response));
                                        return;
                                    }
                                    dialog.dismiss();
                                    filter = "open";
                                    Ui.setChip(chipOpen, true);
                                    Ui.setChip(chipClosed, false);
                                    Ui.setChip(chipAll, false);
                                    load();
                                    RoomDetailActivity.startThread(requireContext(), response.body());
                                }

                                @Override
                                public void onFailure(@NonNull Call<Models.FeedItem> call, @NonNull Throwable t) {
                                    if (!isAdded()) return;
                                    startButton.setEnabled(true);
                                    Ui.toast(requireContext(), ApiClient.connectionMessage(t));
                                }
                            });
                }));
        dialog.show();
    }

    private void load() {
        ApiClient.api().rooms().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.Room>> call, @NonNull Response<List<Models.Room>> response) {
                if (!isAdded() || response.body() == null || response.body().isEmpty()) return;
                roomById.clear();
                roomChoices.removeAllViews();
                for (Models.Room r : response.body()) {
                    roomById.put(r.id, r);
                    TextView choice = new TextView(requireContext());
                    choice.setText(r.name + (r.locked ? " · Locked" : ""));
                    choice.setTextColor(requireContext().getColor(r.locked ? R.color.muted : R.color.ink));
                    choice.setTextSize(13);
                    choice.setPadding(dp(14), dp(9), dp(14), dp(9));
                    choice.setBackgroundResource(r.locked ? R.drawable.bg_chip_grey : R.drawable.bg_card);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMarginEnd(dp(8));
                    choice.setLayoutParams(params);
                    choice.setOnClickListener(v -> openRoom(r.id, r.name));
                    roomChoices.addView(choice);
                }
                Models.Room room = response.body().stream()
                        .filter(r -> "Community".equalsIgnoreCase(r.name))
                        .findFirst()
                        .orElseGet(() -> response.body().stream()
                                .filter(r -> !r.locked)
                                .findFirst()
                                .orElse(response.body().get(0)));
                mainRoomId = room.id;
                mainRoomName = room.name;
                roomName.setText(room.name);
                roomPreview.setText(room.preview == null ? "Tap to open the room" : room.preview);
                roomTime.setText(Ui.timeAgo(room.lastActivityAt));
                roomBadge.setText(room.unreadCount >= 99 ? "99+" : String.valueOf(room.unreadCount));
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.Room>> call, @NonNull Throwable t) {
                if (isAdded()) Ui.toast(requireContext(), ApiClient.connectionMessage(t));
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
