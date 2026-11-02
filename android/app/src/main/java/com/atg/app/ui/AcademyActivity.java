package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AcademyActivity extends AppCompatActivity {

    private CoursesAdapter adapter;
    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academy);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Academy");
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        RecyclerView list = findViewById(R.id.rvCourses);
        adapter = new CoursesAdapter(row -> CourseDetailActivity.start(this, row.course.id));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        empty = findViewById(R.id.coursesEmpty);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.api().courses().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.CourseRow>> call,
                                   @NonNull Response<List<Models.CourseRow>> response) {
                List<Models.CourseRow> rows = response.body();
                if (!response.isSuccessful() || rows == null) {
                    Ui.toast(AcademyActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                adapter.setItems(rows);
                empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.CourseRow>> call, @NonNull Throwable t) {
                Ui.toast(AcademyActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }
}
