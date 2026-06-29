package com.parakh.scoring;

/**
 * One line item in a candidate's score.
 *  - name:     a label like "CareerRelevance"
 *  - weight:   weight for the additive base score (0 for multipliers)
 *  - raw:      the 0..1 signal this scorer produced
 *  - evidence: a short phrase quoting the real profile fact behind raw. the reasoning
 *              column is built only from these phrases, so it can't make anything up.
 */
public record ScoreComponent(String name, double weight, double raw, String evidence) {

    /** weight * raw - what this component adds to the base score. */
    public double weighted() {
        return weight * raw;
    }
}
