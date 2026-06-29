package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * The JD's explicit "we do not want" list, applied as a multiplier rather than a hard zero
 * (only honeypots get a true zero). Someone can look relevant on paper but still be a bad fit:
 * a services-only career, mostly CV/speech/robotics, a job-hopper changing roles every ~1.5
 * years, or someone who has moved off writing code. Every rule that fires multiplies in, and we
 * floor the result so a bad fit gets pushed down instead of wiped out entirely.
 */
@Service
public class DisqualifierPenalizer implements Evaluator {

    public static final double FLOOR = 0.15;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        double mult = 1.0;
        StringBuilder why = new StringBuilder();

        // --- Services-only career: every role at a services/consulting firm, never a product company ---
        int total = 0, services = 0, product = 0;
        for (Candidate.CareerEntry e : c.careerHistory()) {
            total++;
            String comp = e.company().toLowerCase();
            String ind = e.industry().toLowerCase();
            if (Lexicon.containsAny(comp, Lexicon.SERVICES_COMPANIES) || ind.contains("it services") || ind.contains("consult"))
                services++;
            if (Lexicon.containsAny(comp, Lexicon.PRODUCT_COMPANIES)) product++;
        }
        if (total > 0 && services == total && product == 0) {
            mult *= 0.45;
            why.append("services-only career; ");
        }

        // --- CV/speech/robotics primary without NLP/IR exposure ---
        String career = Profiles.careerText(c);
        boolean cv = Lexicon.containsAny(career, Lexicon.CV_SPEECH_ROBOTICS);
        boolean nlpIr = Lexicon.containsAny(career, Lexicon.CORE_EVIDENCE_TERMS)
                || career.contains("nlp") || career.contains("natural language");
        if (cv && !nlpIr) {
            mult *= 0.5;
            why.append("CV/speech/robotics-primary; ");
        }

        // --- Title-chaser: 3+ jobs averaging under 18 months tenure ---
        int jobs = 0, monthsSum = 0;
        for (Candidate.CareerEntry e : c.careerHistory()) {
            if (e.duration_months > 0) { jobs++; monthsSum += e.duration_months; }
        }
        if (jobs >= 3) {
            double avgTenure = monthsSum / (double) jobs;
            if (avgTenure < 18) {
                mult *= 0.7;
                why.append(String.format("job-hopping (avg %.0f mo tenure); ", avgTenure));
            }
        }

        // --- Moved off the keyboard: architecture/leadership title, role no longer writes code ---
        String curTitle = c.profile == null ? "" : c.profile.title().toLowerCase();
        if (curTitle.contains("architect") || curTitle.contains("vp ") || curTitle.contains("vice president")
                || curTitle.contains("director") || curTitle.contains("head of") || curTitle.contains("principal")) {
            mult *= 0.85;
            why.append("leadership/architecture title (code-distance risk); ");
        }

        if (mult < FLOOR) mult = FLOOR;
        b.multiply(new ScoreComponent("Disqualifier", 0, mult,
                why.length() == 0 ? "no disqualifiers fired" : why.toString().trim()));
    }
}
