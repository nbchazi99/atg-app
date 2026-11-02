package com.atg.app.ui;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import com.atg.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class Ui {

    private Ui() {}

    public static String money(double value) {
        return "$" + String.format(Locale.US, "%,.2f", value);
    }

    public static String price(double value) {
        String pattern;
        if (Math.abs(value) < 10) pattern = "#,##0.00000";
        else if (Math.abs(value) < 1000) pattern = "#,##0.000";
        else pattern = "#,##0.00";
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

    public static String pct(double value) {
        return String.format(Locale.US, "%+.2f%%", value);
    }

    public static double parseAmount(String raw, double fallback) {
        if (raw == null) return fallback;
        String cleaned = raw.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return fallback;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String timeAgo(String iso) {
        if (iso == null || iso.isBlank()) return "";
        LocalDateTime time;
        try {
            time = LocalDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            try {
                time = OffsetDateTime.parse(iso).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                return "";
            }
        }
        long minutes = Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days == 1) return "1 d ago";
        return days + " d ago";
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void alert(Context context, String title, String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    public static void setChip(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    button.getContext().getColor(R.color.primary_soft)));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    button.getContext().getColor(R.color.gold)));
            button.setTextColor(button.getContext().getColor(R.color.gold));
        } else {
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    button.getContext().getColor(R.color.surface)));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    button.getContext().getColor(R.color.stroke)));
            button.setTextColor(button.getContext().getColor(R.color.ink_soft));
        }
    }

    public static void setStatusPill(TextView view, String status) {
        int bg, fg;
        switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
            case "APPROVED":
            case "COMPLETED":
            case "LIVE":
                bg = R.drawable.bg_chip_green;
                fg = R.color.green_text;
                break;
            case "REJECTED":
            case "SELL":
                bg = R.drawable.bg_chip_red;
                fg = R.color.red_text;
                break;
            case "PENDING":
            case "BUY":
                bg = R.drawable.bg_chip_amber;
                fg = R.color.gold;
                break;
            default:
                bg = R.drawable.bg_chip_grey;
                fg = R.color.muted;
        }
        view.setBackgroundResource(bg);
        view.setTextColor(view.getContext().getColor(fg));
        view.setText(status);
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "A";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    public static String membershipLabel(String membership) {
        if ("DERIV_VIP".equals(membership)) return "Deriv VIP";
        if ("WELTRADE_VIP".equals(membership)) return "Weltrade VIP";
        return "No membership";
    }
}
