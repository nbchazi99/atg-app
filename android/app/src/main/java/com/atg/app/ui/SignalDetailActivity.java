package com.atg.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.atg.app.R;
import com.atg.app.data.Models;

public class SignalDetailActivity extends AppCompatActivity {

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

        findViewById(R.id.btnOpenChat).setOnClickListener(v ->
                RoomDetailActivity.start(this, 1, value(in, "room", "ATG Trading Floor")));
    }

    private static String value(Intent intent, String key, String fallback) {
        String raw = intent.getStringExtra(key);
        return raw == null || raw.isBlank() ? fallback : raw;
    }
}
