package com.parakh.scoring;

import com.parakh.model.Candidate;

/**
 * One scorer in the panel. Each one looks at a candidate and adds something to the
 * shared ScoreBreakdown - either an additive component, a multiplier, or a honeypot
 * flag. The final score is just the sum of what all the scorers decided, and each one
 * also leaves behind a short evidence string we reuse for the reasoning text.
 */
public interface Evaluator {

    void evaluate(Candidate c, ScoreBreakdown breakdown);
}
