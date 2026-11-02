package com.atg.app.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.atg.app.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class PipCalculatorActivity extends AppCompatActivity {

    private EditText pair, lots, price;
    private TextView value, detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pip_calculator);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Pip calculator");
        findViewById(R.id.btnBell).setVisibility(android.view.View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(android.view.View.GONE);

        pair = findViewById(R.id.etPipPair);
        lots = findViewById(R.id.etPipLots);
        price = findViewById(R.id.etPipPrice);
        value = findViewById(R.id.tvPipValue);
        detail = findViewById(R.id.tvPipDetail);
        MaterialButton calculate = findViewById(R.id.btnCalculate);

        pair.setText("EURUSD");
        lots.setText("1");
        calculate.setOnClickListener(v -> calculate());
        calculate();
    }

    private void calculate() {
        String symbol = pair.getText() == null ? "" : pair.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (symbol.isEmpty()) {
            Ui.toast(this, "Enter a pair, for example EURUSD");
            return;
        }
        double lotSize = Ui.parseAmount(lots.getText() == null ? "" : lots.getText().toString(), 1);
        if (lotSize <= 0) {
            Ui.toast(this, "Enter a lot size above zero");
            return;
        }
        double currentPrice = Ui.parseAmount(price.getText() == null ? "" : price.getText().toString(), 0);

        boolean gold = symbol.startsWith("XAU");
        boolean crypto = symbol.startsWith("BTC") || symbol.startsWith("ETH") || symbol.startsWith("SOL");
        boolean yen = symbol.endsWith("JPY");

        double pipSize;
        double contract;
        if (gold) {
            pipSize = 0.1;
            contract = 100;
        } else if (crypto) {
            pipSize = 1;
            contract = 1;
        } else if (yen) {
            pipSize = 0.01;
            contract = 100000;
        } else {
            pipSize = 0.0001;
            contract = 100000;
        }

        double perPip;
        if (yen) {
            if (currentPrice <= 0) {
                value.setText("—");
                detail.setText("Yen pairs are quoted in yen, so enter the current price to convert the pip value into US dollars.");
                return;
            }
            perPip = contract * pipSize / currentPrice;
        } else {
            perPip = contract * pipSize;
        }

        double total = perPip * lotSize;
        value.setText(Ui.money(total));
        String lotText = lotSize == 1 ? "1 standard lot" : String.format(Locale.US, "%.2f lots", lotSize);
        detail.setText("One pip on " + symbol + " (" + lotText + ") is worth " + Ui.money(total)
                + ". Pip size " + String.format(Locale.US, "%.4f", pipSize)
                + (crypto ? ", 1 coin per lot." : gold ? ", 100oz per lot." : ", 100,000 units per lot."));
    }
}
