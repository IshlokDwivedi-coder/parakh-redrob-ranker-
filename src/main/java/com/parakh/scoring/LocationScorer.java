package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * Geographic fit (weight 0.10). Role is Pune/Noida hybrid, open to relocation from Tier-1 Indian
 * cities; "Outside India: case-by-case, we don't sponsor work visas." So India-based scores highest,
 * willing-to-relocate domestic is strong, and abroad-needs-visa is heavily discounted.
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
