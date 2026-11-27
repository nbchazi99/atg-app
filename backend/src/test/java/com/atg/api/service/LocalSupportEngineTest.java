package com.atg.api.service;

import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.model.SupportMessage;
import com.atg.api.repo.SupportKnowledgeRepository;
import com.atg.api.web.SupportDtos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalSupportEngineTest {

    @Test
    void resolvesAVerifiedPasswordSolutionWithoutHumanEscalation() {
        LocalSupportEngine engine = engineWith(entry(
                "Account access", "ACCOUNT",
                "Use the password-reset process from the sign-in screen.",
                "login, password, profile, email"));

        SupportDtos.AiReply reply = engine.answer(List.of(userMessage("I forgot my password")));

        assertFalse(reply.needsHuman());
        assertEquals("ACCOUNT", reply.category());
        assertTrue(reply.message().contains("password-reset"));
        assertTrue(reply.message().contains("Try these steps"));
    }

    @Test
    void escalatesLowConfidenceAndAccountSpecificIssues() {
        LocalSupportEngine engine = engineWith(entry(
                "Deposits", "PAYMENTS",
                "EcoCash deposits are reviewed manually by an administrator.",
                "deposit, EcoCash, payment"));

        SupportDtos.AiReply low = engine.answer(List.of(userMessage("The blue control panel is blinking")));
        SupportDtos.AiReply account = engine.answer(List.of(
                userMessage("I made a deposit"),
                aiMessage("Please check your payment history."),
                userMessage("My deposit is missing")));

        assertTrue(low.needsHuman());
        assertTrue(account.needsHuman());
        assertTrue(account.message().contains("account-specific"));
        assertEquals("HIGH", account.priority());
    }

    @Test
    void usesRecentConversationContext() {
        LocalSupportEngine engine = engineWith(entry(
                "Withdrawals", "PAYMENTS",
                "EcoCash withdrawals require manual administrator review.",
                "withdrawal, EcoCash, fee"));

        SupportDtos.AiReply reply = engine.answer(List.of(
                userMessage("I have a money problem"),
                aiMessage("Which type of transaction do you mean?"),
                userMessage("I need the withdrawal fee")));

        assertEquals("PAYMENTS", reply.category());
        assertTrue(reply.message().contains("withdrawals"));
    }

    @Test
    void matchesCommaSeparatedPhrasesAndDoesNotMatchSubstrings() {
        SupportKnowledgeEntry cashEntry = entry(
                "Payment help", "PAYMENTS",
                "Cash withdrawals are reviewed by support.",
                "cash withdrawal, transfer status");
        cashEntry.setId(7L);
        SupportKnowledgeRepository repository = mock(SupportKnowledgeRepository.class);
        when(repository.findActiveOrderByTitle()).thenReturn(List.of(cashEntry));
        SupportKnowledgeService knowledge = new SupportKnowledgeService(repository);

        assertEquals(cashEntry, knowledge.findMatches("cash withdrawal").get(0).entry());
        assertTrue(knowledge.findMatches("cash withdrawal").get(0).score() > 0.5);
        assertTrue(knowledge.findMatches("fee").isEmpty());
        assertTrue(knowledge.findMatches("the and").isEmpty());
    }

    @Test
    void breaksEqualScoresByIdAndIgnoresAssistantAndAdminText() {
        SupportKnowledgeEntry second = entry(
                "Cash help", "PAYMENTS", "Cash withdrawal guide.", "cash withdrawal");
        second.setId(2L);
        SupportKnowledgeEntry first = entry(
                "Cash help", "PAYMENTS", "Cash withdrawal guide.", "cash withdrawal");
        first.setId(1L);
        SupportKnowledgeRepository repository = mock(SupportKnowledgeRepository.class);
        when(repository.findActiveOrderByTitle()).thenReturn(List.of(second, first));
        List<SupportKnowledgeService.Match> matches =
                new SupportKnowledgeService(repository).findMatches("cash withdrawal");
        assertEquals(1L, matches.get(0).entry().getId());
        assertEquals(matches.get(0).score(), matches.get(1).score());

        SupportKnowledgeEntry accountEntry = entry(
                "Account access", "ACCOUNT",
                "Use the password reset process to sign in.",
                "password, account access");
        SupportKnowledgeEntry withdrawalEntry = entry(
                "Withdrawals", "PAYMENTS",
                "EcoCash withdrawals require administrator review.",
                "withdrawal fee, EcoCash, transfer");
        LocalSupportEngine engine = engineWith(accountEntry, withdrawalEntry);
        SupportDtos.AiReply reply = engine.answer(List.of(
                userMessage("I have a question"),
                aiMessage("Password account login reset guide"),
                adminMessage("Use password access settings"),
                userMessage("What is the withdrawal fee?")));

        assertEquals("PAYMENTS", reply.category());
        assertTrue(reply.message().contains("Withdrawals"));
        assertFalse(reply.message().contains("\n\nAccount access\n"));
    }

    private static LocalSupportEngine engineWith(SupportKnowledgeEntry... entries) {
        SupportKnowledgeRepository repository = mock(SupportKnowledgeRepository.class);
        when(repository.findActiveOrderByTitle()).thenReturn(List.of(entries));
        return new LocalSupportEngine(new SupportKnowledgeService(repository));
    }

    private static SupportKnowledgeEntry entry(
            String title, String category, String content, String keywords) {
        SupportKnowledgeEntry entry = new SupportKnowledgeEntry();
        entry.setTitle(title);
        entry.setCategory(category);
        entry.setContent(content);
        entry.setKeywords(keywords);
        entry.setActive(true);
        return entry;
    }

    private static SupportMessage userMessage(String text) {
        SupportMessage message = new SupportMessage();
        message.setAuthorType(SupportMessage.AuthorType.USER);
        message.setText(text);
        return message;
    }

    private static SupportMessage aiMessage(String text) {
        SupportMessage message = new SupportMessage();
        message.setAuthorType(SupportMessage.AuthorType.AI);
        message.setText(text);
        return message;
    }

    private static SupportMessage adminMessage(String text) {
        SupportMessage message = new SupportMessage();
        message.setAuthorType(SupportMessage.AuthorType.ADMIN);
        message.setText(text);
        return message;
    }
}
