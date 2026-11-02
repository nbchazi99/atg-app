package com.atg.app.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LessonActivity extends AppCompatActivity {

    private long courseId;
    private long lessonId;
    private boolean completed;
    private MaterialButton complete;
    private boolean saving;

    private final ActivityResultLauncher<String> saveNotes =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"), this::writeNotes);

    public static void start(Context context, long courseId, Models.Lesson lesson, boolean completed) {
        Intent intent = new Intent(context, LessonActivity.class);
        intent.putExtra("courseId", courseId);
        intent.putExtra("lessonId", lesson.id);
        intent.putExtra("title", lesson.title);
        intent.putExtra("content", lesson.content);
        intent.putExtra("video", lesson.videoUrl);
        intent.putExtra("duration", lesson.durationMin);
        intent.putExtra("order", lesson.orderIndex);
        intent.putExtra("completed", completed);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);

        courseId = getIntent().getLongExtra("courseId", 0);
        lessonId = getIntent().getLongExtra("lessonId", 0);
        completed = getIntent().getBooleanExtra("completed", false);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Lesson");
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        ((TextView) findViewById(R.id.tvLessonTitle)).setText(getIntent().getStringExtra("title"));
        ((TextView) findViewById(R.id.tvLessonMeta)).setText(
                getIntent().getIntExtra("duration", 10) + " min · Lesson "
                        + (getIntent().getIntExtra("order", 0) + 1));
        ((TextView) findViewById(R.id.tvLessonContent)).setText(getIntent().getStringExtra("content"));

        complete = findViewById(R.id.btnComplete);
        MaterialButton next = findViewById(R.id.btnNext);
        renderComplete();

        complete.setOnClickListener(v -> markComplete());
        next.setOnClickListener(v -> finish());
        findViewById(R.id.btnDownload).setOnClickListener(v -> saveNotes.launch(notesFileName()));

        String video = getIntent().getStringExtra("video");
        MaterialButton watch = findViewById(R.id.btnVideo);
        if (video != null && !video.isBlank()) {
            watch.setVisibility(View.VISIBLE);
            watch.setOnClickListener(v -> openVideo(video));
        }
    }

    private void openVideo(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Ui.toast(this, "No app can open that video link");
        }
    }

    private String notesFileName() {
        String title = getIntent().getStringExtra("title");
        String base = title == null ? "lesson" : title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = "lesson";
        if (base.length() > 40) base = base.substring(0, 40);
        return base + "-notes.txt";
    }

    private void writeNotes(Uri uri) {
        if (uri == null) return;
        String title = getIntent().getStringExtra("title");
        int duration = getIntent().getIntExtra("duration", 10);
        int order = getIntent().getIntExtra("order", 0) + 1;
        String content = getIntent().getStringExtra("content");
        String body = (title == null ? "Lesson" : title)
                + "\n" + duration + " min · Lesson " + order
                + "\n\n" + (content == null ? "" : content)
                + "\n\n— ATG · Accurate Trading Group\n";

        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                Ui.toast(this, "Could not save the notes");
                return;
            }
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.flush();
            Ui.toast(this, "Notes saved");
        } catch (IOException e) {
            Ui.toast(this, "Could not save the notes");
        }
    }

    private void renderComplete() {
        complete.setEnabled(!completed);
        complete.setText(completed ? "Completed" : "Mark as complete");
        complete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(completed ? R.color.primary_soft : R.color.primary)));
        complete.setTextColor(getColor(completed ? R.color.gold : R.color.ink));
    }

    private void markComplete() {
        if (completed || saving) return;
        saving = true;
        complete.setEnabled(false);
        complete.setText("Saving…");
        ApiClient.api().completeLesson(courseId, new Models.CompleteReq(lessonId)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                saving = false;
                if (response.isSuccessful()) {
                    completed = true;
                    renderComplete();
                    Ui.toast(LessonActivity.this, "Lesson complete");
                } else {
                    renderComplete();
                    Ui.toast(LessonActivity.this, ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                saving = false;
                renderComplete();
                Ui.toast(LessonActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
