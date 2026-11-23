package com.atg.api.service;

import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.repo.SupportKnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SupportKnowledgeService {

    private static final int MAX_CONTEXT_ENTRIES = 8;
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "again", "also", "and", "are", "can", "could",
            "does", "for", "from", "have", "help", "how", "into", "issue",
            "need", "please", "that", "the", "this", "what", "when", "where",
            "with", "would", "you", "your");

    private final SupportKnowledgeRepository knowledge;

    public SupportKnowledgeService(SupportKnowledgeRepository knowledge) {
        this.knowledge = knowledge;
    }

    public List<SupportKnowledgeEntry> findRelevant(String question) {
        List<String> terms = terms(question);
        return knowledge.findActiveOrderByTitle().stream()
                .map(entry -> new ScoredEntry(entry, score(entry, terms)))
                .filter(row -> row.score() > 0)
                .sorted(Comparator.comparingInt(ScoredEntry::score).reversed())
                .limit(MAX_CONTEXT_ENTRIES)
                .map(ScoredEntry::entry)
                .toList();
    }

    private static int score(SupportKnowledgeEntry entry, List<String> terms) {
        String title = normalize(entry.getTitle());
        String category = normalize(entry.getCategory());
        String keywords = normalize(entry.getKeywords());
        String content = normalize(entry.getContent());
        int score = 0;
        for (String term : terms) {
            if (title.contains(term)) score += 4;
            if (category.contains(term)) score += 3;
            if (keywords.contains(term)) score += 3;
            if (content.contains(term)) score++;
        }
        return score;
    }

    private static List<String> terms(String text) {
        return List.of(normalize(text).split("\\s+")).stream()
                .filter(term -> term.length() > 2 && !STOP_WORDS.contains(term))
                .distinct()
                .toList();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private record ScoredEntry(SupportKnowledgeEntry entry, int score) {}
}
