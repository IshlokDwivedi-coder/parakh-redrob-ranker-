package com.parakh.scoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates the per-evaluator components for one candidate and computes the final score.
 *
 * <p>final = (Σ additive weighted components) × (Π multiplicative modifiers), clamped to [0,1].
 * A honeypot short-circuits everything to a final score of 0 so it can never enter the top 100.
 */
public class ScoreBreakdown {

    private final List<ScoreComponent> additive = new ArrayList<>();
    private final List<ScoreComponent> multipliers = new ArrayList<>();
    private boolean honeypot = false;
    private String honeypotReason = "";

    /** An additive component: contributes {@code weight * raw} to the base. */
    public void add(ScoreComponent c) {
        additive.add(c);
    }

    /** A multiplicative modifier in [0,1+]; {@code raw} is read directly as the multiplier. */
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
