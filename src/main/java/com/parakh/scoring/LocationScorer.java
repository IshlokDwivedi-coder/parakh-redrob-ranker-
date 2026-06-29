package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * Location fit (weight 0.10). The role is Pune/Noida hybrid and open to relocation from the big
 * Indian cities; outside India is case-by-case and they don't sponsor visas. So India scores
 * highest, willing-to-relocate within India is strong, and abroad-needs-a-visa is heavily discounted.
 */
@Service
public class LocationScorer implements Evaluator {

    public static final double WEIGHT = 0.10;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        String country = c.profile == null ? "" : c.profile.country().toLowerCase();
        String location = c.profile == null ? "" : c.profile.location().toLowerCase();
        boolean india = country.contains("india");
        boolean preferredCity = Lexicon.containsAny(location, Lexicon.PREFERRED_CITIES);
        boolean relocate = c.signals().willing_to_relocate;

        double raw;
        String evidence;
        if (india && preferredCity) {
            raw = 1.0; evidence = "in preferred Indian metro";
        } else if (india) {
            raw = 0.82; evidence = "in India" + (relocate ? ", will relocate" : "");
        } else if (relocate) {
            raw = 0.4; evidence = "abroad but willing to relocate (visa friction)";
        } else {
            raw = 0.12; evidence = "abroad, not relocating (no visa sponsorship)";
        }

        b.add(new ScoreComponent("Location", WEIGHT, raw, evidence));
    }
}
