package com.atg.api.service;

import com.atg.api.model.SupportKnowledgeEntry;
import com.atg.api.repo.SupportKnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SupportKnowledgeService {

    private static final int MAX_CONTEXT_ENTRIES = 8;
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "after", "again", "also", "an", "and", "are", "as",
            "at", "be", "because", "but", "by", "can", "could", "did", "do",
            "does", "for", "from", "have", "help", "how", "i", "if", "in",
            "into", "is", "it", "me", "my", "need", "of", "on", "or", "our",
            "please", "the", "that", "this", "to", "we", "what", "when", "where",
            "which", "with", "would", "you", "your");

    private final SupportKnowledgeRepository knowledge;

    public SupportKnowledgeService(SupportKnowledgeRepository knowledge) {
        this.knowledge = knowledge;
    }

    public List<SupportKnowledgeEntry> findRelevant(String question) {
        return findMatches(question).stream()
                .map(Match::entry)
                .toList();
    }

    public List<Match> findMatches(String question) {
        if (question == null || question.isBlank()) return List.of();
        List<String> terms = terms(question);
        if (terms.isEmpty()) return List.of();
        return knowledge.findActiveOrderByTitle().stream()
                .map(entry -> new Match(entry, score(entry, question, terms)))
                .filter(row -> row.score() > 0)
                .sorted(Comparator.comparingDouble(Match::score).reversed()
                        .thenComparing(row -> safe(row.entry().getTitle()),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(row -> row.entry().getId(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_CONTEXT_ENTRIES)
                .toList();
    }

    private static double score(SupportKnowledgeEntry entry, String question, List<String> terms) {
        String query = normalize(question);
        String title = normalize(entry.getTitle());
        String category = normalize(entry.getCategory());
        String content = normalize(entry.getContent());
        List<String> keywordPhrases = keywordPhrases(entry.getKeywords());
        Set<String> titleTerms = new HashSet<>(terms(title));
        Set<String> categoryTerms = new HashSet<>(terms(category));
        Set<String> keywordTerms = new HashSet<>();
        keywordPhrases.forEach(phrase -> keywordTerms.addAll(terms(phrase)));
        Set<String> contentTerms = new HashSet<>(terms(content));
        Set<String> searchableTerms = new HashSet<>();
        searchableTerms.addAll(titleTerms);
        searchableTerms.addAll(categoryTerms);
        searchableTerms.addAll(keywordTerms);
        searchableTerms.addAll(contentTerms);
        String searchable = String.join(" ", title, category,
                String.join(" ", keywordPhrases), content).trim();
        List<String> searchableWords = terms(searchable);
        double score = 0;

        for (String term : terms) {
            if (titleTerms.contains(term)) score += 0.18;
            if (categoryTerms.contains(term)) score += 0.08;
            if (keywordTerms.contains(term)) score += 0.24;
            if (contentTerms.contains(term)) score += 0.04;
        }

        if (!title.isBlank() && !query.isBlank()
                && (containsPhrase(query, title) || containsPhrase(title, query))) {
            score += 0.20;
        }
        for (String phrase : keywordPhrases) {
            if (phrase.length() >= 4 && containsPhrase(query, phrase)) {
                score += 0.12;
            }
        }

        score += cosineSimilarity(terms, searchableWords) * 0.45;
        score += bigramSimilarity(terms, searchableWords) * 0.25;
        if (!terms.isEmpty()) {
            long covered = terms.stream().filter(searchableTerms::contains).count();
            score += ((double) covered / terms.size()) * 0.15;
        }
        return Math.min(1.0, score);
    }

    private static List<String> terms(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) return List.of();
        return List.of(normalized.split("\\s+")).stream()
                .filter(term -> term.length() > 1 && !STOP_WORDS.contains(term))
                .distinct()
                .toList();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static double cosineSimilarity(List<String> left, List<String> right) {
        Map<String, Integer> a = termFrequency(left);
        Map<String, Integer> b = termFrequency(right);
        Set<String> all = new HashSet<>();
        all.addAll(a.keySet());
        all.addAll(b.keySet());
        double dot = 0;
        double leftLength = 0;
        double rightLength = 0;
        for (String term : all.stream().sorted().toList()) {
            int x = a.getOrDefault(term, 0);
            int y = b.getOrDefault(term, 0);
            dot += x * y;
            leftLength += x * x;
            rightLength += y * y;
        }
        if (leftLength == 0 || rightLength == 0) return 0;
        return dot / (Math.sqrt(leftLength) * Math.sqrt(rightLength));
    }

    private static Map<String, Integer> termFrequency(List<String> terms) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        for (String term : terms) {
            counts.merge(term, 1, Integer::sum);
        }
        return counts;
    }

    private static double bigramSimilarity(List<String> left, List<String> right) {
        Set<String> a = bigrams(left);
        Set<String> b = bigrams(right);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> bigrams(List<String> words) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 1 < words.size(); i++) {
            result.add(words.get(i) + " " + words.get(i + 1));
        }
        return result;
    }

    private static List<String> keywordPhrases(String keywords) {
        if (keywords == null || keywords.isBlank()) return List.of();
        return java.util.Arrays.stream(keywords.split("[,;\\n|]+"))
                .map(SupportKnowledgeService::normalize)
                .filter(phrase -> !phrase.isBlank())
                .distinct()
                .toList();
    }

    private static boolean containsPhrase(String text, String phrase) {
        if (text.isBlank() || phrase.isBlank()) return false;
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record Match(SupportKnowledgeEntry entry, double score) {}
}
