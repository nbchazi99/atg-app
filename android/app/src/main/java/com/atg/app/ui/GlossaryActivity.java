package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;

import java.util.Arrays;

public class GlossaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glossary);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Glossary");
        findViewById(R.id.btnBell).setVisibility(android.view.View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(android.view.View.GONE);

        RecyclerView list = findViewById(R.id.rvGlossary);
        GlossaryAdapter adapter = new GlossaryAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        adapter.setItems(Arrays.asList(
                new Adapters.GlossaryTerm("Pip", "The smallest standard move of a currency pair. For most pairs that is 0.0001, for yen pairs 0.01."),
                new Adapters.GlossaryTerm("Lot", "The standard trade size. One standard lot is 100,000 units of the base currency."),
                new Adapters.GlossaryTerm("Leverage", "Borrowed exposure from your broker. It multiplies both gains and losses."),
                new Adapters.GlossaryTerm("Margin", "The money your broker holds while a leveraged position is open."),
                new Adapters.GlossaryTerm("Spread", "The difference between the bid and the ask. It is a cost you pay on entry."),
                new Adapters.GlossaryTerm("Stop loss", "An order that closes a trade at a set loss so a bad idea cannot become a disaster."),
                new Adapters.GlossaryTerm("Take profit", "An order that closes a trade once a target is reached."),
                new Adapters.GlossaryTerm("Entry", "The price where your position opens."),
                new Adapters.GlossaryTerm("Break-even", "Moving the stop to your entry so the trade can no longer lose."),
                new Adapters.GlossaryTerm("Drawdown", "The fall from an account's peak to its lowest point. The number that ends most careers."),
                new Adapters.GlossaryTerm("Risk-to-reward", "What you stand to make against what you stand to lose. 1:2 means risking $1 to make $2."),
                new Adapters.GlossaryTerm("Support", "A price area where buyers have previously stepped in."),
                new Adapters.GlossaryTerm("Resistance", "A price area where sellers have previously stepped in."),
                new Adapters.GlossaryTerm("Price action", "Reading the market from the candles themselves, without indicators."),
                new Adapters.GlossaryTerm("Candlestick", "One bar showing the open, high, low and close of a period."),
                new Adapters.GlossaryTerm("Broker", "The firm that holds your trading account and fills your orders."),
                new Adapters.GlossaryTerm("Slippage", "The difference between the price you wanted and the price you got."),
                new Adapters.GlossaryTerm("Swap", "The overnight financing charge for holding a position."),
                new Adapters.GlossaryTerm("Equity", "Your account balance plus or minus the profit and loss on open trades."),
                new Adapters.GlossaryTerm("Position sizing", "Choosing how many lots to trade so one loss cannot hurt the account."),
                new Adapters.GlossaryTerm("ATR", "Average True Range. A measure of how much a pair typically moves."),
                new Adapters.GlossaryTerm("Wella", "An ATG trader who trades on Weltrade, the broker the desk posts calls against.")
        ));

        EditText search = findViewById(R.id.etGlossarySearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
