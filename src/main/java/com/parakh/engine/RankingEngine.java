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
 * The core loop. Streams every candidate through the scorer panel and keeps only the best 100
 * in a min-heap, so memory stays O(100) however big the file is - one pass, no model, no network.
 *
 * Panel order: HoneypotGate first (a flagged profile scores 0 and is skipped), then the six
 * additive scorers, then the two multipliers.
 */
@Service
public class RankingEngine {

    private static final int TOP_K = 100;
    private static final int SCORE_DECIMALS = 6;
    /** how many rejected honeypots to keep as a sample for the dashboard log. */
    private static final int HONEYPOT_SAMPLE = 30;

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
        // order matters for the reasoning text; CareerRelevance comes first.
        this.additive = List.of(career, skill, experience, domain, location, eval);
        this.modifiers = List.of(disqualifier, availability);
    }

    public Result rank(Path input) {
        // Min-heap of the current best 100: head = WORST kept candidate, evicted when a better one arrives.
        PriorityQueue<RankedCandidate> heap =
                new PriorityQueue<>((a, b) -> -RankedCandidate.betterFirst(a, b));
        AtomicLong honeypots = new AtomicLong();
        // The first HONEYPOT_SAMPLE rejects, in file order (the read is sequential, so this is
        // deterministic). Used only for the dashboard's "why we rejected these" audit log.
        List<HoneypotHit> honeypotSample = new ArrayList<>();

        long total = reader.stream(input, c -> {
            ScoreBreakdown b = new ScoreBreakdown();
            honeypotGate.evaluate(c, b);
            if (b.isHoneypot()) {
                honeypots.incrementAndGet();
                if (honeypotSample.size() < HONEYPOT_SAMPLE) {
                    honeypotSample.add(new HoneypotHit(c.candidate_id, b.honeypotReason()));
                }
                return; // forced out of contention entirely
            }
            for (Evaluator e : additive) e.evaluate(c, b);
            for (Evaluator e : modifiers) e.evaluate(c, b);

            double score = round(b.finalScore());
            RankedCandidate rc = new RankedCandidate(
                    c.candidate_id, score, reasoning.compose(c, b), b.base(), snapshot(b));

            if (heap.size() < TOP_K) {
                heap.offer(rc);
            } else if (RankedCandidate.betterFirst(rc, heap.peek()) < 0) {
                heap.poll();
                heap.offer(rc);
            }
        });

        List<RankedCandidate> ranked = new ArrayList<>(heap);
        ranked.sort(RankedCandidate::betterFirst);
        return new Result(ranked, total, honeypots.get(), honeypotSample);
    }

    private static double round(double v) {
        double f = Math.pow(10, SCORE_DECIMALS);
        return Math.round(v * f) / f;
    }

    /** Freezes a breakdown into an output-only list of components (additive first, then modifiers). */
    private static List<ComponentScore> snapshot(ScoreBreakdown b) {
        List<ComponentScore> out = new ArrayList<>();
        b.additiveComponents().forEach(c -> out.add(ComponentScore.additive(c)));
        b.multiplierComponents().forEach(c -> out.add(ComponentScore.modifier(c)));
        return out;
    }

    /** One rejected honeypot, kept for the dashboard audit log. */
    public record HoneypotHit(String candidateId, String reason) {}

    /** Outcome of a ranking pass: the ordered top-100 plus run statistics for the audit log. */
    public record Result(List<RankedCandidate> top, long totalScored, long honeypotsRejected,
                         List<HoneypotHit> honeypotSample) {}
}
