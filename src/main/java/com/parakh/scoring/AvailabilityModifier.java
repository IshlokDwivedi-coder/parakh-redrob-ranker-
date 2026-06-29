package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

/**
 * Availability, as a multiplier. The JD spells this out: a perfect-on-paper candidate who hasn't
 * logged in for 6 months and answers 5% of recruiters is, for hiring, not really available, so
 * down-weight them.
 *
 * We fold four things we can actually observe - login recency, recruiter response rate,
 * open-to-work, and notice period - into one multiplier on the paper score. The JD's own example
 * (gone 6 months, 5% response) lands around x0.3, which is the heavy discount it asks for.
 */
@Service
public class AvailabilityModifier implements Evaluator {

    public static final double FLOOR = 0.20;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        Candidate.RedrobSignals s = c.signals();

        // login recency. someone active in the last ~6 weeks isn't penalised at all; we only start
        // discounting once they've been away a while, since the JD only wants the genuinely inactive
        // pushed down, not the strong top tier reshuffled.
        long days = Profiles.daysSinceActive(c);
        double recency;
        if (days <= 45) recency = 1.0;
        else if (days <= 90) recency = 0.95;
        else if (days <= 180) recency = 0.82;
        else if (days <= 365) recency = 0.60;
        else recency = 0.35;

        // recruiter response rate: 0% maps to 0.5, 100% to 1.0. kept steep at the low end so the
        // JD's example (gone 6mo + 5% response) still lands near x0.3.
        double response = 0.5 + 0.5 * clamp01(s.recruiter_response_rate);

        // open-to-work and notice period. a normal notice of 60 days or less isn't a penalty.
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
