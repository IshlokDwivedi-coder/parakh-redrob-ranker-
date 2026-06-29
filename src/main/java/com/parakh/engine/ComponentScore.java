package com.parakh.engine;

import com.parakh.scoring.ScoreComponent;

/**
 * A frozen, read-only copy of one scored line item for a top-100 candidate. ScoreBreakdown is
 * mutable and thrown away after each candidate, so this is the copy that rides along with a
 * RankedCandidate to let the dashboard show what each scorer added. It carries the numbers, it
 * doesn't compute any. (weight/weighted are 0 for multipliers; raw is the 0..1 signal; evidence
 * quotes a real profile fact.)
 */
public record ComponentScore(
        String name, double weight, double raw, double weighted, String evidence, boolean multiplier) {

    public static ComponentScore additive(ScoreComponent c) {
        return new ComponentScore(c.name(), c.weight(), c.raw(), c.weighted(), c.evidence(), false);
    }

    public static ComponentScore modifier(ScoreComponent c) {
        return new ComponentScore(c.name(), 0.0, c.raw(), 0.0, c.evidence(), true);
    }
}
