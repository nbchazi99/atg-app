package com.atg.app.data;

import android.content.Context;
import android.content.SharedPreferences;

public class Session {

    private static final String PREFS = "atg_session";
    private final SharedPreferences prefs;

    public Session(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String server() {
        return prefs.getString("server", "http://10.0.2.2:8080/");
    }

    public void setServer(String value) {
        String v = value.trim();
        if (!v.startsWith("http")) v = "http://" + v;
        if (!v.endsWith("/")) v = v + "/";
        prefs.edit().putString("server", v).apply();
    }

    public String token() {
        return prefs.getString("token", null);
    }

    public boolean isLoggedIn() {
        return token() != null;
    }

    public void saveAuth(String token, Models.User user) {
        prefs.edit()
                .putString("token", token)
                .putString("name", user.name)
                .putString("email", user.email)
                .putString("phone", user.phone)
                .putString("role", user.role)
                .putString("membership", user.membership)
                .putInt("points", user.points)
                .apply();
    }

    public void saveUser(Models.User user) {
        prefs.edit()
                .putString("name", user.name)
                .putString("email", user.email)
                .putString("phone", user.phone)
                .putString("role", user.role)
                .putString("membership", user.membership)
                .putInt("points", user.points)
                .apply();
    }

    public void savePoints(int points) {
        prefs.edit().putInt("points", points).apply();
    }

    public void logout() {
        prefs.edit().remove("token").apply();
    }

    public String name() {
        return prefs.getString("name", "Trader");
    }

    public String email() {
        return prefs.getString("email", "");
    }

    public String phone() {
        return prefs.getString("phone", "");
    }

    public String role() {
        return prefs.getString("role", "USER");
    }

    public String membership() {
        return prefs.getString("membership", "NONE");
    }

    public int points() {
        return prefs.getInt("points", 0);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public String initials() {
        String n = name().trim();
        if (n.isEmpty()) return "A";
        String[] parts = n.split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    public String membershipLabel() {
        switch (membership()) {
            case "DERIV_VIP": return "Deriv VIP";
            case "WELTRADE_VIP": return "Weltrade VIP";
            default: return "No membership";
        }
    }
}
