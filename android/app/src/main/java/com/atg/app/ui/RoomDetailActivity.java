package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ROOM_ID = "roomId";
    private static final String EXTRA_ROOM_NAME = "roomName";
    private static final String EXTRA_THREAD_ID = "threadId";
    private static final String EXTRA_THREAD_TITLE = "threadTitle";
    private static final String EXTRA_THREAD_OPEN = "threadOpen";
    private static final String EXTRA_THREAD_AUTHOR_ID = "threadAuthorId";

    private long roomId;
    private long threadId;
    private boolean threadOpen = true;
    private boolean canManageThread;
    private MessagesAdapter adapter;
    private RecyclerView list;
    private EditText input;
    private MaterialButton send;
    private boolean sending;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshMessages = new Runnable() {
        @Override
        public void run() {
            load(true);
            refreshHandler.postDelayed(this, 5000);
        }
    };

    public static void start(Context context, long roomId, String name) {
        Intent intent = new Intent(context, RoomDetailActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        intent.putExtra(EXTRA_ROOM_NAME, name);
        context.startActivity(intent);
    }

    public static void startThread(Context context, Models.FeedItem thread) {
        Intent intent = new Intent(context, RoomDetailActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, thread.roomId);
        intent.putExtra(EXTRA_ROOM_NAME, thread.roomName);
        intent.putExtra(EXTRA_THREAD_ID, thread.id);
        intent.putExtra(EXTRA_THREAD_TITLE, thread.title);
        intent.putExtra(EXTRA_THREAD_OPEN, thread.open);
        if (thread.createdById != null) {
            intent.putExtra(EXTRA_THREAD_AUTHOR_ID, thread.createdById);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        roomId = getIntent().getLongExtra(EXTRA_ROOM_ID, 1);
        if (roomId <= 0) roomId = 1;
        threadId = getIntent().getLongExtra(EXTRA_THREAD_ID, 0);
        threadOpen = getIntent().getBooleanExtra(EXTRA_THREAD_OPEN, true);
        String roomName = getIntent().getStringExtra(EXTRA_ROOM_NAME);
        String title = threadId > 0
                ? getIntent().getStringExtra(EXTRA_THREAD_TITLE)
                : roomName;

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText(title == null ? "Room" : title);
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        TextView roomInfo = findViewById(R.id.tvRoomInfo);
        MaterialButton threadStatus = findViewById(R.id.btnThreadStatus);
        MaterialButton startNewThread = findViewById(R.id.btnStartNewThread);
        if (threadId > 0) {
            long authorId = getIntent().getLongExtra(EXTRA_THREAD_AUTHOR_ID, -1);
            canManageThread = ApiClient.session().isAdmin() || authorId == ApiClient.session().id();
            threadStatus.setVisibility(canManageThread ? View.VISIBLE : View.GONE);
            threadStatus.setOnClickListener(v -> toggleThread());
        } else {
            roomInfo.setText("Everyone here follows the same signals. Be kind.");
            boolean isCommunity = "Community".equalsIgnoreCase(roomName);
            startNewThread.setVisibility(isCommunity ? View.VISIBLE : View.GONE);
            startNewThread.setOnClickListener(v -> showCreateThreadDialog());
        }

        list = findViewById(R.id.rvMessages);
        adapter = new MessagesAdapter(ApiClient.session().name());
        LinearLayoutManager layout = new LinearLayoutManager(this);
        layout.setStackFromEnd(true);
        list.setLayoutManager(layout);
        list.setAdapter(adapter);

        input = findViewById(R.id.etMessage);
        send = findViewById(R.id.btnSend);
        send.setOnClickListener(v -> sendMessage());
        if (threadId > 0) updateThreadState(roomInfo, threadStatus);

        load(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.postDelayed(refreshMessages, 5000);
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshMessages);
        super.onPause();
    }

    private void load(boolean silent) {
        ApiClient.api().messages(roomId, threadId > 0 ? threadId : null).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.RoomMessage>> call,
                                   @NonNull Response<List<Models.RoomMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int oldCount = adapter.getItemCount();
                    LinearLayoutManager layout = (LinearLayoutManager) list.getLayoutManager();
                    boolean wasAtBottom = oldCount == 0
                            || layout.findLastVisibleItemPosition() >= oldCount - 1;
                    adapter.setItems(response.body());
                    if (!response.body().isEmpty()
                            && (oldCount == 0 || (wasAtBottom && response.body().size() > oldCount))) {
                        list.scrollToPosition(response.body().size() - 1);
                    }
                } else if (!silent) {
                    Ui.toast(RoomDetailActivity.this, ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.RoomMessage>> call, @NonNull Throwable t) {
                if (!silent) Ui.toast(RoomDetailActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }

    private void sendMessage() {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty() || sending) return;
        sending = true;
        send.setEnabled(false);
        ApiClient.api().sendMessage(roomId, threadId > 0 ? threadId : null,
                new Models.TextReq(text)).enqueue(new Callback<>() {
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

    private void toggleThread() {
        if (threadId <= 0 || !canManageThread) return;
        MaterialButton status = findViewById(R.id.btnThreadStatus);
        status.setEnabled(false);
        ApiClient.api().setThreadOpen(roomId, threadId, new Models.ThreadStateReq(!threadOpen))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Models.FeedItem> call,
                                           @NonNull Response<Models.FeedItem> response) {
                        status.setEnabled(true);
                        if (!response.isSuccessful() || response.body() == null) {
                            Ui.toast(RoomDetailActivity.this, ApiClient.errorMessage(response));
                            return;
                        }
                        threadOpen = response.body().open;
                        updateThreadState(findViewById(R.id.tvRoomInfo), status);
                    }

                    @Override
                    public void onFailure(@NonNull Call<Models.FeedItem> call, @NonNull Throwable t) {
                        status.setEnabled(true);
                        Ui.toast(RoomDetailActivity.this, ApiClient.connectionMessage(t));
                    }
                });
    }

    private void updateThreadState(TextView roomInfo, MaterialButton status) {
        roomInfo.setText(threadOpen
                ? "Open conversation · Members can send and receive messages."
                : "Closed conversation · Members can still read messages.");
        input.setEnabled(threadOpen);
        send.setEnabled(threadOpen);
        status.setText(threadOpen ? "Close conversation" : "Reopen conversation");
    }

    private void showCreateThreadDialog() {
        EditText title = new EditText(this);
        title.setHint("Conversation title");
        title.setSingleLine(true);
        title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText message = new EditText(this);
        message.setHint("What would you like to discuss?");
        message.setMinLines(3);
        message.setMaxLines(5);
        message.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
        message.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int sidePadding = (int) (24 * getResources().getDisplayMetrics().density + 0.5f);
        form.setPadding(sidePadding, sidePadding / 3, sidePadding, 0);
        form.addView(title);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = sidePadding / 2;
        form.addView(message, messageParams);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Start a conversation")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Start chat", null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
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
                                            if (!response.isSuccessful() || response.body() == null) {
                                                startButton.setEnabled(true);
                                                Ui.toast(RoomDetailActivity.this, ApiClient.errorMessage(response));
                                                return;
                                            }
                                            dialog.dismiss();
                                            startThread(RoomDetailActivity.this, response.body());
                                        }

                                        @Override
                                        public void onFailure(@NonNull Call<Models.FeedItem> call,
                                                              @NonNull Throwable t) {
                                            startButton.setEnabled(true);
                                            Ui.toast(RoomDetailActivity.this, ApiClient.connectionMessage(t));
                                        }
                                    });
                        }));
        dialog.show();
    }
}
