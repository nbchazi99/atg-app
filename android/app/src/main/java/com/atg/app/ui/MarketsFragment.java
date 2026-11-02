package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MarketsFragment extends Fragment {

    private MarketsAdapter adapter;
    private SwipeRefreshLayout swipe;
    private View empty;
    private TextView label;
    private EditText search;
    private MaterialButton chipFav, chipForex, chipMetals, chipCrypto;
    private String category = "forex";
    private List<Models.MarketPair> all = new ArrayList<>();
    private List<Models.MarketPair> favourites = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_markets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView title = view.findViewById(R.id.tvAppTitle);
        title.setText("Markets");
        TextView avatar = view.findViewById(R.id.tvAvatar);
        avatar.setText(ApiClient.session().initials());
        avatar.setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), ProfileActivity.class)));
        view.findViewById(R.id.btnBell).setOnClickListener(v ->
                Ui.alert(requireContext(), "Notifications", "You are all caught up."));

        label = view.findViewById(R.id.tvPairsLabel);
        search = view.findViewById(R.id.etSearch);
        chipFav = view.findViewById(R.id.btnCatFav);
        chipForex = view.findViewById(R.id.btnCatForex);
        chipMetals = view.findViewById(R.id.btnCatMetals);
        chipCrypto = view.findViewById(R.id.btnCatCrypto);

        chipFav.setOnClickListener(v -> choose(chipFav, "favourites"));
        chipForex.setOnClickListener(v -> choose(chipForex, "forex"));
        chipMetals.setOnClickListener(v -> choose(chipMetals, "metals"));
        chipCrypto.setOnClickListener(v -> choose(chipCrypto, "crypto"));

        RecyclerView list = view.findViewById(R.id.rvMarkets);
        adapter = new MarketsAdapter(new Adapters.OnMarket() {
            @Override
            public void open(Models.MarketPair pair) {
                PairDetailActivity.start(requireContext(), pair.symbol);
            }

            @Override
            public void favourite(Models.MarketPair pair, int position) {
                ApiClient.api().toggleFavourite(pair.symbol).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                                           @NonNull Response<java.util.Map<String, Object>> response) {
                        loadFavourites();
                        if ("favourites".equals(category)) load();
                    }

                    @Override
                    public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                        Ui.toast(requireContext(), ApiClient.connectionMessage(t));
                    }
                });
            }
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        empty = view.findViewById(R.id.marketsEmpty);
        swipe = view.findViewById(R.id.swipeMarkets);
        swipe.setColorSchemeResources(R.color.primary);
        swipe.setOnRefreshListener(this::load);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        choose(chipForex, "forex");
        loadFavourites();
    }

    private void choose(MaterialButton chip, String value) {
        category = value;
        Ui.setChip(chipFav, chip == chipFav);
        Ui.setChip(chipForex, chip == chipForex);
        Ui.setChip(chipMetals, chip == chipMetals);
        Ui.setChip(chipCrypto, chip == chipCrypto);
        label.setText(value.equals("favourites") ? "FAVOURITES" : value.toUpperCase(Locale.ROOT));
        search.setHint(value.equals("favourites") ? "Search favourites" : "Search " + value + " pairs");
        load();
    }

    private void load() {
        String requested = category;
        ApiClient.api().markets(requested).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.MarketPair>> call,
                                   @NonNull Response<List<Models.MarketPair>> response) {
                if (!isAdded() || !requested.equals(category)) return;
                swipe.setRefreshing(false);
                List<Models.MarketPair> items = response.body();
                if (!response.isSuccessful() || items == null) {
                    Ui.toast(requireContext(), ApiClient.errorMessage(response));
                    return;
                }
                all = new ArrayList<>(items);
                applyFilter();
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.MarketPair>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Ui.toast(requireContext(), ApiClient.connectionMessage(t));
            }
        });
    }

    private void loadFavourites() {
        ApiClient.api().markets("favourites").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.MarketPair>> call,
                                   @NonNull Response<List<Models.MarketPair>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    favourites = response.body();
                    adapter.setFavourites(favourites);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.MarketPair>> call, @NonNull Throwable t) {
            }
        });
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Models.MarketPair> filtered = new ArrayList<>();
        for (Models.MarketPair p : all) {
            if (q.isEmpty() || p.symbol.toLowerCase(Locale.ROOT).contains(q)
                    || (p.name != null && p.name.toLowerCase(Locale.ROOT).contains(q))) {
                filtered.add(p);
            }
        }
        adapter.setItems(filtered);
        empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
