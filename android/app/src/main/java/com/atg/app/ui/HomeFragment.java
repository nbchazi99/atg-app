package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.content.Intent;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView tvPoints, tvToday, tvGreeting, tvLiveLine, tvPromoTitle, tvPromoBody, tvFact;
    private RecyclerView rvSignals, rvTickers;
    private View signalEmpty;
    private SwipeRefreshLayout swipe;
    private final SignalsAdapter signalsAdapter = new SignalsAdapter(signal ->
            SignalDetailActivity.start(requireContext(), signal));
    private final TickerAdapter tickerAdapter = new TickerAdapter();
    private String fact = "Black Monday";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvPoints = view.findViewById(R.id.tvPoints);
        tvToday = view.findViewById(R.id.tvToday);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvLiveLine = view.findViewById(R.id.tvLiveLine);
        tvPromoTitle = view.findViewById(R.id.tvPromoTitle);
        tvPromoBody = view.findViewById(R.id.tvPromoBody);
        tvFact = view.findViewById(R.id.tvFact);
        rvSignals = view.findViewById(R.id.rvHomeSignals);
        rvTickers = view.findViewById(R.id.rvHomeTickers);
        signalEmpty = view.findViewById(R.id.homeSignalEmpty);
        swipe = view.findViewById(R.id.swipeHome);

        rvSignals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSignals.setAdapter(signalsAdapter);
        rvTickers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTickers.setAdapter(tickerAdapter);

        TextView avatar = view.findViewById(R.id.tvHomeAvatar);
        avatar.setText(ApiClient.session().initials());
        avatar.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProfileActivity.class)));

        view.findViewById(R.id.btnHomeBell).setOnClickListener(v ->
                Ui.alert(requireContext(), "Notifications", "You are all caught up. New calls and payment decisions appear here."));

        view.findViewById(R.id.actDeposit).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DepositActivity.class)));
        view.findViewById(R.id.actWithdraw).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), WithdrawActivity.class)));
        view.findViewById(R.id.actRooms).setOnClickListener(v -> openTab(R.id.nav_rooms));
        view.findViewById(R.id.actCalls).setOnClickListener(v -> openTab(R.id.nav_signals));

        view.findViewById(R.id.toolPip).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PipCalculatorActivity.class)));
        view.findViewById(R.id.toolGlossary).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), GlossaryActivity.class)));
        view.findViewById(R.id.toolAcademy).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AcademyActivity.class)));

        view.findViewById(R.id.btnDismissPromo).setOnClickListener(v -> {
            View header = (View) v.getParent();
            ((View) header.getParent()).setVisibility(View.GONE);
        });
        view.findViewById(R.id.btnDismissInvite).setOnClickListener(v -> {
            View card = (View) view.findViewById(R.id.btnInviteInfo).getParent();
            card.setVisibility(View.GONE);
        });

        view.findViewById(R.id.cardFact).setOnClickListener(v ->
                Ui.alert(requireContext(), "Today's fact", fact));
        view.findViewById(R.id.btnInviteInfo).setOnClickListener(v -> Ui.alert(requireContext(),
                "Invite and earn",
                "Share your ATG code with friends who want to learn. When they get verified you earn 5 points. Points unlock courses and VIP trials."));
        view.findViewById(R.id.btnWellaYes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));
        view.findViewById(R.id.btnWellaNo).setOnClickListener(v ->
                Ui.toast(requireContext(), "Weltrade is the broker ATG posts every call against. Membership unlocks the VIP rooms."));

        swipe.setColorSchemeResources(R.color.primary);
        swipe.setOnRefreshListener(this::load);

        LocalDateTime now = LocalDateTime.now();
        String day = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US).toUpperCase(Locale.ROOT);
        String part = now.getHour() < 12 ? "Morning" : now.getHour() < 17 ? "Afternoon" : "Evening";
        String first = ApiClient.session().name().split("\\s+")[0];
        tvToday.setText(day);
        tvGreeting.setText(part + ", " + first + ".");
        tvPoints.setText(String.valueOf(ApiClient.session().points()));

        load();
    }

    private void openTab(int id) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openTab(id);
        }
    }

    private void load() {
        ApiClient.api().home().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.HomeRes> call, @NonNull Response<Models.HomeRes> response) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Models.HomeRes home = response.body();
                if (!response.isSuccessful() || home == null) return;
                tvPoints.setText(String.valueOf(home.points));
                tvLiveLine.setText(home.liveCount == 1 ? "1 call is live." : home.liveCount + " calls are live.");
                if (home.promotionTitle != null && !home.promotionTitle.isBlank()) {
                    tvPromoTitle.setText(home.promotionTitle);
                    tvPromoBody.setText(home.promotionBody == null ? "" : home.promotionBody);
                }
                if (home.fact != null && !home.fact.isBlank()) {
                    fact = home.fact;
                    tvFact.setText(home.fact);
                }
                tickerAdapter.setItems(home.tickers);
                ApiClient.session().savePoints(home.points);
            }

            @Override
            public void onFailure(@NonNull Call<Models.HomeRes> call, @NonNull Throwable t) {
                if (isAdded()) swipe.setRefreshing(false);
            }
        });

        ApiClient.api().signals("LIVE", null).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.Signal>> call,
                                   @NonNull Response<List<Models.Signal>> response) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                if (!response.isSuccessful() || response.body() == null) return;
                List<Models.Signal> live = response.body();
                List<Models.Signal> top = live.isEmpty() ? live : live.subList(0, 1);
                signalsAdapter.setItems(top);
                signalEmpty.setVisibility(top.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.Signal>> call, @NonNull Throwable t) {
                if (isAdded()) swipe.setRefreshing(false);
            }
        });
    }
}
