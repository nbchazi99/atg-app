package com.atg.api.service;

import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.model.SupportMessage;
import com.atg.api.web.SupportDtos;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class LocalSupportEngine {

    private static final double SOLUTION_THRESHOLD = 0.55;
    private static final double UNCERTAIN_THRESHOLD = 0.32;

    private final SupportKnowledgeService knowledge;

    public LocalSupportEngine(SupportKnowledgeService knowledge) {
        this.knowledge = knowledge;
    }

    public SupportDtos.AiReply answer(List<SupportMessage> history) {
        ConversationContext conversation = conversationContext(history);
        SupportKnowledgeService.Match latestMatch = knowledge.findMatches(conversation.latest())
                .stream().findFirst().orElse(null);
        SupportKnowledgeService.Match best = latestMatch;
        if (latestMatch == null || latestMatch.score() < UNCERTAIN_THRESHOLD) {
            SupportKnowledgeService.Match contextualMatch = knowledge.findMatches(conversation.text())
                    .stream().findFirst().orElse(null);
            if (contextualMatch != null
                    && (best == null || contextualMatch.score() > best.score())) {
                best = contextualMatch;
            }
        }

        String normalized = normalize(conversation.text());
        String category = best == null
                ? inferCategory(normalized)
                : normalizeCategory(best.entry().getCategory());
        String priority = inferPriority(normalized);
        boolean explicitHuman = containsAny(normalized,
                "talk to human", "speak to human", "contact admin", "admin please",
                "support ticket", "real person", "human support");
        boolean accountSpecific = containsAny(normalized,
                "my account", "my payment", "my deposit", "my withdrawal", "missing",
                "not approved", "approval", "charged twice", "wrong amount",
                "my course", "my vip", "lost access", "not received");

        if (best == null || best.score() < UNCERTAIN_THRESHOLD) {
            return new SupportDtos.AiReply(
                    "I could not confidently match this issue to a verified ATG solution. "
                            + "I am forwarding the full conversation to an administrator.",
                    true, category, priority,
                    best == null ? 0.0 : round(best.score()));
        }

        boolean needsHuman = explicitHuman || accountSpecific
                || best.score() < SOLUTION_THRESHOLD;
        StringBuilder message = new StringBuilder();
        if (needsHuman) {
            message.append("I found related information, but this may need account-specific review.");
        } else {
            message.append("I found a verified troubleshooting guide.");
        }
        message.append("\n\n").append(best.entry().getTitle()).append("\n")
                .append(best.entry().getContent().trim());
        message.append("\n\nTry these steps:\n")
                .append(troubleshootingSteps(category));

        if (needsHuman) {
            message.append("\n\nIf these steps do not resolve it, respond here; "
                    + "I will keep the conversation and escalate it to the support team.");
        } else {
            message.append("\n\nReply with the result of step 1, and I will continue troubleshooting.");
        }

        return new SupportDtos.AiReply(
                message.toString(), needsHuman, category, priority, round(best.score()));
    }

    private static ConversationContext conversationContext(List<SupportMessage> history) {
        if (history == null || history.isEmpty()) return new ConversationContext("", "");
        List<String> userMessages = history.stream()
                .filter(message -> message != null
                        && !message.isInternalNote()
                        && message.getAuthorType() == SupportMessage.AuthorType.USER)
                .map(SupportMessage::getText)
                .filter(text -> text != null && !text.isBlank())
                .toList();
        int from = Math.max(0, userMessages.size() - 12);
        List<String> recent = userMessages.subList(from, userMessages.size());
        return new ConversationContext(
                userMessages.isEmpty() ? "" : userMessages.get(userMessages.size() - 1),
                String.join(" ", recent));
    }

    private static String troubleshootingSteps(String category) {
        return switch (category) {
            case "PAYMENTS" -> "1. Check the transaction status in your ATG payment history.\n"
                    + "2. Confirm the exact amount and EcoCash number used.\n"
                    + "3. Do not resend a duplicate payment while the existing request is pending.";
            case "ACCOUNT" -> "1. Confirm your registered email and internet connection.\n"
                    + "2. Use the password-reset option from the sign-in screen.\n"
                    + "3. Restart the app and sign in again; never share your password or security codes.";
            case "VIP" -> "1. Confirm the correct ATG membership name and course of purchase.\n"
                    + "2. Verify that the app shows the payment request status.\n"
                    + "3. Check whether your VIP room appears after the administrator approves the request.";
            case "ACADEMY" -> "1. Open the Academy and refresh the course list.\n"
                    + "2. Confirm that you are signed into the account used for the purchase.\n"
                    + "3. Reopen the course and verify lesson progress.";
            case "SIGNALS" -> "1. Confirm that the signal belongs to your current membership audience.\n"
                    + "2. Refresh the Signals area and reopen the signal.\n"
                    + "3. Re-check the exact entry and time shown in the app.";
            case "TECHNICAL" -> "1. Update the app and restart the device.\n"
                    + "2. Check the internet connection and try the action again.\n"
                    + "3. Note the exact screen, error text, and time it occurred.";
            default -> "1. Recheck the details and repeat the action once.\n"
                    + "2. Restart the app and confirm you are signed in.\n"
                    + "3. Save the exact error or result before requesting further help.";
        };
    }

    private static String inferCategory(String text) {
        if (containsAny(text, "deposit", "withdraw", "payment", "ecocash", "charged", "fee"))
            return "PAYMENTS";
        if (containsAny(text, "vip", "weltrade", "deriv", "membership"))
            return "VIP";
        if (containsAny(text, "course", "academy", "lesson", "video"))
            return "ACADEMY";
        if (containsAny(text, "signal", "trade", "entry", "take profit", "stop loss"))
            return "SIGNALS";
        if (containsAny(text, "login", "password", "email", "phone", "profile", "account"))
            return "ACCOUNT";
        if (containsAny(text, "error", "crash", "bug", "app", "loading", "network"))
            return "TECHNICAL";
        return "GENERAL";
    }

    private static String inferPriority(String text) {
        if (containsAny(text, "charged twice", "hacked", "security", "urgent",
                "cannot withdraw", "cannot access", "lost access", "missing payment"))
            return "URGENT";
        if (containsAny(text, "payment", "deposit", "withdrawal", "vip",
                "cannot login", "password", "locked", "error"))
            return "HIGH";
        return "NORMAL";
    }

    private static String normalizeCategory(String category) {
        String value = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        return List.of("ACCOUNT", "PAYMENTS", "VIP", "ACADEMY", "SIGNALS", "TECHNICAL", "GENERAL")
                .contains(value) ? value : "GENERAL";
    }

    private static boolean containsAny(String text, String... phrases) {
        String searchable = " " + normalize(text) + " ";
        for (String phrase : phrases) {
            String normalizedPhrase = normalize(phrase);
            if (!normalizedPhrase.isBlank()
                    && searchable.contains(" " + normalizedPhrase + " ")) return true;
        }
        return false;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private record ConversationContext(String latest, String text) {}
}
