package com.atg.api.config;

import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.repo.SupportKnowledgeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class SupportKnowledgeSeeder implements CommandLineRunner {

    private final SupportKnowledgeRepository knowledge;

    public SupportKnowledgeSeeder(SupportKnowledgeRepository knowledge) {
        this.knowledge = knowledge;
    }

    @Override
    public void run(String... args) {
        seed("Account access", "ACCOUNT",
                "Users can reset a forgotten password from the sign-in screen using the password-reset process. "
                        + "Profile name, email, phone, and avatar can be updated in the app. Never ask a user to "
                        + "share a password, PIN, verification code, or broker credentials.",
                "login, registration, password, profile, email, phone");
        seed("Deposits", "PAYMENTS",
                "EcoCash is the supported deposit payment method. Deposits are for Weltrade accounts, and the "
                        + "minimum deposit is USD 5. Payment requests are reviewed manually by an ATG administrator. "
                        + "Never claim a deposit was received or approved unless the app shows that status.",
                "deposit, EcoCash, Weltrade, minimum, payment, pending");
        seed("Withdrawals", "PAYMENTS",
                "EcoCash is the supported withdrawal method. The minimum withdrawal is USD 5 and the current "
                        + "withdrawal fee is 10 percent. Withdrawal requests are reviewed manually by an ATG "
                        + "administrator. Never claim that money has been transferred.",
                "withdrawal, EcoCash, fee, charge, transaction, pending");
        seed("VIP membership", "VIP",
                "ATG has Deriv VIP and Weltrade VIP membership options. Membership requests require payment proof "
                        + "and are reviewed manually by an administrator. VIP rooms are private to members with the "
                        + "matching membership; support cannot grant membership or confirm payment approval.",
                "VIP, membership, Deriv, Weltrade, room, access, subscription");
        seed("Academy courses", "ACADEMY",
                "The Academy lists courses and lessons in the ATG app. Free courses can be joined directly. "
                        + "Paid course requests require payment proof and manual administrator review. Course access "
                        + "and lesson progress are attached to the signed-in account.",
                "Academy, course, lesson, video, progress, purchase, material");
        seed("Signals", "SIGNALS",
                "Signals are published in the Signals area and may be restricted to OPEN FLOOR, Deriv VIP, or "
                        + "Weltrade VIP audiences. ATG signals are educational information, not financial advice. "
                        + "The support assistant must not recommend trades or promise results.",
                "signals, buy, sell, entry, stop loss, take profit, VIP");
        seed("Support escalation", "GENERAL",
                "If the issue involves a specific account, payment, access decision, missing content, or an answer "
                        + "not covered by approved ATG information, collect the user's contact details and create a "
                        + "support ticket. Include the AI conversation so the administrator can continue without "
                        + "asking the user to repeat the issue.",
                "support, complaint, ticket, administrator, escalation");
    }

    private void seed(String title, String category, String content, String keywords) {
        if (knowledge.findByTitle(title) != null) return;
        SupportKnowledgeEntry entry = new SupportKnowledgeEntry();
        entry.setTitle(title);
        entry.setCategory(category);
        entry.setContent(content);
        entry.setKeywords(keywords);
        entry.setActive(true);
        entry.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entry.setUpdatedAt(entry.getCreatedAt());
        knowledge.save(entry);
    }
}
