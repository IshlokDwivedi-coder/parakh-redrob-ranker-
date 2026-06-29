package com.parakh.scoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the components each scorer produced for one candidate and works out the final score.
 *
 * final = (sum of the weighted additive components) * (product of the multipliers), clamped to [0,1].
 * If a candidate is flagged as a honeypot the score is forced to 0 so it can't reach the top 100.
 */
public class ScoreBreakdown {

    private final List<ScoreComponent> additive = new ArrayList<>();
    private final List<ScoreComponent> multipliers = new ArrayList<>();
    private boolean honeypot = false;
    private String honeypotReason = "";

    /** an additive component - adds weight*raw to the base. */
    public void add(ScoreComponent c) {
        additive.add(c);
    }

    /** a multiplier - raw is used directly as the factor. */
    public void multiply(ScoreComponent c) {
        multipliers.add(c);
    }

    public void flagHoneypot(String reason) {
        this.honeypot = true;
        this.honeypotReason = reason;
    }

    public boolean isHoneypot() {
        return honeypot;
    }

    public String honeypotReason() {
        return honeypotReason;
    }

    public List<ScoreComponent> additiveComponents() {
        return additive;
    }

    public List<ScoreComponent> multiplierComponents() {
        return multipliers;
    }

    public double base() {
        double sum = 0;
        for (ScoreComponent c : additive) sum += c.weighted();
        return sum;
    }

    public double finalScore() {
        if (honeypot) return 0.0;
        double score = base();
        for (ScoreComponent m : multipliers) score *= m.raw();
        if (score < 0) score = 0;
        if (score > 1) score = 1;
        return score;
    }
}
