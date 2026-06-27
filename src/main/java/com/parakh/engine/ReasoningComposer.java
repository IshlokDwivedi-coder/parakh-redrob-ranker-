package com.parakh.engine;

import com.parakh.model.Candidate;
import com.parakh.scoring.ScoreBreakdown;
import com.parakh.scoring.ScoreComponent;
import org.springframework.stereotype.Service;

/**
 * Builds the human-readable {@code reasoning} string for the submission. Two hard requirements from
 * the challenge's Stage-4 human review: it must cite REAL profile facts (no hallucination) and must
 * acknowledge concerns. We satisfy both by construction — the string is assembled ONLY from the
 * evidence phrases the evaluators already attached to grounded fields, plus a Concerns clause sourced
 * from the multiplicative penalties. Nothing here invents anything.
 */
@Service
public class ReasoningComposer {

    /** Honeypots never reach the top 100, but compose a reason anyway for the rejection log / audit. */
    public String composeHoneypot(Candidate c, ScoreBreakdown b) {
        return "REJECTED as honeypot: " + b.honeypotReason() + ".";
    }

    public String compose(Candidate c, ScoreBreakdown b) {
        Candidate.Profile p = c.profile;
        String title = p == null ? "Candidate" : safe(p.title());
        String company = p == null ? "" : safe(p.company());
        double yoe = p == null ? 0 : p.years_of_experience;

        StringBuilder sb = new StringBuilder(220);
        sb.append(title);
        if (!company.isEmpty()) sb.append(" @ ").append(company);
        sb.append(String.format(", %.1f yrs. ", yoe));

        // Additive evidence, in panel order (CareerRelevance first — the headline signal).
        for (ScoreComponent comp : b.additiveComponents()) {
            switch (comp.name()) {
                case "CareerRelevance" -> sb.append(cap(comp.evidence())).append(". ");
                case "DomainProduct" -> sb.append(cap(comp.evidence())).append(". ");
                case "SkillTrust" -> sb.append(cap(comp.evidence())).append(". ");
                default -> { /* keep reasoning tight; other additive signals omitted from prose */ }
            }
        }

        // Availability is always worth stating (it's a top differentiator in this dataset).
        // Concerns come from any multiplier that fired below ~0.95.
        StringBuilder concerns = new StringBuilder();
        for (ScoreComponent m : b.multiplierComponents()) {
            if (m.name().equals("Availability")) {
                sb.append(cap(m.evidence())).append(". ");
                if (m.raw() < 0.7) concerns.append("availability is limited");
            } else if (m.name().equals("Disqualifier") && m.raw() < 0.95) {
                if (concerns.length() > 0) concerns.append("; ");
                concerns.append(m.evidence());
            }
        }
        if (concerns.length() > 0) sb.append("Concerns: ").append(concerns).append(".");

        return collapse(sb.toString().trim());
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
