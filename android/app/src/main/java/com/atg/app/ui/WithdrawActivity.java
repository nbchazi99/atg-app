package com.atg.app.ui;

import com.atg.app.ui.Adapters.*;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WithdrawActivity extends AppCompatActivity {

    private int step = 1;
    private final List<Adapters.Option> payouts = new ArrayList<>();
    private final List<Adapters.Option> sources = new ArrayList<>();
    private OptionsAdapter payoutAdapter;
    private OptionsAdapter sourceAdapter;

    private LinearLayout groupPayout, groupDetails, groupTransfer, groupDone;
    private TextView tvStepPayout, tvStepDetails, tvStepTransfer, tvStepDone;
    private TextView tvStepLabel, tvFiledUnder, tvPayoutHint, tvInstructionTitle, tvInstructionBody;
    private TextView tvTransferHint, tvProofName, tvMinWithdraw;
    private TextView tvWithdrawAmount, tvWithdrawFeeLabel, tvWithdrawFee, tvYouReceive;
    private TextView tvDoneBody, tvDoneNote;
    private View barPayout, barDetails, barTransfer, barDone;
    private TextInputLayout tilPayoutMobile, tilWithdrawAmount, tilWithdrawMobile;
    private TextInputLayout tilWithdrawClientId;
    private EditText etPayoutMobile, etWithdrawAmount, etWithdrawMobile, etWithdrawClientId;
    private MaterialButton btnContinue1, btnContinue2, btnBack2, btnRequest, btnBack3, btnDone, btnChooseProof;
    private ScrollView scroll;
    private Models.SettingsRes settings;
    private String proofUrl;
    private boolean submitting;

    private final ActivityResultLauncher<String> proofPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onProofPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Withdraw");
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        scroll = findViewById(R.id.scrollWithdraw);
        groupPayout = findViewById(R.id.groupPayout);
        groupDetails = findViewById(R.id.groupDetails);
        groupTransfer = findViewById(R.id.groupTransfer);
        groupDone = findViewById(R.id.groupDone);

        tvStepPayout = findViewById(R.id.tvStepPayout);
        tvStepDetails = findViewById(R.id.tvStepDetails);
        tvStepTransfer = findViewById(R.id.tvStepTransfer);
        tvStepDone = findViewById(R.id.tvStepDone);
        tvStepLabel = findViewById(R.id.tvStepLabel);
        barPayout = findViewById(R.id.barPayout);
        barDetails = findViewById(R.id.barDetails);
        barTransfer = findViewById(R.id.barTransfer);
        barDone = findViewById(R.id.barDone);

        tvFiledUnder = findViewById(R.id.tvFiledUnder);
        tvPayoutHint = findViewById(R.id.tvPayoutHint);
        tvInstructionTitle = findViewById(R.id.tvInstructionTitle);
        tvInstructionBody = findViewById(R.id.tvInstructionBody);
        tvTransferHint = findViewById(R.id.tvTransferHint);
        tvProofName = findViewById(R.id.tvProofName);
        tvMinWithdraw = findViewById(R.id.tvMinWithdraw);
        tvWithdrawAmount = findViewById(R.id.tvWithdrawAmount);
        tvWithdrawFeeLabel = findViewById(R.id.tvWithdrawFeeLabel);
        tvWithdrawFee = findViewById(R.id.tvWithdrawFee);
        tvYouReceive = findViewById(R.id.tvYouReceive);
        tvDoneBody = findViewById(R.id.tvDoneBody);
        tvDoneNote = findViewById(R.id.tvDoneNote);

        tilPayoutMobile = findViewById(R.id.tilPayoutMobile);
        tilWithdrawAmount = findViewById(R.id.tilWithdrawAmount);
        tilWithdrawMobile = findViewById(R.id.tilWithdrawMobile);
        tilWithdrawClientId = findViewById(R.id.tilWithdrawClientId);
        etPayoutMobile = findViewById(R.id.etPayoutMobile);
        etWithdrawAmount = findViewById(R.id.etWithdrawAmount);
        etWithdrawMobile = findViewById(R.id.etWithdrawMobile);
        etWithdrawClientId = findViewById(R.id.etWithdrawClientId);

        btnContinue1 = findViewById(R.id.btnContinue1);
        btnContinue2 = findViewById(R.id.btnContinue2);
        btnBack2 = findViewById(R.id.btnBack2);
        btnRequest = findViewById(R.id.btnRequest);
        btnBack3 = findViewById(R.id.btnBack3);
        btnDone = findViewById(R.id.btnDone);
        btnChooseProof = findViewById(R.id.btnChooseProof);

        etPayoutMobile.setText(ApiClient.session().phone());
        etWithdrawMobile.setText(ApiClient.session().phone());
        tvFiledUnder.setText("Filed under " + ApiClient.session().name() + ", " + ApiClient.session().email() + ".");

        payouts.add(new Adapters.Option("EcoCash", "Mobile money", "EC"));
        payouts.add(new Adapters.Option("SmileCash", "Mobile money", "SC").tag("No fee"));
        payouts.add(new Adapters.Option("Cash", "Collect at the office", "CA"));

        sources.add(new Adapters.Option("Weltrade withdrawal", "You withdrew to the ATG agent on Weltrade.", "W").color("#1B7A3D"));
        sources.add(new Adapters.Option("Deriv withdrawal", "You withdrew to the ATG agent on Deriv.", "D").color("#B3261E"));
        sources.add(new Adapters.Option("Crypto withdrawal", "You sent USDT from your own wallet to ATG.", "C"));

        payoutAdapter = new OptionsAdapter(index -> updatePayoutFields());
        sourceAdapter = new OptionsAdapter(index -> updateSource());
        payoutAdapter.setItems(payouts);
        sourceAdapter.setItems(sources);

        RecyclerView rvPayout = findViewById(R.id.rvPayout);
        rvPayout.setLayoutManager(new LinearLayoutManager(this));
        rvPayout.setAdapter(payoutAdapter);

        RecyclerView rvSources = findViewById(R.id.rvSources);
        rvSources.setLayoutManager(new LinearLayoutManager(this));
        rvSources.setAdapter(sourceAdapter);

        etWithdrawAmount.addTextChangedListener(new TextWatcher() {
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

        btnContinue1.setOnClickListener(v -> {
            if (validatePayout()) show(2);
        });
        btnContinue2.setOnClickListener(v -> {
            if (validateDetails()) show(3);
        });
        btnBack2.setOnClickListener(v -> show(1));
        btnBack3.setOnClickListener(v -> show(2));
        btnChooseProof.setOnClickListener(v -> proofPicker.launch("image/*"));
        btnRequest.setOnClickListener(v -> submit());
        btnDone.setOnClickListener(v -> finish());

        updatePayoutFields();
        updateSource();
        show(1);
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
                Ui.toast(WithdrawActivity.this, ApiClient.connectionMessage(t));
            }
        });
    }

    private void updatePayoutFields() {
        boolean mobileMoney = payoutAdapter.selectedIndex() < 2;
        tilPayoutMobile.setVisibility(mobileMoney ? View.VISIBLE : View.GONE);
        tvPayoutHint.setVisibility(mobileMoney ? View.VISIBLE : View.GONE);
    }

    private void updateSource() {
        int index = sourceAdapter.selectedIndex();
        if (index == 1) {
            tvInstructionTitle.setText("Withdrawing through Deriv");
            tvInstructionBody.setText("1. In Deriv, start a withdrawal to the ATG agent.\n\n2. When it completes, screenshot the confirmation.\n\n3. Attach that screenshot and enter your Deriv nickname.");
            tilWithdrawClientId.setHint("Deriv nickname");
            tvTransferHint.setText("The nickname on your Deriv account, not your email.");
        } else if (index == 2) {
            tvInstructionTitle.setText("Withdrawing through crypto");
            tvInstructionBody.setText("1. Send USDT to the ATG wallet address the desk gives you.\n\n2. When the transfer confirms, screenshot it.\n\n3. Attach that screenshot and paste the sending wallet address.");
            tilWithdrawClientId.setHint("USDT wallet address you sent from");
            tvTransferHint.setText("The address the USDT left. Double-check every character.");
        } else {
            tvInstructionTitle.setText("Withdrawing through Weltrade");
            tvInstructionBody.setText("1. In Weltrade, start a withdrawal to the \"ATG\" agent.\n\n2. When it completes, screenshot the confirmation.\n\n3. Attach that screenshot and enter your Weltrade Client ID.");
            tilWithdrawClientId.setHint("Weltrade Client ID");
            tvTransferHint.setText("In your Weltrade cabinet, under your name. It is not your MT5 login.");
        }
    }

    private void updateSummary() {
        double amount = Ui.parseAmount(text(etWithdrawAmount), 0);
        double pct = settings == null ? 7 : settings.withdrawFeePct;
        double min = settings == null ? 12.50 : settings.minWithdraw;
        double fee = Math.round(amount * pct) / 100.0;
        double receive = Math.round((amount - fee) * 100.0) / 100.0;

        tvMinWithdraw.setText("Minimum withdrawal is " + Ui.money(min) + ".");
        tvWithdrawFeeLabel.setText("Withdrawal charge (" + formatPct(pct) + "%)");
        tvWithdrawAmount.setText(Ui.money(amount));
        tvWithdrawFee.setText("- " + Ui.money(fee));
        tvYouReceive.setText(Ui.money(receive));
        btnRequest.setText("Request " + Ui.money(receive));
    }

    private boolean validatePayout() {
        if (payoutAdapter.selectedIndex() < 2 && text(etPayoutMobile).isEmpty()) {
            Ui.toast(this, "Enter the mobile money number");
            return false;
        }
        return true;
    }

    private boolean validateDetails() {
        tilWithdrawAmount.setError(null);
        tilWithdrawMobile.setError(null);
        double amount = Ui.parseAmount(text(etWithdrawAmount), 0);
        double min = settings == null ? 12.50 : settings.minWithdraw;
        if (amount <= 0) {
            tilWithdrawAmount.setError("Enter the amount you want to withdraw");
            return false;
        }
        if (amount < min) {
            tilWithdrawAmount.setError("Minimum withdrawal is " + Ui.money(min));
            return false;
        }
        if (text(etWithdrawMobile).isEmpty()) {
            tilWithdrawMobile.setError("Enter the number ATG can reach you on");
            return false;
        }
        return true;
    }

    private void submit() {
        tilWithdrawClientId.setError(null);
        if (submitting) return;
        if (text(etWithdrawClientId).isEmpty()) {
            tilWithdrawClientId.setError("Enter your account details");
            return;
        }
        if (proofUrl == null || proofUrl.isBlank()) {
            Ui.toast(this, "Attach the screenshot of the broker confirmation");
            return;
        }
        Adapters.Option payout = payoutAdapter.selectedOption();
        Adapters.Option source = sourceAdapter.selectedOption();
        if (payout == null || source == null) return;

        Models.WithdrawReq req = new Models.WithdrawReq();
        req.payout = payout.title;
        req.mobile = text(etPayoutMobile).isEmpty() ? text(etWithdrawMobile) : text(etPayoutMobile);
        req.amount = Ui.parseAmount(text(etWithdrawAmount), 0);
        req.sourceType = source.title;
        req.sourceDetail = text(etWithdrawClientId);
        req.proofUrl = proofUrl;

        submitting = true;
        btnRequest.setEnabled(false);
        btnRequest.setText("Requesting…");
        ApiClient.api().withdraw(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.WithdrawRes> call, @NonNull Response<Models.WithdrawRes> response) {
                submitting = false;
                btnRequest.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Models.WithdrawRes res = response.body();
                    tvDoneBody.setText("We will send " + Ui.money(res.receive) + " to " + res.payout
                            + " on " + res.mobile + ".");
                    tvDoneNote.setText("Reference W-" + res.id
                            + ". The administrator reviews withdrawals manually. Your wallet history shows the decision.");
                    show(4);
                } else {
                    updateSummary();
                    Ui.alert(WithdrawActivity.this, "Could not file the withdrawal", ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.WithdrawRes> call, @NonNull Throwable t) {
                submitting = false;
                btnRequest.setEnabled(true);
                updateSummary();
                Ui.alert(WithdrawActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void onProofPicked(Uri uri) {
        if (uri == null) return;
        tvProofName.setText("Uploading screenshot…");
        ApiClient.upload(this, uri, new ApiClient.UploadCallback() {
            @Override
            public void ok(String url) {
                proofUrl = url;
                tvProofName.setText("Screenshot attached. The administrator will check it.");
            }

            @Override
            public void fail(String message) {
                tvProofName.setText("Upload failed. Tap to choose the screenshot again.");
                Ui.alert(WithdrawActivity.this, "Could not upload", message);
            }
        });
    }

    private void show(int target) {
        step = target;
        groupPayout.setVisibility(target == 1 ? View.VISIBLE : View.GONE);
        groupDetails.setVisibility(target == 2 ? View.VISIBLE : View.GONE);
        groupTransfer.setVisibility(target == 3 ? View.VISIBLE : View.GONE);
        groupDone.setVisibility(target == 4 ? View.VISIBLE : View.GONE);
        tvStepLabel.setText("Step " + target + " of 4");
        paint(tvStepPayout, barPayout, target >= 1);
        paint(tvStepDetails, barDetails, target >= 2);
        paint(tvStepTransfer, barTransfer, target >= 3);
        paint(tvStepDone, barDone, target >= 4);
        scroll.smoothScrollTo(0, 0);
    }

    private void paint(TextView label, View bar, boolean active) {
        label.setTextColor(ContextCompat.getColor(this, active ? R.color.gold : R.color.muted));
        bar.setBackgroundColor(ContextCompat.getColor(this, active ? R.color.primary : R.color.stroke_soft));
    }

    @Override
    public void onBackPressed() {
        if (step > 1 && step < 4) {
            show(step - 1);
        } else {
            super.onBackPressed();
        }
    }

    private static String formatPct(double pct) {
        return pct == Math.rint(pct) ? String.valueOf((long) pct) : String.valueOf(pct);
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
