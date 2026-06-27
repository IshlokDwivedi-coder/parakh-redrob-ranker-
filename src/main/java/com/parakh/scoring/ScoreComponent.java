package com.parakh.scoring;

/**
 * One explainable line item in a candidate's score.
 *
 * @param name     human label, e.g. "CareerRelevance"
 * @param weight   the weight applied to this component in the additive base score (0 for multipliers)
 * @param raw      the raw [0,1] signal this evaluator produced
 * @param evidence a short, GROUNDED phrase citing the real profile fact behind {@code raw}.
 *                 Reasoning is assembled only from these, so it can never hallucinate.
 */
public record ScoreComponent(String name, double weight, double raw, String evidence) {

    /** Contribution to the additive base score. */
    public double weighted() {
        return weight * raw;
    }
}
