package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * THE decisive signal (weight 0.35) and the explicit anti-keyword-stuffer. The JD says verbatim:
 * a Marketing Manager with every AI keyword is NOT a fit, while someone who BUILT a recommendation
 * system at a product company IS — even without the buzzwords.
 *
 * <p>So relevance is judged on TITLE CLASS + real CAREER EVIDENCE (titles, descriptions, summary) +
 * SENIORITY — and pointedly NOT on the skills list, which is the surface the trap candidates stuff.
 * An off-track title with zero IR/ML evidence in the actual work history is floored, no matter how
 * many AI skills are listed. The evidence term uses a saturating-but-never-flat curve so this signal
 * keeps discriminating among strong candidates (critical: NDCG@10 is half the composite score).
 */
@Service
public class CareerRelevanceScorer implements Evaluator {

    public static final double WEIGHT = 0.35;

    private static final Set<String> SENIOR_LEAD = Set.of("staff", "principal", "head of", "head,", "distinguished", "vp ", "director");
    private static final Set<String> SENIOR_IC = Set.of("senior", "sr.", "sr ", "lead");

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        String titles = Profiles.titleText(c);
        String career = Profiles.careerText(c);

        double titleScore;
        String titleClass;
        if (Lexicon.containsAny(titles, Lexicon.BULLSEYE_TITLES)) {
            titleScore = 1.0;
            titleClass = "on-target ranking/retrieval title";
        } else if (Lexicon.containsAny(titles, Lexicon.ADJACENT_TITLES)) {
            titleScore = 0.55;
            titleClass = "adjacent engineering/DS title";
        } else if (Lexicon.containsAny(titles, Lexicon.OFFTRACK_TITLES)) {
            titleScore = 0.08;
            titleClass = "off-track title";
        } else {
            titleScore = 0.30;
            titleClass = "unclassified title";
        }

        // Real IR/ML evidence found in the WORK HISTORY (not the skills list). Saturating-but-never-flat
        // curve so 20 signals genuinely outranks 10 — the old min(1, n/5) capped at 5 signals, flattening
        // the entire top tier to an identical 0.350 and giving up the top-10 ordering that NDCG@10 rewards.
        int evidenceCount = Lexicon.countDistinct(career, Lexicon.CORE_EVIDENCE_TERMS);
        double evidenceScore = 1.0 - Math.exp(-evidenceCount / 8.0); // 5->.47, 10->.71, 15->.85, 20->.92

        // Seniority depth (Staff/Principal > Senior/Lead > IC). The JD wants 6-8 yrs who SHIPPED, so a
        // Staff/Lead ranking engineer should edge out a same-evidence junior — another top-tier tie-breaker.
        double seniorityScore;
        String seniority;
        if (Lexicon.containsAny(titles, SENIOR_LEAD)) { seniorityScore = 1.0; seniority = "staff/principal-level"; }
        else if (Lexicon.containsAny(titles, SENIOR_IC)) { seniorityScore = 0.82; seniority = "senior/lead-level"; }
        else { seniorityScore = 0.60; seniority = "IC-level"; }

        double raw = 0.50 * titleScore + 0.32 * evidenceScore + 0.18 * seniorityScore;

        // Hard anti-trap floor: off-track title with no demonstrated IR/ML work cannot rank high,
        // regardless of a keyword-stuffed skills section.
        if (titleScore <= 0.08 && evidenceCount == 0) {
            raw = 0.04;
        }

        String evidence = titleClass + ", " + seniority
                + (evidenceCount > 0 ? ", " + evidenceCount + " IR/ML signals in work history" : ", no IR/ML work evidence");
        b.add(new ScoreComponent("CareerRelevance", WEIGHT, raw, evidence));
    }
}
