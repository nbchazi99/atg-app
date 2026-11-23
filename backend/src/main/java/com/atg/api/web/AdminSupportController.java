package com.atg.api.web;

import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.model.SupportMessage;
import com.atg.api.model.SupportTicket;
import com.atg.api.repo.UserRepository;
import com.atg.api.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.atg.api.web.SupportDtos.*;

@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportController extends BaseController {

    private final SupportService support;

    public AdminSupportController(UserRepository users, SupportService support) {
        super(users);
        this.support = support;
    }

    @GetMapping("/tickets")
    public List<TicketView> tickets() {
        return support.adminTickets().stream().map(TicketView::of).toList();
    }

    @GetMapping("/tickets/{id}")
    public TicketDetail ticket(@PathVariable Long id) {
        SupportTicket ticket = support.adminTicket(id);
        return new TicketDetail(
                TicketView.of(ticket),
                support.adminTicketMessages(ticket).stream().map(MessageView::of).toList());
    }

    @PatchMapping("/tickets/{id}")
    public TicketView updateTicket(
            @PathVariable Long id, @Valid @RequestBody TicketStatusReq request) {
        return TicketView.of(support.updateTicket(id, request, me()));
    }

    @PostMapping("/tickets/{id}/messages")
    public MessageView reply(
            @PathVariable Long id, @Valid @RequestBody AdminReplyReq request) {
        return MessageView.of(support.addAdminMessage(id, me(), request));
    }

    @GetMapping("/conversations/{id}")
    public List<MessageView> conversation(@PathVariable Long id) {
        return support.adminConversationMessages(id).stream().map(MessageView::of).toList();
    }

    @GetMapping("/knowledge")
    public List<KnowledgeEntryView> knowledge() {
        return support.knowledgeEntries().stream().map(KnowledgeEntryView::of).toList();
    }

    @PostMapping("/knowledge")
    public KnowledgeEntryView createKnowledge(
            @Valid @RequestBody KnowledgeEntryReq request) {
        SupportKnowledgeEntry saved = support.saveKnowledgeEntry(null, request, me());
        return KnowledgeEntryView.of(saved);
    }

    @PutMapping("/knowledge/{id}")
    public KnowledgeEntryView updateKnowledge(
            @PathVariable Long id, @Valid @RequestBody KnowledgeEntryReq request) {
        SupportKnowledgeEntry saved = support.saveKnowledgeEntry(id, request, me());
        return KnowledgeEntryView.of(saved);
    }

    @DeleteMapping("/knowledge/{id}")
    public void deleteKnowledge(@PathVariable Long id) {
        support.deleteKnowledgeEntry(id);
    }
}
