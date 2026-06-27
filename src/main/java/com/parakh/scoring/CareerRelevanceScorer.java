package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * THE decisive signal (weight 0.35) and the explicit anti-keyword-stuffer. The JD says verbatim:
 * a Marketing Manager with every AI keyword is NOT a fit, while someone who BUILT a recommendation
 * system at a product company IS — even without the buzzwords.
 *
 * <p>So relevance is judged on TITLE CLASS + real CAREER EVIDENCE (titles, descriptions, summary) —
 * and pointedly NOT on the skills list, which is the surface the trap candidates stuff. An off-track
 * title with zero IR/ML evidence in the actual work history is floored, no matter how many AI skills
 * are listed.
 */
@Service
public class CareerRelevanceScorer implements Evaluator {

    public static final double WEIGHT = 0.35;

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

        // Real IR/ML evidence found in the WORK HISTORY (not the skills list).
        int evidenceCount = Lexicon.countDistinct(career, Lexicon.CORE_EVIDENCE_TERMS);
        double evidenceScore = Math.min(1.0, evidenceCount / 5.0);

        double raw = 0.6 * titleScore + 0.4 * evidenceScore;

        // Hard anti-trap floor: off-track title with no demonstrated IR/ML work cannot rank high,
        // regardless of a keyword-stuffed skills section.
        if (titleScore <= 0.08 && evidenceCount == 0) {
            raw = 0.04;
        }

        String evidence = titleClass
                + (evidenceCount > 0 ? ", " + evidenceCount + " IR/ML signals in work history" : ", no IR/ML work evidence");
        b.add(new ScoreComponent("CareerRelevance", WEIGHT, raw, evidence));
    }
}
