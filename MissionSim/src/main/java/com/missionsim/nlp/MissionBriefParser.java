package com.missionsim.nlp;

import java.util.*;
import java.util.regex.*;

/**
 * Keyword-based NLP parser that extracts structured directives from a free-text mission briefing.
 *
 * Recognises patterns like:
 *   "Scout must reach checkpoint Alpha"
 *   "Engineer should clear debris blocking the northern corridor"
 *   "Medic should treat wounded and assist the team"
 */
public class MissionBriefParser {

    // A record is a compact container for related values — like a tiny data class.
    // The compiler generates the constructor, getters, equals, and hashCode automatically.
    public record Directive(String role, String verb, String target) {
        @Override
        public String toString() {
            // Left-aligned columns so all directives print in a tidy table
            return String.format("%-10s %-12s %s", role.toUpperCase(), verb, target);
        }
    }

    // The roles and verbs we look for. Order matters for VERBS — the first match wins,
    // so put the most specific/important verbs earlier in the list if needed.
    private static final List<String> ROLES   = List.of("scout", "medic", "engineer");
    private static final List<String> VERBS   = List.of(
        "clear", "reach", "secure", "treat", "assist", "defend", "rescue",
        "destroy", "patrol", "report", "extract", "cover"
    );

    // Splits on sentence-ending punctuation OR the word "and" so compound sentences
    // like "Scout must reach Alpha and secure the perimeter" become two directives
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[.!?;]|\\band\\b", Pattern.CASE_INSENSITIVE);

    public List<Directive> parse(String briefing) {
        List<Directive> directives = new ArrayList<>();
        String[] sentences = SENTENCE_SPLIT.split(briefing);

        for (String sentence : sentences) {
            sentence = sentence.strip();
            if (sentence.isEmpty()) continue;
            String lower = sentence.toLowerCase();

            // Check each sentence for a known role and verb — fall back to "TEAM" / "execute" if not found
            String role = ROLES.stream().filter(lower::contains).findFirst().orElse("TEAM");
            String verb = VERBS.stream().filter(lower::contains).findFirst().orElse("execute");
            String target = extractTarget(sentence, verb);

            directives.add(new Directive(role, verb, target));
        }

        return directives;
    }

    /**
     * Extracts the noun phrase following the detected verb.
     * Falls back to the trimmed remainder of the sentence.
     */
    private String extractTarget(String sentence, String verb) {
        int idx = sentence.toLowerCase().indexOf(verb);
        if (idx == -1) return sentence.trim();
        // grab everything after the verb
        String remainder = sentence.substring(idx + verb.length()).trim();
        // strip leading filler words like "the", "a", "to", "towards", etc.
        remainder = remainder.replaceFirst("^(the|a|an|to|towards?|into|at|by)\\s+", "");
        // cut off at the first clause boundary (comma, semicolon, or conjunctions like "and"/"but")
        // so "clear debris and then secure the objective" just gives us "debris"
        Matcher m = Pattern.compile("[,;]|\\b(and|but|while|when|if)\\b").matcher(remainder);
        if (m.find()) remainder = remainder.substring(0, m.start()).trim();
        return remainder.isEmpty() ? "(unspecified)" : remainder;
    }

    // Prints all directives in a formatted table
    public void printDirectives(List<Directive> directives) {
        System.out.println("\n--- Parsed Mission Directives ---");
        System.out.printf("%-10s %-12s %s%n", "ROLE", "VERB", "TARGET");
        System.out.println("-".repeat(50));
        directives.forEach(d -> System.out.println(d));
        System.out.println();
    }
}
