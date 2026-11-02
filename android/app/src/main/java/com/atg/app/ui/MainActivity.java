package com.atg.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        FloatingActionButton fab = findViewById(R.id.fabChat);

        bottomNav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });
        fab.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_rooms));

        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_rooms);
        badge.setNumber(99);
        badge.setMaxCharacterCount(3);
        badge.setBackgroundColor(getColor(R.color.primary));
        badge.setBadgeTextColor(getColor(R.color.ink));

        if (savedInstanceState == null) {
            String name = ApiClient.session().name();
            Ui.toast(this, "Welcome, " + (name == null ? "trader" : name.split("\\s+")[0]));
            bottomNav.setSelectedItemId(R.id.nav_home);
            show(R.id.nav_home);
        }
    }

    public void openTab(int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    private void show(int itemId) {

        Fragment fragment;
        if (itemId == R.id.nav_signals) fragment = new SignalsFragment();
        else if (itemId == R.id.nav_markets) fragment = new MarketsFragment();
        else if (itemId == R.id.nav_rooms) fragment = new RoomsFragment();
        else if (itemId == R.id.nav_wallet) fragment = new WalletFragment();
        else fragment = new HomeFragment();

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
