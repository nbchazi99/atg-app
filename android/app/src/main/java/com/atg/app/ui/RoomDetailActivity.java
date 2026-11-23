package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

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
    private static final int REQUEST_ATTACHMENT = 4101;
    private static final int REQUEST_RECORD_AUDIO = 4102;
    private static final long MAX_ATTACHMENT_BYTES = 80L * 1024 * 1024;

    private long roomId;
    private long threadId;
    private boolean threadOpen = true;
    private boolean canManageThread;
    private MessagesAdapter adapter;
    private RecyclerView list;
    private EditText input;
    private MaterialButton send;
    private MaterialButton attach;
    private MaterialButton voice;
    private boolean sending;
    private boolean uploadingAttachment;
    private boolean recording;
    private MediaRecorder recorder;
    private File recordingFile;
    private MediaPlayer audioPlayer;
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
        if (threadId > 0) {
            long authorId = getIntent().getLongExtra(EXTRA_THREAD_AUTHOR_ID, -1);
            canManageThread = ApiClient.session().isAdmin() || authorId == ApiClient.session().id();
            threadStatus.setVisibility(canManageThread ? View.VISIBLE : View.GONE);
            threadStatus.setOnClickListener(v -> toggleThread());
        } else {
            roomInfo.setText("Everyone here follows the same signals. Be kind.");
        }

        list = findViewById(R.id.rvMessages);
        adapter = new MessagesAdapter(ApiClient.session().name(), this::openAttachment);
        LinearLayoutManager layout = new LinearLayoutManager(this);
        layout.setStackFromEnd(true);
        list.setLayoutManager(layout);
        list.setAdapter(adapter);

        input = findViewById(R.id.etMessage);
        send = findViewById(R.id.btnSend);
        attach = findViewById(R.id.btnAttach);
        voice = findViewById(R.id.btnVoice);
        send.setOnClickListener(v -> sendMessage());
        attach.setOnClickListener(v -> pickAttachment());
        voice.setOnClickListener(v -> toggleVoiceRecording());
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
        releaseAudioPlayer();
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
        sendMessage(null, null);
    }

    private void sendMessage(String attachmentUrl) {
        sendMessage(attachmentUrl, null);
    }

    private void sendMessage(String attachmentUrl, String attachmentType) {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if ((text.isEmpty() && attachmentUrl == null) || sending) return;
        sending = true;
        send.setEnabled(false);
        ApiClient.api().sendMessage(roomId, threadId > 0 ? threadId : null,
                new Models.TextReq(text, attachmentUrl, attachmentType)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.RoomMessage> call,
                                   @NonNull Response<Models.RoomMessage> response) {
                sending = false;
                updateComposerControls();
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
                updateComposerControls();
                Ui.toast(RoomDetailActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }

    private void pickAttachment() {
        if (uploadingAttachment || recording || !threadOpen) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "image/*", "video/*", "audio/*", "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        });
        startActivityForResult(intent, REQUEST_ATTACHMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ATTACHMENT && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            uploadAttachment(data.getData(), null);
        }
    }

    private void uploadAttachment(Uri uri, String attachmentType) {
        uploadingAttachment = true;
        updateComposerControls();
        ApiClient.uploadRoomAttachment(this, roomId, uri, new ApiClient.UploadCallback() {
            @Override
            public void ok(String url) {
                uploadingAttachment = false;
                updateComposerControls();
                if (recordingFile != null) recordingFile.delete();
                recordingFile = null;
                sendMessage(url, attachmentType);
            }

            @Override
            public void fail(String message) {
                uploadingAttachment = false;
                updateComposerControls();
                if (recordingFile != null) recordingFile.delete();
                recordingFile = null;
                Ui.toast(RoomDetailActivity.this, message);
            }
        });
    }

    private void toggleVoiceRecording() {
        if (uploadingAttachment || sending || !threadOpen) return;
        if (recording) {
            stopVoiceRecording();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        startVoiceRecording();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording();
            } else {
                Ui.toast(this, "Microphone permission is required to record voice notes");
            }
        }
    }

    private void startVoiceRecording() {
        recordingFile = new File(getCacheDir(),
                "voice-" + System.currentTimeMillis() + ".m4a");
        try {
            recorder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? new MediaRecorder(this) : new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(recordingFile.getAbsolutePath());
            recorder.setMaxFileSize(MAX_ATTACHMENT_BYTES);
            recorder.setOnInfoListener((mediaRecorder, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    stopVoiceRecording();
                }
            });
            recorder.prepare();
            recorder.start();
            recording = true;
            voice.setText("Stop");
            attach.setEnabled(false);
            send.setEnabled(false);
        } catch (IOException | RuntimeException e) {
            releaseRecorder();
            recordingFile.delete();
            recordingFile = null;
            Ui.toast(this, "Could not start recording");
        }
    }

    private void stopVoiceRecording() {
        boolean saved = false;
        try {
            if (recorder != null) {
                recorder.stop();
                saved = true;
            }
        } catch (RuntimeException e) {
            Ui.toast(this, "The voice note was too short to save");
        } finally {
            releaseRecorder();
            recording = false;
            updateComposerControls();
        }
        if (saved && recordingFile != null && recordingFile.length() > 0) {
            uploadAttachment(Uri.fromFile(recordingFile), "AUDIO");
        } else if (recordingFile != null) {
            recordingFile.delete();
            recordingFile = null;
        }
    }

    private void updateComposerControls() {
        boolean enabled = threadOpen && !sending && !uploadingAttachment;
        input.setEnabled(enabled);
        send.setEnabled(enabled && !recording);
        attach.setEnabled(enabled && !recording);
        voice.setEnabled(enabled);
        voice.setText(recording ? "Stop" : "Voice");
    }

    private void openAttachment(Models.RoomMessage message) {
        String url = message.attachmentUrl;
        if (url == null || url.isBlank()) return;
        if ("AUDIO".equalsIgnoreCase(message.attachmentType) || isAudio(url)) {
            playAudio(url);
            return;
        }
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(url)
                .toLowerCase(Locale.ROOT);
        String mime = android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension);
        if (mime == null) mime = "*/*";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mime);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Ui.toast(this, "No app is available to open this attachment");
        }
    }

    private boolean isAudio(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".wav")
                || lower.endsWith(".m4a") || lower.endsWith(".aac")
                || lower.endsWith(".ogg") || lower.endsWith(".opus")
                || lower.endsWith(".flac");
    }

    private void playAudio(String url) {
        releaseAudioPlayer();
        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setDataSource(url);
            audioPlayer.setOnPreparedListener(MediaPlayer::start);
            audioPlayer.setOnCompletionListener(player -> releaseAudioPlayer());
            audioPlayer.setOnErrorListener((player, what, extra) -> {
                releaseAudioPlayer();
                Ui.toast(this, "Could not play this audio file");
                return true;
            });
            audioPlayer.prepareAsync();
        } catch (IOException | RuntimeException e) {
            releaseAudioPlayer();
            Ui.toast(this, "Could not play this audio file");
        }
    }

    private void releaseAudioPlayer() {
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
            } catch (RuntimeException ignored) {
            }
            recorder.release();
            recorder = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (recording) {
            try {
                if (recorder != null) recorder.stop();
            } catch (RuntimeException ignored) {
            }
            recording = false;
        }
        if (recordingFile != null) {
            recordingFile.delete();
            recordingFile = null;
        }
        releaseRecorder();
        releaseAudioPlayer();
        super.onDestroy();
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
        updateComposerControls();
        status.setText(threadOpen ? "Close conversation" : "Reopen conversation");
    }

}
