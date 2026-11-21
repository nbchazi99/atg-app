package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DepositActivity extends AppCompatActivity {

    private final List<Adapters.Option> brokers = new ArrayList<>();
    private final List<Adapters.Option> wallets = new ArrayList<>();
    private OptionsAdapter brokerAdapter;
    private OptionsAdapter walletAdapter;

    private TextInputLayout tilClientId, tilAmount, tilMobile;
    private EditText etClientId, etAmount, etMobile;
    private TextView tvClientHint, tvAmountHint, tvDepositAmount, tvFeeLabel, tvFee, tvTotal, tvTotalNote;
    private MaterialButton btnPay;
    private ProgressBar progress;
    private Models.SettingsRes settings;
    private boolean submitting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);

        View header = findViewById(R.id.btnBack);
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Deposit");
        header.setOnClickListener(v -> finish());
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        tilClientId = findViewById(R.id.tilClientId);
        tilAmount = findViewById(R.id.tilAmount);
        tilMobile = findViewById(R.id.tilMobile);
        etClientId = findViewById(R.id.etClientId);
        etAmount = findViewById(R.id.etAmount);
        etMobile = findViewById(R.id.etMobile);
        tvClientHint = findViewById(R.id.tvClientHint);
        tvAmountHint = findViewById(R.id.tvAmountHint);
        tvDepositAmount = findViewById(R.id.tvDepositAmount);
        tvFeeLabel = findViewById(R.id.tvFeeLabel);
        tvFee = findViewById(R.id.tvFee);
        tvTotal = findViewById(R.id.tvTotal);
        tvTotalNote = findViewById(R.id.tvTotalNote);
        btnPay = findViewById(R.id.btnPay);
        progress = findViewById(R.id.pbDeposit);

        etMobile.setText(ApiClient.session().phone());

        brokers.add(new Adapters.Option("Weltrade", "The broker ATG funds and posts every signal against", "W").color("#1B7A3D"));

        wallets.add(new Adapters.Option("EcoCash", "Mobile money · Econet", "EC"));
        wallets.add(new Adapters.Option("Cash", "Cash payment", "C"));
        wallets.add(new Adapters.Option("SmileCash", "SmileCash mobile money", "SC"));

        brokerAdapter = new OptionsAdapter(index -> updateBroker());
        walletAdapter = new OptionsAdapter(index -> {
        });
        brokerAdapter.setItems(brokers);
        walletAdapter.setItems(wallets);

        RecyclerView rvBrokers = findViewById(R.id.rvBrokers);
        rvBrokers.setLayoutManager(new LinearLayoutManager(this));
        rvBrokers.setAdapter(brokerAdapter);

        RecyclerView rvWallets = findViewById(R.id.rvWallets);
        rvWallets.setLayoutManager(new LinearLayoutManager(this));
        rvWallets.setAdapter(walletAdapter);

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSummary();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnPay.setOnClickListener(v -> submit());
        updateBroker();
        loadSettings();
    }

    private void loadSettings() {
        ApiClient.api().settings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.SettingsRes> call, @NonNull Response<Models.SettingsRes> response) {
                if (response.isSuccessful() && response.body() != null) {
                    settings = response.body();
                    updateSummary();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.SettingsRes> call, @NonNull Throwable t) {
                Ui.toast(DepositActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }

    private void updateBroker() {
        tilClientId.setHint("Weltrade Client ID");
        tvClientHint.setText("In your Weltrade cabinet, under your name. It is not your MT5 login.");
    }

    private void updateSummary() {
        double amount = Ui.parseAmount(text(etAmount), 0);
        double pct = settings == null ? 12 : settings.adminFeePct;
        double min = settings == null ? 5 : settings.minDeposit;
        double max = settings == null ? 454 : settings.maxDeposit;
        double fee = Math.round(amount * pct) / 100.0;
        double total = Math.round((amount + fee) * 100.0) / 100.0;

        tvAmountHint.setText(Ui.money(min) + " to " + Ui.money(max) + ", before the "
                + formatPct(pct) + " admin fee.");
        tvFeeLabel.setText("Admin fee (" + formatPct(pct) + ")");
        tvDepositAmount.setText(Ui.money(amount));
        tvFee.setText("+ " + Ui.money(fee));
        tvTotal.setText(Ui.money(total));
        tvTotalNote.setText(Ui.money(total) + " leaves your wallet. " + Ui.money(amount)
                + " reaches your trading account.");
        btnPay.setText("Pay " + Ui.money(total));
    }

    private void submit() {
        tilAmount.setError(null);
        tilClientId.setError(null);
        tilMobile.setError(null);
        if (submitting) return;

        double amount = Ui.parseAmount(text(etAmount), 0);
        double min = settings == null ? 5 : settings.minDeposit;
        double max = settings == null ? 454 : settings.maxDeposit;
        if (amount <= 0) {
            tilAmount.setError("Enter the amount you want to deposit");
            return;
        }
        if (amount < min) {
            tilAmount.setError("Minimum deposit is " + Ui.money(min));
            return;
        }
        if (amount > max) {
            tilAmount.setError("Maximum deposit is " + Ui.money(max));
            return;
        }
        String clientId = text(etClientId);
        if (clientId.isEmpty()) {
            tilClientId.setError("Enter your broker account details");
            return;
        }
        String mobile = text(etMobile);
        if (mobile.isEmpty()) {
            tilMobile.setError("Enter the number ATG can reach you on");
            return;
        }
        Adapters.Option broker = brokerAdapter.selectedOption();
        Adapters.Option wallet = walletAdapter.selectedOption();
        if (broker == null || wallet == null) return;

        Models.DepositReq req = new Models.DepositReq();
        req.broker = broker.title;
        req.clientId = clientId;
        req.amount = amount;
        req.wallet = wallet.title;
        req.mobile = mobile;

        loading(true);
        ApiClient.api().deposit(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.DepositRes> call, @NonNull Response<Models.DepositRes> response) {
                loading(false);
                if (response.isSuccessful() && response.body() != null) {
                    Models.DepositRes res = response.body();
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(DepositActivity.this)
                            .setTitle("Deposit request filed")
                            .setMessage("Pay " + Ui.money(res.totalPaid) + " to " + res.wallet
                                    + " on " + res.mobile + ". The administrator will review it and your wallet history will update.\n\nReference: DEP-" + res.id)
                            .setPositiveButton("Done", (d, w) -> finish())
                            .setCancelable(false)
                            .show();
                } else {
                    Ui.alert(DepositActivity.this, "Could not file the deposit", ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.DepositRes> call, @NonNull Throwable t) {
                loading(false);
                Ui.alert(DepositActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void loading(boolean active) {
        submitting = active;
        progress.setVisibility(active ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!active);
        if (active) btnPay.setText("Filing request…");
        else updateSummary();
    }

    private static String formatPct(double pct) {
        return pct == Math.rint(pct) ? String.valueOf((long) pct) : String.valueOf(pct);
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
