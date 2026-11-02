package com.atg.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private boolean registerMode;
    private TextInputLayout tilName, tilPhone, tilEmail, tilPassword;
    private EditText etName, etPhone, etEmail, etPassword;
    private MaterialButton btnModeLogin, btnModeRegister, btnSubmit;
    private ProgressBar progress;
    private TextView tvServer, tvAuthTitle, tvAuthSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        tilName = findViewById(R.id.tilName);
        tilPhone = findViewById(R.id.tilPhone);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnModeLogin = findViewById(R.id.btnModeLogin);
        btnModeRegister = findViewById(R.id.btnModeRegister);
        btnSubmit = findViewById(R.id.btnSubmit);
        progress = findViewById(R.id.pbAuth);
        tvServer = findViewById(R.id.tvServer);
        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = findViewById(R.id.tvAuthSubtitle);

        btnModeLogin.setOnClickListener(v -> setMode(false));
        btnModeRegister.setOnClickListener(v -> setMode(true));
        btnSubmit.setOnClickListener(v -> submit());
        findViewById(R.id.btnDemo).setOnClickListener(v -> {
            setMode(false);
            etEmail.setText("blessed@atg.app");
            etPassword.setText("User@123");
        });
        tvServer.setOnClickListener(v -> editServer());
        renderServer();
        setMode(false);
    }

    private void renderServer() {
        tvServer.setText("Server: " + ApiClient.session().server());
    }

    private void setMode(boolean register) {
        registerMode = register;
        tilName.setVisibility(register ? View.VISIBLE : View.GONE);
        tilPhone.setVisibility(register ? View.VISIBLE : View.GONE);
        tvAuthTitle.setText(register ? "Join the floor." : "Welcome back.");
        tvAuthSubtitle.setText(register
                ? "Create your ATG account to track courses, calls and payment requests."
                : "Sign in to your ATG account. Your courses, calls and wallet live here.");
        btnSubmit.setText(register ? "Create account" : "Sign in");
        Ui.setChip(btnModeLogin, !register);
        Ui.setChip(btnModeRegister, register);
    }

    private void editServer() {
        EditText input = new EditText(this);
        input.setText(ApiClient.session().server());
        input.setSingleLine(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle("ATG server address")
                .setMessage("Use 10.0.2.2 for the Android emulator, or your computer's local IP when using a phone on the same network.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    ApiClient.session().setServer(input.getText().toString());
                    ApiClient.reloadServer();
                    renderServer();
                })
                .show();
    }

    private void submit() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);

        String email = text(etEmail);
        String password = text(etPassword);

        if (email.isEmpty()) {
            tilEmail.setError("Enter your email address");
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }
        String name = text(etName);
        String phone = text(etPhone);
        if (registerMode) {
            if (name.isEmpty()) {
                tilName.setError("Enter your full name");
                return;
            }
            if (phone.isEmpty()) {
                tilPhone.setError("Enter your mobile number");
                return;
            }
        }

        loading(true);
        Callback<Models.AuthRes> callback = new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.AuthRes> call, @NonNull Response<Models.AuthRes> response) {
                loading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiClient.session().saveAuth(response.body().token, response.body().user);
                    startActivity(new Intent(AuthActivity.this, MainActivity.class));
                    finishAffinity();
                } else {
                    Ui.alert(AuthActivity.this, "Could not sign you in", ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.AuthRes> call, @NonNull Throwable t) {
                loading(false);
                Ui.alert(AuthActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        };

        if (registerMode) {
            ApiClient.api().register(new Models.RegisterReq(name, email, phone, password)).enqueue(callback);
        } else {
            ApiClient.api().login(new Models.LoginReq(email, password)).enqueue(callback);
        }
    }

    private void loading(boolean active) {
        progress.setVisibility(active ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!active);
        btnSubmit.setText(active ? "Please wait" : (registerMode ? "Create account" : "Sign in"));
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
