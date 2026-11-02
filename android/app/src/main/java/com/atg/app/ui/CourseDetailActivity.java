package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseDetailActivity extends AppCompatActivity {

    private static final String EXTRA_ID = "courseId";

    private long courseId;
    private Models.Course course;
    private boolean enrolled;
    private LessonsAdapter adapter;
    private TextView name, subtitle, level, lessons, price, desc, progressLabel, lockedNote;
    private ProgressBar progress;
    private MaterialButton buy;
    private boolean ordering;

    private final ActivityResultLauncher<String> proofPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onProofPicked);

    public static void start(Context context, long courseId) {
        Intent intent = new Intent(context, CourseDetailActivity.class);
        intent.putExtra(EXTRA_ID, courseId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        courseId = getIntent().getLongExtra(EXTRA_ID, 0);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Course");
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        name = findViewById(R.id.tvCourseName);
        subtitle = findViewById(R.id.tvCourseSubtitle);
        level = findViewById(R.id.tvCourseLevel);
        lessons = findViewById(R.id.tvCourseLessons);
        price = findViewById(R.id.tvCoursePrice);
        desc = findViewById(R.id.tvCourseDesc);
        progressLabel = findViewById(R.id.tvProgressLabel);
        progress = findViewById(R.id.pbCourseDetail);
        lockedNote = findViewById(R.id.tvLockedNote);
        buy = findViewById(R.id.btnBuyCourse);

        RecyclerView list = findViewById(R.id.rvLessons);
        adapter = new LessonsAdapter((lesson, completed) -> {
            if (!enrolled) {
                Ui.toast(this, "Buy this course to unlock the lessons");
                return;
            }
            LessonActivity.start(this, courseId, lesson, completed);
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        buy.setOnClickListener(v -> beginPurchase());
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (course != null) load();
    }

    private void load() {
        ApiClient.api().course(courseId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.CourseDetail> call,
                                   @NonNull Response<Models.CourseDetail> response) {
                Models.CourseDetail detail = response.body();
                if (!response.isSuccessful() || detail == null || detail.course == null) {
                    Ui.toast(CourseDetailActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                course = detail.course;
                enrolled = detail.enrolled;
                name.setText(course.title);
                subtitle.setText(course.subtitle);
                level.setText(course.level);
                lessons.setText(course.lessonsCount + " lessons");
                price.setText(Ui.money(course.price));
                desc.setText(course.description);

                adapter.setItems(detail.lessons, detail.completedLessonIds);
                int total = course.lessonsCount;
                int done = detail.completedLessonIds == null ? 0 : detail.completedLessonIds.size();
                int pct = total == 0 ? 0 : Math.round(done * 100f / total);
                progress.setProgress(pct);
                if (enrolled) {
                    progressLabel.setText(done + " of " + total + " lessons complete");
                    buy.setVisibility(View.GONE);
                    lockedNote.setVisibility(View.GONE);
                } else {
                    progress.setVisibility(View.GONE);
                    progressLabel.setText("Not enrolled");
                    buy.setVisibility(View.VISIBLE);
                    buy.setText("Buy this course · " + Ui.money(course.price));
                    lockedNote.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.CourseDetail> call, @NonNull Throwable t) {
                Ui.alert(CourseDetailActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void beginPurchase() {
        if (course == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Buy " + course.title)
                .setMessage("Pay " + Ui.money(course.price)
                        + " to the ATG desk, then attach a screenshot of the payment. The administrator unlocks the course after reviewing it.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose screenshot", (d, w) -> proofPicker.launch("image/*"))
                .show();
    }

    private void onProofPicked(Uri uri) {
        if (uri == null || ordering) return;
        ordering = true;
        buy.setEnabled(false);
        buy.setText("Uploading proof…");
        ApiClient.upload(this, uri, new ApiClient.UploadCallback() {
            @Override
            public void ok(String url) {
                buy.setText("Filing purchase…");
                ApiClient.api().purchase(new Models.PurchaseReq("COURSE", courseId, url)).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Models.PurchaseRes> call,
                                           @NonNull Response<Models.PurchaseRes> response) {
                        ordering = false;
                        resetButton();
                        if (response.isSuccessful() && response.body() != null) {
                            Ui.alert(CourseDetailActivity.this, "Purchase filed",
                                    "Reference P-" + response.body().id
                                            + ". The administrator reviews proofs manually; once approved the lessons unlock here.");
                        } else {
                            Ui.alert(CourseDetailActivity.this, "Could not file the purchase", ApiClient.errorMessage(response));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Models.PurchaseRes> call, @NonNull Throwable t) {
                        ordering = false;
                        resetButton();
                        Ui.alert(CourseDetailActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
                    }
                });
            }

            @Override
            public void fail(String message) {
                ordering = false;
                resetButton();
                Ui.alert(CourseDetailActivity.this, "Could not upload", message);
            }
        });
    }

    private void resetButton() {
        buy.setEnabled(true);
        buy.setText(course == null ? "Buy course" : "Buy this course · " + Ui.money(course.price));
    }
}
