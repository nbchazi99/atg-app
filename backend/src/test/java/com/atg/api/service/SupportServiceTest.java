package com.atg.api.service;

import com.atg.api.model.SupportConversation;
import com.atg.api.model.SupportMessage;
import com.atg.api.model.SupportTicket;
import com.atg.api.model.User;
import com.atg.api.realtime.RealtimeHub;
import com.atg.api.repo.PushTokenRepository;
import com.atg.api.repo.SupportConversationRepository;
import com.atg.api.repo.SupportKnowledgeRepository;
import com.atg.api.repo.SupportMessageRepository;
import com.atg.api.repo.SupportTicketRepository;
import com.atg.api.repo.UserRepository;
import com.atg.api.web.SupportDtos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportServiceTest {

    @Test
    void administratorsCanPublishAndDisableVerifiedKnowledge() {
        SupportKnowledgeRepository knowledge = mock(SupportKnowledgeRepository.class);
        when(knowledge.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SupportService service = new SupportService(
                mock(SupportConversationRepository.class),
                mock(SupportTicketRepository.class),
                mock(SupportMessageRepository.class),
                knowledge,
                mock(UserRepository.class),
                new LocalSupportEngine(new SupportKnowledgeService(knowledge)),
                mock(RealtimeHub.class),
                mock(PushTokenRepository.class),
                mock(PushService.class),
                "test-bucket");

        User admin = new User();
        admin.setId(99L);
        SupportDtos.KnowledgeEntryReq approved = new SupportDtos.KnowledgeEntryReq(
                "Withdrawal fee", "PAYMENTS", "The current withdrawal fee is 10%.",
                "withdrawal fee, EcoCash", true);
        var saved = service.saveKnowledgeEntry(null, approved, admin);

        assertTrue(saved.isActive());
        assertEquals(99L, saved.getUpdatedBy());
        assertEquals("withdrawal fee, EcoCash", saved.getKeywords());

        saved.setId(5L);
        when(knowledge.findById(5L)).thenReturn(Optional.of(saved));
        SupportDtos.KnowledgeEntryReq disabled = new SupportDtos.KnowledgeEntryReq(
                approved.title(), approved.category(), approved.content(),
                approved.keywords(), false);
        var updated = service.saveKnowledgeEntry(5L, disabled, admin);
        assertFalse(updated.isActive());
        assertEquals(99L, updated.getUpdatedBy());
    }

    @Test
    void forwardsLowConfidenceConversationToAdminTicketInbox() {
        SupportConversationRepository conversations = mock(SupportConversationRepository.class);
        SupportTicketRepository tickets = mock(SupportTicketRepository.class);
        SupportMessageRepository messages = mock(SupportMessageRepository.class);
        SupportKnowledgeRepository knowledge = mock(SupportKnowledgeRepository.class);
        UserRepository users = mock(UserRepository.class);
        RealtimeHub realtime = mock(RealtimeHub.class);
        PushTokenRepository pushTokens = mock(PushTokenRepository.class);
        PushService push = mock(PushService.class);

        SupportConversation conversation = new SupportConversation();
        conversation.setId(42L);
        conversation.setUserId(7L);
        List<SupportMessage> persistedMessages = new ArrayList<>();
        when(conversations.findById(42L)).thenReturn(Optional.of(conversation));
        when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.findByConversationIdOrderByCreatedAtAsc(42L))
                .thenAnswer(invocation -> new ArrayList<>(persistedMessages));
        when(messages.save(any())).thenAnswer(invocation -> {
            SupportMessage saved = invocation.getArgument(0);
            persistedMessages.add(saved);
            return saved;
        });
        when(tickets.findByConversationId(42L)).thenReturn(null);
        when(tickets.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledge.findActiveOrderByTitle()).thenReturn(List.of());

        User user = new User();
        user.setId(7L);
        user.setName("Test member");
        user.setEmail("member@example.test");
        user.setPhone("+263770000000");
        LocalSupportEngine engine = new LocalSupportEngine(new SupportKnowledgeService(knowledge));
        SupportService service = new SupportService(
                conversations, tickets, messages, knowledge, users, engine, realtime,
                pushTokens, push, "test-bucket");

        SupportService.ChatTurn turn = service.sendConversationMessage(
                42L, user, null, "My unusual blue issue is broken", List.of());

        assertTrue(turn.needsHuman());
        assertNotNull(turn.ticket());
        assertEquals(42L, turn.ticket().getConversationId());
        assertEquals(2, conversation.getMessageCount());
        assertEquals(SupportConversation.Status.ESCALATED, conversation.getStatus());

        List<SupportMessage> adminView = service.adminTicketMessages(turn.ticket());
        assertEquals(2, adminView.size());
        assertTrue(adminView.stream().allMatch(message -> message.getConversationId().equals(42L)));
        assertEquals(SupportMessage.AuthorType.USER, adminView.get(0).getAuthorType());
        assertEquals(SupportMessage.AuthorType.AI, adminView.get(1).getAuthorType());
        verify(tickets, times(1)).save(any(SupportTicket.class));
        verify(realtime, times(4)).publish(any(String.class), any());
    }
}
