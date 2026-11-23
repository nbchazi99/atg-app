package com.atg.api.web;

import com.atg.api.model.SupportConversation;
import com.atg.api.model.SupportMessage;
import com.atg.api.model.SupportTicket;
import com.atg.api.model.User;
import com.atg.api.repo.UserRepository;
import com.atg.api.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.atg.api.web.SupportDtos.*;

@RestController
@RequestMapping("/api/support")
public class SupportController extends BaseController {

    private final SupportService support;

    public SupportController(UserRepository users, SupportService support) {
        super(users);
        this.support = support;
    }

    @GetMapping("/conversations")
    public List<ConversationView> conversations() {
        return support.listConversations(me()).stream()
                .map(row -> ConversationView.of(row, null))
                .toList();
    }

    @PostMapping("/conversations")
    public ConversationView createConversation() {
        return ConversationView.of(support.createConversation(me()), null);
    }

    @GetMapping("/conversations/{id}")
    public ConversationDetail conversation(@PathVariable Long id) {
        User user = me();
        SupportConversation row = support.conversationForUser(id, user);
        return new ConversationDetail(
                ConversationView.of(row, null),
                support.conversationMessages(id, user, null, false).stream()
                        .map(MessageView::of).toList());
    }

    @PostMapping("/conversations/{id}/messages")
    public ChatResult sendMessage(
            @PathVariable Long id, @Valid @RequestBody MessageReq request) {
        SupportService.ChatTurn turn = support.sendConversationMessage(
                id, me(), null, request.text(), request.attachmentUrls());
        return chatResult(turn);
    }

    @PostMapping("/conversations/{id}/tickets")
    public TicketView createTicket(
            @PathVariable Long id, @Valid @RequestBody TicketCreateReq request) {
        return TicketView.of(support.createTicket(id, me(), null, request));
    }

    @GetMapping("/tickets")
    public List<TicketView> tickets() {
        return support.listTickets(me()).stream().map(TicketView::of).toList();
    }

    @GetMapping("/tickets/{id}")
    public TicketDetail ticket(@PathVariable Long id) {
        User user = me();
        SupportTicket ticket = support.ticketForUser(id, user, null);
        return ticketDetail(ticket, support.ticketMessages(ticket, user, null, false));
    }

    @PostMapping("/tickets/{id}/messages")
    public MessageView sendTicketMessage(
            @PathVariable Long id, @Valid @RequestBody TicketMessageReq request) {
        return MessageView.of(support.addTicketMessage(
                id, me(), null, request.text(), request.attachmentUrls()));
    }

    @PatchMapping("/tickets/{id}/resolve")
    public TicketView resolveTicket(@PathVariable Long id) {
        return TicketView.of(support.resolveTicket(id, me(), null));
    }

    @PostMapping("/guest/conversations")
    public ConversationView createGuestConversation() {
        SupportService.GuestConversation guest = support.createGuestConversation();
        return ConversationView.of(guest.conversation(), guest.token());
    }

    @PostMapping("/guest/conversations/{id}/messages")
    public ChatResult sendGuestMessage(
            @PathVariable Long id, @Valid @RequestBody MessageReq request) {
        SupportService.ChatTurn turn = support.sendConversationMessage(
                id, null, request.guestToken(), request.text(), request.attachmentUrls());
        return chatResult(turn);
    }

    @PostMapping("/guest/conversations/{id}/tickets")
    public TicketView createGuestTicket(
            @PathVariable Long id, @Valid @RequestBody TicketCreateReq request) {
        return TicketView.of(support.createTicket(id, null, request.guestToken(), request));
    }

    @PostMapping("/guest/tickets/{id}/view")
    public TicketDetail viewGuestTicket(
            @PathVariable Long id, @Valid @RequestBody GuestTicketAccessReq request) {
        SupportTicket ticket = support.ticketForUser(id, null, request.guestToken());
        return ticketDetail(ticket, support.ticketMessages(ticket, null, request.guestToken(), false));
    }

    @PostMapping("/guest/tickets/{id}/messages")
    public MessageView sendGuestTicketMessage(
            @PathVariable Long id, @Valid @RequestBody TicketMessageReq request) {
        return MessageView.of(support.addTicketMessage(
                id, null, request.guestToken(), request.text(), request.attachmentUrls()));
    }

    @PatchMapping("/guest/tickets/{id}/resolve")
    public TicketView resolveGuestTicket(
            @PathVariable Long id, @Valid @RequestBody GuestTicketAccessReq request) {
        return TicketView.of(support.resolveTicket(id, null, request.guestToken()));
    }

    private static ChatResult chatResult(SupportService.ChatTurn turn) {
        return new ChatResult(
                ConversationView.of(turn.conversation(), null),
                MessageView.of(turn.userMessage()),
                MessageView.of(turn.assistantMessage()),
                turn.needsHuman(),
                turn.ticket() == null ? null : TicketView.of(turn.ticket()));
    }

    private static TicketDetail ticketDetail(
            SupportTicket ticket, List<SupportMessage> messages) {
        return new TicketDetail(
                TicketView.of(ticket), messages.stream().map(MessageView::of).toList());
    }
}
