package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * Behavioral availability (MULTIPLICATIVE). The JD's closing instruction is explicit: "a
 * perfect-on-paper candidate who hasn't logged in for 6 months and has a 5% recruiter response rate
 * is, for hiring purposes, not actually available. Down-weight them appropriately."
 *
 * <p>We compound four observable behaviors — login recency, recruiter responsiveness, open-to-work,
 * and notice period — into one multiplier on the paper score. Worked example from the JD
 * (stale 6mo + 5% response) lands near ×0.3, exactly the intended heavy discount.
 */
@Service
public class AvailabilityModifier implements Evaluator {

    public static final double FLOOR = 0.20;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        Candidate.RedrobSignals s = c.signals();

        // Login recency. A genuinely active candidate (within ~6 weeks) should NOT be taxed at all —
        // the old 30-day cliff docked even a 32-days-ago candidate 8%, so availability was quietly
        // re-ordering the strong top tier instead of just down-weighting the truly stale (the JD's intent).
        long days = Profiles.daysSinceActive(c);
        double recency;
        if (days <= 45) recency = 1.0;
        else if (days <= 90) recency = 0.95;
        else if (days <= 180) recency = 0.82;
        else if (days <= 365) recency = 0.60;
        else recency = 0.35;

        // Recruiter responsiveness: 0% -> 0.5, 100% -> 1.0. Kept deliberately steep at the low end so
        // the JD's worked example (stale 6mo + 5% response) still lands near the intended ×0.3.
        double response = 0.5 + 0.5 * clamp01(s.recruiter_response_rate);

        // Open-to-work and notice period. A standard ≤60-day notice is normal hiring, not a penalty.
        double otw = s.open_to_work_flag ? 1.0 : 0.80;
        double notice;
        if (s.notice_period_days <= 60) notice = 1.0;
        else if (s.notice_period_days <= 90) notice = 0.97;
        else if (s.notice_period_days <= 150) notice = 0.90;
        else notice = 0.82;

        double mult = recency * response * otw * notice;
        if (mult < FLOOR) mult = FLOOR;

        String evidence = String.format("active %s, response %.0f%%, %s, notice %dd",
                days == Long.MAX_VALUE ? "unknown" : days + "d ago",
                clamp01(s.recruiter_response_rate) * 100,
                s.open_to_work_flag ? "open-to-work" : "not open",
                s.notice_period_days);
        b.multiply(new ScoreComponent("Availability", 0, mult, evidence));
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
