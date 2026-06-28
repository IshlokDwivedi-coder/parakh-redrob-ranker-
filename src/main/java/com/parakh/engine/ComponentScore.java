package com.parakh.engine;

import com.parakh.scoring.ScoreComponent;

/**
 * An immutable, output-facing snapshot of one scored line item for a surviving candidate.
 *
 * <p>The engine's {@link com.parakh.scoring.ScoreBreakdown} is mutable and short-lived; this record
 * is the frozen view that travels out with a {@link RankedCandidate} so the dashboard can show the
 * exact per-evaluator contribution behind a final score. It adds no signal — it only exposes what
 * the panel already computed.
 *
 * @param name       evaluator label, e.g. "CareerRelevance"
 * @param weight     additive weight (0 for multiplicative modifiers)
 * @param raw        the raw [0,1] signal the evaluator produced
 * @param weighted   contribution to the additive base ({@code weight * raw}); 0 for modifiers
 * @param evidence   the grounded phrase citing the real profile fact behind {@code raw}
 * @param multiplier true if this is a multiplicative modifier rather than an additive component
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
