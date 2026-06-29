package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * The heaviest signal (weight 0.35). Someone who lists every AI keyword while really working in,
 * say, marketing isn't a fit, but someone who built a recommender at a product company is. So we
 * score three things: the title, the real IR/ML evidence in the work history (titles, descriptions,
 * summary), and seniority. We ignore the skills list here on purpose - it's the easy part to fake,
 * so an off-track title with no real evidence gets floored however many skills it lists.
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

        // count how many distinct IR/ML terms show up in the work history (not the skills list).
        // the curve keeps climbing instead of flattening at a cap, so someone with 20 real signals
        // still ranks above someone with 10 - which matters since the top-10 order is half the score.
        int evidenceCount = Lexicon.countDistinct(career, Lexicon.CORE_EVIDENCE_TERMS);
        double evidenceScore = 1.0 - Math.exp(-evidenceCount / 8.0);

        // seniority: Staff/Principal > Senior/Lead > IC. Used as a tie-breaker so a senior engineer
        // edges out a junior with the same evidence.
        double seniorityScore;
        String seniority;
        if (Lexicon.containsAny(titles, SENIOR_LEAD)) { seniorityScore = 1.0; seniority = "staff/principal-level"; }
        else if (Lexicon.containsAny(titles, SENIOR_IC)) { seniorityScore = 0.82; seniority = "senior/lead-level"; }
        else { seniorityScore = 0.60; seniority = "IC-level"; }

        double raw = 0.50 * titleScore + 0.32 * evidenceScore + 0.18 * seniorityScore;

        // floor: an off-track title with no real IR/ML work can't rank high, whatever the skills say
        if (titleScore <= 0.08 && evidenceCount == 0) {
            raw = 0.04;
        }

        String evidence = titleClass + ", " + seniority
                + (evidenceCount > 0 ? ", " + evidenceCount + " IR/ML signals in work history" : ", no IR/ML work evidence");
        b.add(new ScoreComponent("CareerRelevance", WEIGHT, raw, evidence));
    }
}
