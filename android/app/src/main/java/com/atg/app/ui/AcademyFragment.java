package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

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

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AcademyFragment extends Fragment {

    private CoursesAdapter adapter;
    private View empty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_academy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnBack).setVisibility(View.GONE);
        ((TextView) view.findViewById(R.id.tvHeaderTitle)).setText("Academy");
        view.findViewById(R.id.btnBell).setVisibility(View.GONE);
        view.findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        RecyclerView list = view.findViewById(R.id.rvCourses);
        adapter = new CoursesAdapter(row ->
                CourseDetailActivity.start(requireContext(), row.course.id));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        empty = view.findViewById(R.id.coursesEmpty);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.api().courses().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.CourseRow>> call,
                                   @NonNull Response<List<Models.CourseRow>> response) {
                if (!isAdded()) return;
                List<Models.CourseRow> rows = response.body();
                if (!response.isSuccessful() || rows == null) {
                    Ui.toast(requireContext(), ApiClient.errorMessage(response));
                    return;
                }
                adapter.setItems(rows);
                empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.CourseRow>> call, @NonNull Throwable t) {
                if (isAdded()) Ui.toast(requireContext(), ApiClient.connectionMessage(t));
            }
        });
    }
}
