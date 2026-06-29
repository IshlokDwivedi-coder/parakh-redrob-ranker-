package com.parakh.engine;

import java.util.List;

/**
 * A scored candidate in the top-100 list. We sort and print on the SAME rounded score, so the
 * validator rule (scores non-increasing, ties broken by candidate_id) can't be broken by a row
 * that looks tied in the file but wasn't tied in our sort.
 *
 * base and components are just a snapshot for the ~100 survivors so the dashboard can show each
 * scorer's contribution; the csv and the ordering never read them.
 */
public record RankedCandidate(
        String candidateId, double score, String reasoning,
        double base, List<ComponentScore> components) {

    /** Ordering from BEST to WORST: higher score first, then candidate_id ascending on ties. */
    public static int betterFirst(RankedCandidate a, RankedCandidate b) {
        int byScore = Double.compare(b.score, a.score);
        if (byScore != 0) return byScore;
        return a.candidateId.compareTo(b.candidateId);
    }
}
