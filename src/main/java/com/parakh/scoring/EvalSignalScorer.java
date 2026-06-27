package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * "Nice-to-haves" and external-validation signals (weight 0.10). The JD lists eval-framework literacy
 * (NDCG/MRR/MAP), open-source / GitHub activity, fine-tuning (LoRA/QLoRA/PEFT), learning-to-rank, and
 * HR-tech exposure as bonuses, and explicitly distrusts people with no external validation. This is a
 * small additive booster, never decisive on its own.
 */
@Service
public class EvalSignalScorer implements Evaluator {

    public static final double WEIGHT = 0.10;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        String career = Profiles.careerText(c);
        double gh = c.signals().github_activity_score; // -1 if no GitHub

        double evalLiteracy = (career.contains("ndcg") || career.contains("mrr") || career.contains("map@")
                || career.contains("a/b test") || career.contains("offline") && career.contains("online")) ? 1.0 : 0.0;
        double ghScore = gh < 0 ? 0.0 : Math.min(1.0, gh / 60.0);
        double niceToHave = (career.contains("lora") || career.contains("qlora") || career.contains("peft")
                || career.contains("learning to rank") || career.contains("xgboost")
                || career.contains("recsys") || career.contains("hr-tech") || career.contains("recruit")) ? 1.0 : 0.0;
        double certs = (c.certifications != null && !c.certifications.isEmpty()) ? 1.0 : 0.0;

        double raw = 0.35 * evalLiteracy + 0.30 * ghScore + 0.25 * niceToHave + 0.10 * certs;

        StringBuilder ev = new StringBuilder();
        if (evalLiteracy > 0) ev.append("eval-framework literate; ");
        if (gh >= 0) ev.append("github ").append(String.format("%.0f", gh)).append("; ");
        if (niceToHave > 0) ev.append("relevant nice-to-haves; ");
        if (ev.length() == 0) ev.append("limited bonus signals");

        b.add(new ScoreComponent("EvalSignal", WEIGHT, raw, ev.toString().trim()));
    }
}
