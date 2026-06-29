package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * Experience-band fit (weight 0.10). The JD says "5-9 years", with the sweet spot around 6-8 and
 * 4-5 of those in applied ML at product companies. So this is a tent shape: it peaks at 6-8, stays
 * strong across 5-9, and tapers off for the too-junior and the very senior (likely on a mgmt track).
 */
@Service
public class ExperienceFitScorer implements Evaluator {

    public static final double WEIGHT = 0.10;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        double y = c.profile == null ? 0 : c.profile.years_of_experience;

        double raw;
        if (y >= 6 && y <= 8) raw = 1.0;
        else if (y >= 5 && y < 6) raw = 0.85;
        else if (y > 8 && y <= 9) raw = 0.85;
        else if (y >= 4 && y < 5) raw = 0.65;
        else if (y > 9 && y <= 11) raw = 0.6;
        else if (y >= 3 && y < 4) raw = 0.4;
        else if (y > 11 && y <= 14) raw = 0.35;
        else if (y < 3) raw = 0.15;
        else raw = 0.2; // 14+ years: likely management track for this IC role

        b.add(new ScoreComponent("ExperienceFit", WEIGHT, raw,
                String.format("%.1f yrs vs 6–8 ideal (5–9 band)", y)));
    }
}
