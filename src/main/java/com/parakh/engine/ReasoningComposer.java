package com.parakh.engine;

import com.parakh.model.Candidate;
import com.parakh.scoring.ScoreBreakdown;
import com.parakh.scoring.ScoreComponent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the reasoning text for each candidate row in the output.
 *
 * It only uses the evidence phrases the scorers already produced, so it can't make up facts.
 * The opening verdict comes from the candidate's final score, so a strong pick reads
 * confidently and a weaker one reads more critically. The sentence shape is picked from the
 * candidate id so neighbouring rows don't all look identical, but the run stays reproducible.
 */
@Service
public class ReasoningComposer {

    public String compose(Candidate c, ScoreBreakdown b) {
        Candidate.Profile p = c.profile;
        String title = p == null ? "Candidate" : safe(p.title());
        String company = p == null ? "" : safe(p.company());
        double yoe = p == null ? 0 : p.years_of_experience;
        double finalScore = b.finalScore();

        // pick one of three sentence shapes from the id (String.hashCode is stable across JVMs)
        int variant = Math.floorMod(c.candidate_id == null ? 0 : c.candidate_id.hashCode(), 3);

        // headline: title @ company, years of experience
        String head;
        String co = company.isEmpty() ? "" : " @ " + company;
        switch (variant) {
            case 0 -> head = title + co + String.format(", %.1fy.", yoe);
            case 1 -> head = String.format("%.1fy %s", yoe, title) + (company.isEmpty() ? "." : " at " + company + ".");
            default -> head = title + (company.isEmpty() ? "" : " (" + company + ")") + String.format(", %.1fy experience.", yoe);
        }

        // sort each component into a strength or a concern based on its raw score
        List<String> strengths = new ArrayList<>();
        List<String> concerns = new ArrayList<>();

        for (ScoreComponent comp : b.additiveComponents()) {
            String ev = comp.evidence();
            if (ev == null || ev.isBlank()) continue;
            switch (comp.name()) {
                case "CareerRelevance" -> {
                    if (comp.raw() >= 0.55) strengths.add(ev);
                    else concerns.add("career signal only adjacent to core ranking work (" + ev + ")");
                }
                case "SkillTrust" -> {
                    if (comp.raw() >= 0.55) strengths.add(ev);
                    else if (comp.raw() < 0.40) concerns.add("thin verified skill overlap (" + ev + ")");
                }
                case "DomainProduct" -> {
                    if (comp.raw() >= 0.60) strengths.add(ev);
                    else concerns.add("limited product-company / NLP-IR depth (" + ev + ")");
                }
                case "ExperienceFit" -> {
                    if (comp.raw() < 0.70) concerns.add("experience outside the ideal 6-8y band (" + ev + ")");
                }
                case "Location" -> {
                    if (comp.raw() < 0.60) concerns.add("location needs relocation (" + ev + ")");
                }
                default -> { /* EvalSignal is left out of the text to keep it short */ }
            }
        }
        for (ScoreComponent m : b.multiplierComponents()) {
            if (m.name().equals("Availability")) {
                if (m.raw() >= 0.90) strengths.add("strong engagement (" + m.evidence() + ")");
                else if (m.raw() < 0.80) concerns.add("availability is a question (" + m.evidence() + ")");
            } else if (m.name().equals("Disqualifier") && m.raw() < 0.95) {
                concerns.add(m.evidence());
            }
        }

        // verdict word picked from the final-score band so the tone matches the rank.
        // the whole top-100 lands around 0.58-0.87, and these are the top 0.1% of 100k, so even
        // the low band stays positive ("rounds out the shortlist") instead of sounding negative.
        String[] high = {"Strong fit", "Top-tier match", "Clear standout"};
        String[] mid  = {"Solid fit", "Reasonable fit", "Good, not top-tier"};
        String[] low  = {"Lower-shortlist fit", "Borderline for the top 100", "Rounds out the shortlist"};
        String verdict = (finalScore >= 0.70 ? high : finalScore >= 0.62 ? mid : low)[variant];

        StringBuilder sb = new StringBuilder(240);
        sb.append(head).append(' ').append(verdict).append(": ");
        sb.append(strengths.isEmpty()
                ? "adjacent signals only"
                : String.join("; ", strengths.subList(0, Math.min(3, strengths.size()))));
        sb.append('.');
        if (!concerns.isEmpty()) {
            String lead = switch (variant) {
                case 0 -> " Concerns: ";
                case 1 -> " Watch-outs: ";
                default -> " Caveats: ";
            };
            sb.append(lead).append(String.join("; ", concerns.subList(0, Math.min(2, concerns.size())))).append('.');
        }
        return collapse(cap(sb.toString().trim()));
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /** Collapse whitespace and stray double-periods so the prose reads cleanly. */
    private static String collapse(String s) {
        return s.replaceAll("\\s+", " ").replace(" .", ".").replace("..", ".");
    }
}
