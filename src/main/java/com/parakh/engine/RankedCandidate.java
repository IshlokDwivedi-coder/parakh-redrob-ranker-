package com.parakh.engine;

/**
 * A scored candidate carried through the top-100 selection. {@code score} is the rounded value used
 * for BOTH ordering and output, so the validator's "non-increasing score + tie-break by candidate_id
 * ascending" rule holds exactly (rounding then sorting avoids the full-precision-vs-rounded tie trap).
 */
public record RankedCandidate(String candidateId, double score, String reasoning) {

    /** Ordering from BEST to WORST: higher score first, then candidate_id ascending on ties. */
    public static int betterFirst(RankedCandidate a, RankedCandidate b) {
        int byScore = Double.compare(b.score, a.score);
        if (byScore != 0) return byScore;
        return a.candidateId.compareTo(b.candidateId);
    }
}
