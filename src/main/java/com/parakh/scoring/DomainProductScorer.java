package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * Domain quality (weight 0.15): did they build at PRODUCT companies, and is their domain NLP/IR
 * rather than CV/speech/robotics? The JD prizes product-company shipping and explicitly de-prioritises
 * CV/speech/robotics-primary profiles. (The hard services-only / CV-primary KILLS are applied
 * multiplicatively in {@link DisqualifierPenalizer}; here we reward the positive domain fit.)
 */
@Service
public class DomainProductScorer implements Evaluator {

    public static final double WEIGHT = 0.15;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        int productRoles = 0, servicesRoles = 0, total = 0;
        for (Candidate.CareerEntry e : c.careerHistory()) {
            total++;
            String company = e.company().toLowerCase();
            String industry = e.industry().toLowerCase();
            boolean services = Lexicon.containsAny(company, Lexicon.SERVICES_COMPANIES)
                    || industry.contains("it services") || industry.contains("consult");
            boolean product = Lexicon.containsAny(company, Lexicon.PRODUCT_COMPANIES)
                    || (!services && (industry.contains("product") || industry.contains("internet")
                        || industry.contains("software") || industry.contains("technology")));
            if (product) productRoles++;
            else if (services) servicesRoles++;
        }
        double productSignal = total == 0 ? 0.3 : (double) productRoles / total;

        String career = Profiles.careerText(c);
        boolean hasNlpIr = Lexicon.containsAny(career, Lexicon.CORE_EVIDENCE_TERMS)
                || career.contains("nlp") || career.contains("natural language");
        boolean hasCvEtc = Lexicon.containsAny(career, Lexicon.CV_SPEECH_ROBOTICS);
        double domainSignal;
        if (hasNlpIr) domainSignal = 1.0;
        else if (hasCvEtc) domainSignal = 0.2;       // CV/speech/robotics without NLP/IR
        else domainSignal = 0.5;                      // neutral / general SWE

        double raw = 0.6 * productSignal + 0.4 * domainSignal;
        b.add(new ScoreComponent("DomainProduct", WEIGHT, raw,
                productRoles + "/" + total + " product roles, "
                        + (hasNlpIr ? "NLP/IR domain" : hasCvEtc ? "CV/speech/robotics domain" : "general domain")));
    }
}
