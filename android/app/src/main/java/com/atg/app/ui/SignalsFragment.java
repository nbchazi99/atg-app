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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignalsFragment extends Fragment {

    private SignalsAdapter adapter;
    private TickerAdapter tickerAdapter;
    private SwipeRefreshLayout swipe;
    private View empty;
    private TextView subtitle;
    private MaterialButton chipLive, chipOpen, chipInner, chipClosed;
    private String filter = "LIVE";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView title = view.findViewById(R.id.tvAppTitle);
        subtitle = view.findViewById(R.id.tvAppSubtitle);
        title.setText("Signals");
        subtitle.setVisibility(View.VISIBLE);
        subtitle.setText("Loading the floor…");

        TextView avatar = view.findViewById(R.id.tvAvatar);
        avatar.setText(ApiClient.session().initials());
        avatar.setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), ProfileActivity.class)));
        view.findViewById(R.id.btnBell).setOnClickListener(v ->
                Ui.alert(requireContext(), "Notifications", "New calls and updates land here. You are all caught up."));

        chipLive = view.findViewById(R.id.btnFilterLive);
        chipOpen = view.findViewById(R.id.btnFilterOpen);
        chipInner = view.findViewById(R.id.btnFilterInner);
        chipClosed = view.findViewById(R.id.btnFilterClosed);

        chipLive.setOnClickListener(v -> select(chipLive, "LIVE"));
        chipOpen.setOnClickListener(v -> select(chipOpen, "OPEN"));
        chipInner.setOnClickListener(v -> select(chipInner, "INNER"));
        chipClosed.setOnClickListener(v -> select(chipClosed, "CLOSED"));

        RecyclerView tickers = view.findViewById(R.id.rvSignalTickers);
        tickerAdapter = new TickerAdapter();
        tickers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        tickers.setAdapter(tickerAdapter);

        RecyclerView list = view.findViewById(R.id.rvSignals);
        adapter = new SignalsAdapter(signal -> SignalDetailActivity.start(requireContext(), signal));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        empty = view.findViewById(R.id.signalsEmpty);
        swipe = view.findViewById(R.id.swipeSignals);
        swipe.setColorSchemeResources(R.color.primary);
        swipe.setOnRefreshListener(this::load);

        select(chipLive, "LIVE");
    }

    private void select(MaterialButton chip, String value) {
        filter = value;
        Ui.setChip(chipLive, chip == chipLive);
        Ui.setChip(chipOpen, chip == chipOpen);
        Ui.setChip(chipInner, chip == chipInner);
        Ui.setChip(chipClosed, chip == chipClosed);
        load();
    }

    private void load() {
        ApiClient.api().signals(filter, null).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.Signal>> call,
                                   @NonNull Response<List<Models.Signal>> response) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                List<Models.Signal> items = response.body();
                if (!response.isSuccessful() || items == null) {
                    Ui.toast(requireContext(), ApiClient.errorMessage(response));
                    return;
                }
                adapter.setItems(items);
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                subtitle.setText(items.size() == 1 ? "1 call" : items.size() + " calls");
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.Signal>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Ui.toast(requireContext(), ApiClient.connectionMessage(t));
            }
        });

        ApiClient.api().home().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.HomeRes> call, @NonNull Response<Models.HomeRes> response) {
                if (!isAdded() || response.body() == null) return;
                tickerAdapter.setItems(response.body().tickers);
                subtitle.setText(response.body().liveCount + " live now");
            }

            @Override
            public void onFailure(@NonNull Call<Models.HomeRes> call, @NonNull Throwable t) {
            }
        });
    }
}
