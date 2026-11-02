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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletFragment extends Fragment {

    private TxAdapter adapter;
    private SwipeRefreshLayout swipe;
    private View empty;
    private TextView pending, membership;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView title = view.findViewById(R.id.tvAppTitle);
        title.setText("Wallet");
        TextView avatar = view.findViewById(R.id.tvAvatar);
        avatar.setText(ApiClient.session().initials());
        avatar.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProfileActivity.class)));
        view.findViewById(R.id.btnBell).setOnClickListener(v ->
                Ui.alert(requireContext(), "Notifications", "You are all caught up."));

        pending = view.findViewById(R.id.tvWalletPending);
        membership = view.findViewById(R.id.tvWalletMembership);

        view.findViewById(R.id.btnWalletDeposit).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DepositActivity.class)));
        view.findViewById(R.id.btnWalletWithdraw).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), WithdrawActivity.class)));
        view.findViewById(R.id.btnWalletMembership).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));

        RecyclerView list = view.findViewById(R.id.rvTx);
        adapter = new TxAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        empty = view.findViewById(R.id.walletEmpty);
        swipe = view.findViewById(R.id.swipeWallet);
        swipe.setColorSchemeResources(R.color.primary);
        swipe.setOnRefreshListener(this::load);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        membership.setText(ApiClient.session().membershipLabel());
        ApiClient.api().wallet().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.WalletRes> call, @NonNull Response<Models.WalletRes> response) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Models.WalletRes wallet = response.body();
                if (!response.isSuccessful() || wallet == null) return;
                adapter.setItems(wallet.transactions);
                empty.setVisibility(wallet.transactions == null || wallet.transactions.isEmpty()
                        ? View.VISIBLE : View.GONE);
                long awaiting = wallet.pendingDeposits + wallet.pendingWithdrawals;
                if (awaiting > 0) {
                    pending.setVisibility(View.VISIBLE);
                    pending.setText(awaiting + " request" + (awaiting == 1 ? "" : "s") + " awaiting review");
                } else {
                    pending.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.WalletRes> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                swipe.setRefreshing(false);
                Ui.toast(requireContext(), ApiClient.connectionMessage(t));
            }
        });
    }
}
