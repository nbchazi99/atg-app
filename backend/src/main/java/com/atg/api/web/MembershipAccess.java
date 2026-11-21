package com.atg.api.web;

import com.atg.api.model.Room;
import com.atg.api.model.Signal;
import com.atg.api.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

public final class MembershipAccess {

    private MembershipAccess() {}

    public static String requiredForRoom(Room room) {
        return requiredForLabel(room.getName());
    }

    public static String requiredForSignal(Signal signal) {
        return requiredForLabel(signal.getRoomLabel());
    }

    public static boolean canAccess(User user, String requiredMembership) {
        if (requiredMembership == null || user.getRole() == User.Role.ADMIN) return true;
        return user.getMembership() != null
                && requiredMembership.equals(user.getMembership().name());
    }

    public static void requireAccess(User user, String requiredMembership, String resourceName) {
        if (!canAccess(user, requiredMembership)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    resourceName + " is for members. Join from your profile to unlock it.");
        }
    }

    private static String requiredForLabel(String label) {
        if (label == null) return null;
        String normalized = label.toLowerCase(Locale.ROOT);
        if (!normalized.contains("vip")) return null;
        if (normalized.contains("deriv")) return User.Membership.DERIV_VIP.name();
        if (normalized.contains("weltrade")) return User.Membership.WELTRADE_VIP.name();
        return "VIP";
    }
}
