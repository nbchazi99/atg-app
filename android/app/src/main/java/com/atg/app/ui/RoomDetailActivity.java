package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ROOM_ID = "roomId";
    private static final String EXTRA_ROOM_NAME = "roomName";

    private long roomId;
    private MessagesAdapter adapter;
    private RecyclerView list;
    private EditText input;
    private MaterialButton send;
    private boolean sending;

    public static void start(Context context, long roomId, String name) {
        Intent intent = new Intent(context, RoomDetailActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_ROOM_NAME, name);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        roomId = getIntent().getLongExtra(EXTRA_ROOM_ID, 1);
        if (roomId <= 0) roomId = 1;
        String name = getIntent().getStringExtra(EXTRA_ROOM_NAME);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText(name == null ? "Room" : name);
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        list = findViewById(R.id.rvMessages);
        adapter = new MessagesAdapter(ApiClient.session().name());
        LinearLayoutManager layout = new LinearLayoutManager(this);
        layout.setStackFromEnd(true);
        list.setLayoutManager(layout);
        list.setAdapter(adapter);

        input = findViewById(R.id.etMessage);
        send = findViewById(R.id.btnSend);
        send.setOnClickListener(v -> sendMessage());

        load();
    }

    private void load() {
        ApiClient.api().messages(roomId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.RoomMessage>> call,
                                   @NonNull Response<List<Models.RoomMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                    if (!response.body().isEmpty()) list.scrollToPosition(response.body().size() - 1);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.RoomMessage>> call, @NonNull Throwable t) {
                Ui.toast(RoomDetailActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }

    private void sendMessage() {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty() || sending) return;
        sending = true;
        send.setEnabled(false);
        ApiClient.api().sendMessage(roomId, new Models.TextReq(text)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.RoomMessage> call,
                                   @NonNull Response<Models.RoomMessage> response) {
                sending = false;
                send.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    input.setText("");
                    adapter.append(response.body());
                    list.scrollToPosition(adapter.getItemCount() - 1);
                } else {
                    Ui.toast(RoomDetailActivity.this, ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.RoomMessage> call, @NonNull Throwable t) {
                sending = false;
                send.setEnabled(true);
                Ui.toast(RoomDetailActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }
}
