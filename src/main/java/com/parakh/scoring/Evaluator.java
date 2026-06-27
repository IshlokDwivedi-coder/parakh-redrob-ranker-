package com.parakh.scoring;

import com.parakh.model.Candidate;

/**
 * One member of the explainable scoring panel. Each evaluator inspects the candidate and contributes
 * either an additive {@link ScoreComponent}, a multiplicative modifier, or a honeypot flag to the
 * shared {@link ScoreBreakdown}. The panel is transparent by construction: the final number is just
 * the sum of what these nine evaluators decided, with the evidence string of each carried along.
 */
public interface Evaluator {

    void evaluate(Candidate c, ScoreBreakdown breakdown);
}
