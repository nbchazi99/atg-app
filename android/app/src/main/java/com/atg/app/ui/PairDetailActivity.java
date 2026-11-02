package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.content.Context;
import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PairDetailActivity extends AppCompatActivity {

    private static final String EXTRA_SYMBOL = "symbol";

    private String symbol;
    private TextView tvBigPrice, tvBigChange, tvHigh, tvLow, tvPrevClose, tvCallsHeading, tvCallsEmpty;
    private MaterialButton btnFavourite;
    private SignalsAdapter adapter;
    private boolean favourite;

    public static void start(Context context, String symbol) {
        Intent intent = new Intent(context, PairDetailActivity.class);
        intent.putExtra(EXTRA_SYMBOL, symbol);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_detail);

        symbol = getIntent().getStringExtra(EXTRA_SYMBOL);
        if (symbol == null) symbol = "EURUSD";

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText(display(symbol));
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        tvBigPrice = findViewById(R.id.tvBigPrice);
        tvBigChange = findViewById(R.id.tvBigChange);
        tvHigh = findViewById(R.id.tvHigh);
        tvLow = findViewById(R.id.tvLow);
        tvPrevClose = findViewById(R.id.tvPrevClose);
        tvCallsHeading = findViewById(R.id.tvCallsHeading);
        tvCallsEmpty = findViewById(R.id.tvCallsEmpty);
        btnFavourite = findViewById(R.id.btnFavouritePair);

        tvCallsHeading.setText("ATG calls on " + display(symbol));

        RecyclerView calls = findViewById(R.id.rvPairCalls);
        adapter = new SignalsAdapter(signal -> SignalDetailActivity.start(this, signal));
        calls.setLayoutManager(new LinearLayoutManager(this));
        calls.setAdapter(adapter);

        btnFavourite.setOnClickListener(v -> toggleFavourite());
        load();
    }

    private void load() {
        ApiClient.api().pair(symbol).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.PairRes> call, @NonNull Response<Models.PairRes> response) {
                Models.PairRes res = response.body();
                if (!response.isSuccessful() || res == null || res.pair == null) {
                    Ui.toast(PairDetailActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                Models.MarketPair p = res.pair;
                ((TextView) findViewById(R.id.tvPairSubtitle)).setText(p.name);
                tvBigPrice.setText(Ui.price(p.price));
                boolean up = p.changePct >= 0;
                tvBigChange.setText(Ui.pct(p.changePct) + " today");
                tvBigChange.setTextColor(getColor(up ? R.color.green_text : R.color.red_text));
                tvHigh.setText(Ui.price(p.high));
                tvLow.setText(Ui.price(p.low));
                tvPrevClose.setText(Ui.price(p.prevClose));

                favourite = res.favourite;
                renderFavourite();

                List<Models.Signal> signals = res.calls;
                adapter.setItems(signals);
                tvCallsEmpty.setVisibility(signals == null || signals.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<Models.PairRes> call, @NonNull Throwable t) {
                Ui.alert(PairDetailActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void toggleFavourite() {
        ApiClient.api().toggleFavourite(symbol).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object value = response.body().get("favourite");
                    favourite = value instanceof Boolean && (Boolean) value;
                    renderFavourite();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Ui.toast(PairDetailActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }

    private void renderFavourite() {
        btnFavourite.setText(favourite ? "Remove from favourites" : "Favourite this pair");
    }

    private static String display(String symbol) {
        String upper = symbol.toUpperCase(Locale.ROOT);
        if (upper.length() == 6) return upper.substring(0, 3) + "/" + upper.substring(3);
        return upper;
    }
}
