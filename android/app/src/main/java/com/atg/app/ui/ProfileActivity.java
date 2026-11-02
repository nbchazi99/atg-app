package com.atg.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.atg.app.data.Session;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private Session session;
    private TextView avatar, name, email, membership, points, role, purchases, serverValue, txEmpty;
    private MaterialButton buyDeriv, buyWeltrade, adminButton;
    private Adapters.TxAdapter txAdapter;
    private boolean ordering;
    private String pendingType;

    private final ActivityResultLauncher<String> proofPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onProofPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        session = ApiClient.session();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Profile");
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        avatar = findViewById(R.id.tvProfileAvatar);
        name = findViewById(R.id.tvProfileName);
        email = findViewById(R.id.tvProfileEmail);
        membership = findViewById(R.id.tvProfileMembership);
        points = findViewById(R.id.tvProfilePoints);
        role = findViewById(R.id.tvProfileRole);
        purchases = findViewById(R.id.tvPurchases);
        serverValue = findViewById(R.id.tvServerValue);
        txEmpty = findViewById(R.id.tvProfileTxEmpty);
        buyDeriv = findViewById(R.id.btnBuyDeriv);
        buyWeltrade = findViewById(R.id.btnBuyWeltrade);
        adminButton = findViewById(R.id.btnAdminDashboard);

        RecyclerView txList = findViewById(R.id.rvProfileTx);
        txAdapter = new Adapters.TxAdapter();
        txList.setLayoutManager(new LinearLayoutManager(this));
        txList.setAdapter(txAdapter);

        buyDeriv.setOnClickListener(v -> beginMembership("DERIV_VIP"));
        buyWeltrade.setOnClickListener(v -> beginMembership("WELTRADE_VIP"));
        adminButton.setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));
        findViewById(R.id.btnServerAddress).setOnClickListener(v -> editServer());
        findViewById(R.id.btnLogout).setOnClickListener(v -> confirmLogout());

        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        loadProfile();
        loadWallet();
        loadPurchases();
    }

    private void render() {
        avatar.setText(session.initials());
        name.setText(session.name());
        email.setText(session.email());
        membership.setText(session.membershipLabel().toUpperCase(Locale.ROOT));
        points.setText(session.points() + " POINTS");
        role.setText(session.isAdmin() ? "ADMIN" : "LEARNER");
        serverValue.setText(session.server());
        adminButton.setVisibility(session.isAdmin() ? View.VISIBLE : View.GONE);
        applyMembershipButtons();
    }

    private void applyMembershipButtons() {
        String current = session.membership();
        boolean deriv = "DERIV_VIP".equals(current);
        boolean weltrade = "WELTRADE_VIP".equals(current);
        buyDeriv.setEnabled(!deriv && !ordering);
        buyDeriv.setText(deriv ? "Active membership" : "Pay proof and join");
        buyWeltrade.setEnabled(!weltrade && !ordering);
        buyWeltrade.setText(weltrade ? "Active membership" : "Pay proof and join");
    }

    private void loadProfile() {
        ApiClient.api().me().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.User> call, @NonNull Response<Models.User> response) {
                Models.User user = response.body();
                if (!response.isSuccessful() || user == null) return;
                session.saveUser(user);
                render();
            }

            @Override
            public void onFailure(@NonNull Call<Models.User> call, @NonNull Throwable t) {
            }
        });
    }

    private void loadWallet() {
        ApiClient.api().wallet().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.WalletRes> call, @NonNull Response<Models.WalletRes> response) {
                Models.WalletRes wallet = response.body();
                if (!response.isSuccessful() || wallet == null) return;
                List<Models.WalletTx> items = wallet.transactions;
                txAdapter.setItems(items == null ? Collections.emptyList() : items);
                boolean empty = items == null || items.isEmpty();
                txEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<Models.WalletRes> call, @NonNull Throwable t) {
                txEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadPurchases() {
        ApiClient.api().purchases().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.PurchaseRes>> call,
                                   @NonNull Response<List<Models.PurchaseRes>> response) {
                List<Models.PurchaseRes> items = response.body();
                if (!response.isSuccessful() || items == null || items.isEmpty()) {
                    purchases.setText("No purchases yet.");
                    return;
                }
                StringBuilder builder = new StringBuilder();
                for (Models.PurchaseRes p : items) {
                    if (builder.length() > 0) builder.append('\n');
                    builder.append(label(p.type))
                            .append(" · ")
                            .append(Ui.money(p.amount))
                            .append(" · ")
                            .append(p.status)
                            .append(" · ")
                            .append(Ui.timeAgo(p.createdAt));
                }
                purchases.setText(builder.toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.PurchaseRes>> call, @NonNull Throwable t) {
                purchases.setText("Could not load purchases.");
            }
        });
    }

    private static String label(String type) {
        if ("COURSE".equals(type)) return "Course";
        if ("VIP_DERIV".equals(type)) return "Deriv VIP";
        if ("VIP_WELTRADE".equals(type)) return "Weltrade VIP";
        return type;
    }

    private void beginMembership(String membershipType) {
        if (ordering) return;
        String label = "DERIV_VIP".equals(membershipType) ? "Deriv VIP" : "Weltrade VIP";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Join " + label)
                .setMessage("Pay $30.00 to the ATG desk, then attach a screenshot of the payment. "
                        + "The administrator reviews proofs manually and unlocks the room once approved.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Choose screenshot", (d, w) -> {
                    pendingType = membershipType;
                    proofPicker.launch("image/*");
                })
                .show();
    }

    private void onProofPicked(Uri uri) {
        if (uri == null || ordering || pendingType == null) return;
        ordering = true;
        applyMembershipButtons();
        String label = "DERIV_VIP".equals(pendingType) ? "Deriv VIP" : "Weltrade VIP";
        MaterialButton active = "DERIV_VIP".equals(pendingType) ? buyDeriv : buyWeltrade;
        active.setText("Uploading proof…");

        ApiClient.upload(this, uri, new ApiClient.UploadCallback() {
            @Override
            public void ok(String url) {
                active.setText("Filing purchase…");
                ApiClient.api().purchase(new Models.PurchaseReq(pendingType, null, url))
                        .enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<Models.PurchaseRes> call,
                                                   @NonNull Response<Models.PurchaseRes> response) {
                                ordering = false;
                                pendingType = null;
                                render();
                                if (response.isSuccessful() && response.body() != null) {
                                    Ui.alert(ProfileActivity.this, "Request filed",
                                            "Reference P-" + response.body().id + ". " + label
                                                    + " unlocks here once the administrator approves the proof.");
                                    loadPurchases();
                                } else {
                                    Ui.alert(ProfileActivity.this, "Could not file the purchase",
                                            ApiClient.errorMessage(response));
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Models.PurchaseRes> call, @NonNull Throwable t) {
                                ordering = false;
                                pendingType = null;
                                render();
                                Ui.alert(ProfileActivity.this, "Server not reachable",
                                        ApiClient.connectionMessage(t));
                            }
                        });
            }

            @Override
            public void fail(String message) {
                ordering = false;
                pendingType = null;
                render();
                Ui.alert(ProfileActivity.this, "Could not upload", message);
            }
        });
    }

    private void editServer() {
        EditText input = new EditText(this);
        input.setText(session.server());
        input.setSingleLine(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle("ATG server address")
                .setMessage("Use 10.0.2.2 for the Android emulator, or your computer's local IP when using a phone on the same network.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    session.setServer(input.getText().toString());
                    ApiClient.reloadServer();
                    serverValue.setText(session.server());
                })
                .show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign out?")
                .setMessage("Your session on this phone ends. Courses, purchases and requests stay on your account.")
                .setNegativeButton("Stay", null)
                .setPositiveButton("Sign out", (d, w) -> {
                    session.logout();
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                })
                .show();
    }
}
