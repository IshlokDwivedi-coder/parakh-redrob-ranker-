package com.parakh.engine;

import com.parakh.io.CandidateReader;
import com.parakh.model.Candidate;
import com.parakh.scoring.AvailabilityModifier;
import com.parakh.scoring.CareerRelevanceScorer;
import com.parakh.scoring.DisqualifierPenalizer;
import com.parakh.scoring.DomainProductScorer;
import com.parakh.scoring.EvalSignalScorer;
import com.parakh.scoring.Evaluator;
import com.parakh.scoring.ExperienceFitScorer;
import com.parakh.scoring.HoneypotGate;
import com.parakh.scoring.LocationScorer;
import com.parakh.scoring.ScoreBreakdown;
import com.parakh.scoring.SkillTrustScorer;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The core: stream every candidate through the explainable panel, keep only the best 100 in a
 * bounded min-heap, and emit them ranked. Memory stays O(100) regardless of input size; one pass,
 * no network, no model — trivially inside the 5-minute / 16GB / CPU-only / network-off budget.
 *
 * <p>The panel runs in a fixed, defensible order: HoneypotGate first (a flag short-circuits scoring
 * to zero), then the six additive scorers, then the two multiplicative modifiers.
 */
@Service
public class RankingEngine {

    private static final int TOP_K = 100;
    private static final int SCORE_DECIMALS = 6;

    private final CandidateReader reader;
    private final ReasoningComposer reasoning;

    private final HoneypotGate honeypotGate;
    private final List<Evaluator> additive;
    private final List<Evaluator> modifiers;

    public RankingEngine(CandidateReader reader, ReasoningComposer reasoning,
                         HoneypotGate honeypotGate,
                         CareerRelevanceScorer career, SkillTrustScorer skill,
                         ExperienceFitScorer experience, DomainProductScorer domain,
                         LocationScorer location, EvalSignalScorer eval,
                         DisqualifierPenalizer disqualifier, AvailabilityModifier availability) {
        this.reader = reader;
        this.reasoning = reasoning;
        this.honeypotGate = honeypotGate;
        // Order matters for the narrative and the reasoning prose; CareerRelevance leads.
        this.additive = List.of(career, skill, experience, domain, location, eval);
        this.modifiers = List.of(disqualifier, availability);
    }

    public Result rank(Path input) {
        // Min-heap of the current best 100: head = WORST kept candidate, evicted when a better one arrives.
        PriorityQueue<RankedCandidate> heap =
                new PriorityQueue<>((a, b) -> -RankedCandidate.betterFirst(a, b));
        AtomicLong honeypots = new AtomicLong();

        long total = reader.stream(input, c -> {
            ScoreBreakdown b = new ScoreBreakdown();
            honeypotGate.evaluate(c, b);
            if (b.isHoneypot()) {
                honeypots.incrementAndGet();
                return; // forced out of contention entirely
            }
            for (Evaluator e : additive) e.evaluate(c, b);
            for (Evaluator e : modifiers) e.evaluate(c, b);

            double score = round(b.finalScore());
            RankedCandidate rc = new RankedCandidate(c.candidate_id, score, reasoning.compose(c, b));

            if (heap.size() < TOP_K) {
                heap.offer(rc);
            } else if (RankedCandidate.betterFirst(rc, heap.peek()) < 0) {
                heap.poll();
                heap.offer(rc);
            }
        });

        List<RankedCandidate> ranked = new ArrayList<>(heap);
        ranked.sort(RankedCandidate::betterFirst);
        return new Result(ranked, total, honeypots.get());
    }

    private static double round(double v) {
        double f = Math.pow(10, SCORE_DECIMALS);
        return Math.round(v * f) / f;
    }

    /** Outcome of a ranking pass: the ordered top-100 plus run statistics for the audit log. */
    public record Result(List<RankedCandidate> top, long totalScored, long honeypotsRejected) {}
}
