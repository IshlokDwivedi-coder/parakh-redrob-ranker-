package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill match, but trust-weighted (weight 0.20). Listing a skill is cheap, and the trap
 * candidates list a lot of them. So we only credit a relevant skill in proportion to the
 * evidence behind it: proficiency, endorsements, how long they've used it, and the Redrob
 * assessment score for that exact skill.
 *
 * That's what tells apart a real "Embeddings (expert, 40 endorsements, 4 yrs, assessment 88)"
 * from a padded "Embeddings (beginner, 0 endorsements, 2 mo, no assessment)".
 */
@Service
public class SkillTrustScorer implements Evaluator {

    public static final double WEIGHT = 0.20;
    private static final int TOP_N = 5;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        Map<String, Double> assessments = lowerKeys(c.signals().assessments());
        List<Double> trusts = new ArrayList<>();

        for (Candidate.Skill s : c.skills()) {
            String name = s.name().toLowerCase();
            if (!Lexicon.containsAny(name, Lexicon.CORE_EVIDENCE_TERMS)) continue; // only role-relevant skills

            double prof = switch (s.proficiency()) {
                case "expert" -> 1.0;
                case "advanced" -> 0.75;
                case "intermediate" -> 0.5;
                default -> 0.25; // beginner / unknown
            };
            double endorse = Math.min(1.0, s.endorsements / 20.0);
            double dur = Math.min(1.0, s.duration_months / 36.0);
            Double assess = assessments.get(name);
            double assessScore = assess != null ? assess / 100.0 : 0.4; // mild penalty for un-assessed

            double trust = 0.30 * prof + 0.25 * endorse + 0.20 * dur + 0.25 * assessScore;
            trusts.add(trust);
        }

        double raw;
        String evidence;
        if (trusts.isEmpty()) {
            raw = 0.08;
            evidence = "no role-relevant skills backed by evidence";
        } else {
            trusts.sort((x, y) -> Double.compare(y, x));
            int n = Math.min(TOP_N, trusts.size());
            double avg = 0;
            for (int i = 0; i < n; i++) avg += trusts.get(i);
            avg /= n;
            double coverage = Math.min(1.0, trusts.size() / 4.0); // reward breadth of trusted skills
            raw = avg * (0.7 + 0.3 * coverage);
            evidence = trusts.size() + " trusted role-relevant skill(s)";
        }

        b.add(new ScoreComponent("SkillTrust", WEIGHT, raw, evidence));
    }

    private static Map<String, Double> lowerKeys(Map<String, Double> in) {
        Map<String, Double> out = new java.util.HashMap<>();
        for (Map.Entry<String, Double> e : in.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) out.put(e.getKey().toLowerCase(), e.getValue());
        }
        return out;
    }
}
