package com.atg.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignalDetailActivity extends AppCompatActivity {

    private boolean openingChat;

    public static void start(Context context, Models.Signal signal) {
        Intent intent = new Intent(context, SignalDetailActivity.class);
        intent.putExtra("title", signal.title);
        intent.putExtra("subtitle", signal.subtitle);
        intent.putExtra("side", signal.side);
        intent.putExtra("status", signal.status);
        intent.putExtra("room", signal.roomLabel);
        intent.putExtra("entry", signal.entry);
        intent.putExtra("stop", signal.stopLoss);
        intent.putExtra("targets", signal.targets);
        intent.putExtra("rule", signal.ruleName);
        intent.putExtra("participants", signal.participants);
        intent.putExtra("createdAt", signal.createdAt);
        intent.putExtra("symbol", signal.symbol);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signal_detail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Signal");
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        Intent in = getIntent();
        String side = value(in, "side", "BUY");
        String status = value(in, "status", "LIVE");
        String title = value(in, "title", "Signal");

        ((TextView) findViewById(R.id.tvDetailTitle)).setText(title);
        ((TextView) findViewById(R.id.tvDetailSubtitle)).setText(value(in, "subtitle", ""));
        ((TextView) findViewById(R.id.tvDetailRoom)).setText(value(in, "room", "OPEN FLOOR"));
        ((TextView) findViewById(R.id.tvDetailEntry)).setText(value(in, "entry", "At market, now"));
        ((TextView) findViewById(R.id.tvDetailStop)).setText(value(in, "stop", "Not given"));
        ((TextView) findViewById(R.id.tvDetailTargets)).setText(value(in, "targets", "Not given"));
        ((TextView) findViewById(R.id.tvDetailRule)).setText(value(in, "rule", "M10 rule"));
        ((TextView) findViewById(R.id.tvDetailTime)).setText(Ui.timeAgo(in.getStringExtra("createdAt")));

        int participants = in.getIntExtra("participants", 0);
        ((TextView) findViewById(R.id.tvDetailParticipants)).setText(
                participants == 1 ? "1 trader" : participants + " traders");

        TextView sideChip = findViewById(R.id.tvDetailSide);
        boolean buy = "BUY".equals(side);
        sideChip.setText(side);
        sideChip.setBackgroundResource(buy ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);
        sideChip.setTextColor(ContextCompat.getColor(this, buy ? R.color.green_text : R.color.red_text));

        TextView statusChip = findViewById(R.id.tvDetailStatus);
        statusChip.setText(status);
        if ("LIVE".equals(status)) {
            statusChip.setBackgroundResource(R.drawable.bg_chip_green);
            statusChip.setTextColor(ContextCompat.getColor(this, R.color.green_text));
        } else if ("CLOSED".equals(status)) {
            statusChip.setBackgroundResource(R.drawable.bg_chip_grey);
            statusChip.setTextColor(ContextCompat.getColor(this, R.color.muted));
        } else {
            statusChip.setBackgroundResource(R.drawable.bg_chip_amber);
            statusChip.setTextColor(ContextCompat.getColor(this, R.color.gold));
        }

        TextView notes = findViewById(R.id.tvDetailNotes);
        if (buy) {
            notes.setText("Wait for the candle to close in your favour before entering. Risk no more than 1% of your account. Move the stop to break-even when the first target prints, and do not add to a losing position.");
        } else {
            notes.setText("Sell only after price rejects the level on the close. Risk no more than 1% of your account, trail the stop behind structure, and take the call off the table if the rule invalidates.");
        }

        View openChat = findViewById(R.id.btnOpenChat);
        openChat.setOnClickListener(v -> openRoomChat(openChat, value(in, "room", "OPEN FLOOR")));
    }

    private void openRoomChat(View button, String roomLabel) {
        if (openingChat) return;
        openingChat = true;
        button.setEnabled(false);
        ApiClient.api().rooms().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.Room>> call,
                                   @NonNull Response<List<Models.Room>> response) {
                openingChat = false;
                button.setEnabled(true);
                if (isFinishing() || isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    Ui.toast(SignalDetailActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                Models.Room room = findRoom(response.body(), roomLabel);
                if (room == null) {
                    Ui.toast(SignalDetailActivity.this, "Community chat is not configured. Try again shortly.");
                    return;
                }
                if (room.locked) {
                    String plan = Ui.membershipLabel(room.requiredMembership);
                    new MaterialAlertDialogBuilder(SignalDetailActivity.this)
                            .setTitle(room.name)
                            .setMessage(plan + " members only. Buy the membership from your profile, attach your payment proof, and the administrator unlocks the room after reviewing it.")
                            .setNegativeButton("Not now", null)
                            .setPositiveButton("View memberships", (dialog, which) ->
                                    startActivity(new Intent(SignalDetailActivity.this, ProfileActivity.class)))
                            .show();
                    return;
                }
                RoomDetailActivity.start(SignalDetailActivity.this, room.id, room.name);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.Room>> call, @NonNull Throwable t) {
                openingChat = false;
                button.setEnabled(true);
                if (!isFinishing() && !isDestroyed()) {
                    Ui.toast(SignalDetailActivity.this, ApiClient.connectionMessage(t));
                }
            }
        });
    }

    private static Models.Room findRoom(List<Models.Room> rooms, String roomLabel) {
        String label = roomLabel == null ? "" : roomLabel.trim();
        if (!label.isEmpty() && !label.equalsIgnoreCase("OPEN FLOOR")) {
            for (Models.Room room : rooms) {
                if (room.name != null
                        && room.name.toLowerCase(Locale.ROOT).contains(label.toLowerCase(Locale.ROOT))) {
                    return room;
                }
            }
        }
        for (Models.Room room : rooms) {
            if ("Community".equalsIgnoreCase(room.name)) return room;
        }
        for (Models.Room room : rooms) {
            if (!room.locked) return room;
        }
        return rooms.isEmpty() ? null : rooms.get(0);
    }

    private static String value(Intent intent, String key, String fallback) {
        String raw = intent.getStringExtra(key);
        return raw == null || raw.isBlank() ? fallback : raw;
    }
}
