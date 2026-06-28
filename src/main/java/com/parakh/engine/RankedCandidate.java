package com.parakh.engine;

import java.util.List;

/**
 * A scored candidate carried through the top-100 selection. {@code score} is the rounded value used
 * for BOTH ordering and output, so the validator's "non-increasing score + tie-break by candidate_id
 * ascending" rule holds exactly (rounding then sorting avoids the full-precision-vs-rounded tie trap).
 *
 * <p>{@code base} and {@code components} are an additive, output-only snapshot of the score breakdown,
 * retained ONLY for the ~100 survivors so the dashboard can show each evaluator's contribution. They
 * are never read by ordering, the validator, or the submission CSV; ranking behaviour is unchanged.
 */
public record RankedCandidate(
        String candidateId, double score, String reasoning,
        double base, List<ComponentScore> components) {

    /** Convenience for callers that don't need the breakdown (tests, ad-hoc use). */
    public static RankedCandidate of(String candidateId, double score, String reasoning) {
        return new RankedCandidate(candidateId, score, reasoning, score, List.of());
    }

    /** Ordering from BEST to WORST: higher score first, then candidate_id ascending on ties. */
    public static int betterFirst(RankedCandidate a, RankedCandidate b) {
        int byScore = Double.compare(b.score, a.score);
        if (byScore != 0) return byScore;
        return a.candidateId.compareTo(b.candidateId);
    }
}
