package com.parakh.scoring;

import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Disqualification guard. The dataset has ~80 planted "impossible" profiles, and if more than 10%
 * of our top 100 are honeypots we're disqualified. They look perfect on paper, so we catch them on
 * arithmetic that can't be true for a real person: a skill used longer than they've worked, more
 * experience than their age allows, or a job that ends before it starts.
 *
 * We tune for precision, not recall - every check is a hard impossibility, so a real strong
 * candidate never trips it. A flagged profile scores 0 and can't reach the top 100.
 */
@Service
public class HoneypotGate implements Evaluator {

    /** Slack (months) allowed before a skill-duration / experience mismatch is deemed impossible. */
    private static final int MONTH_SLACK = 24;

    @Override
    public void evaluate(Candidate c, ScoreBreakdown b) {
        double yoe = c.profile == null ? 0 : c.profile.years_of_experience;
        int yoeMonths = (int) Math.round(yoe * 12);

        // 1. A skill used longer than the candidate has worked at all (+ generous slack) is impossible.
        for (Candidate.Skill s : c.skills()) {
            if (s.duration_months > yoeMonths + MONTH_SLACK && s.duration_months > 12) {
                b.flagHoneypot("skill '" + s.name() + "' duration " + s.duration_months
                        + "mo exceeds total experience " + yoeMonths + "mo");
                return;
            }
        }

        // 2. More career experience than years since starting college is impossible.
        int earliestEduStart = Integer.MAX_VALUE;
        for (Candidate.Education e : c.education()) {
            if (e.start_year >= 1970 && e.start_year < earliestEduStart) earliestEduStart = e.start_year;
            if (e.end_year > 0 && e.start_year > 0 && e.end_year < e.start_year) {
                b.flagHoneypot("education ends (" + e.end_year + ") before it starts (" + e.start_year + ")");
                return;
            }
        }
        if (earliestEduStart != Integer.MAX_VALUE) {
            int yearsSinceCollege = Profiles.REFERENCE_DATE.getYear() - earliestEduStart;
            if (yoe > yearsSinceCollege + 2) {
                b.flagHoneypot("claims " + yoe + "y experience but only " + yearsSinceCollege
                        + "y since college began (" + earliestEduStart + ")");
                return;
            }
        }

        // 3. Career-history date contradictions: end before start, or a future start date.
        for (Candidate.CareerEntry e : c.careerHistory()) {
            LocalDate start = Profiles.parse(e.start_date);
            LocalDate end = Profiles.parse(e.end_date);
            if (start != null && start.isAfter(Profiles.REFERENCE_DATE)) {
                b.flagHoneypot("career role at '" + e.company() + "' starts in the future (" + e.start_date + ")");
                return;
            }
            if (start != null && end != null && end.isBefore(start)) {
                b.flagHoneypot("career role at '" + e.company() + "' ends before it starts");
                return;
            }
            if (e.duration_months > 600) {
                b.flagHoneypot("career role duration " + e.duration_months + "mo is impossible");
                return;
            }
        }

        // 4. Total career span far exceeding claimed experience (timeline can't hold the claim).
        //    Uses the contiguous span earliest-start..latest-end; gaps make this conservative, not false-positive.
        LocalDate earliest = null, latest = null;
        for (Candidate.CareerEntry e : c.careerHistory()) {
            LocalDate s = Profiles.parse(e.start_date);
            LocalDate en = e.is_current ? Profiles.REFERENCE_DATE : Profiles.parse(e.end_date);
            if (s != null && (earliest == null || s.isBefore(earliest))) earliest = s;
            if (en != null && (latest == null || en.isAfter(latest))) latest = en;
        }
        if (earliest != null && latest != null) {
            int spanMonths = (int) java.time.temporal.ChronoUnit.MONTHS.between(earliest, latest);
            if (yoeMonths > spanMonths + 3 * 12) {
                b.flagHoneypot("claims " + yoe + "y experience but career timeline spans only "
                        + String.format("%.1f", spanMonths / 12.0) + "y");
                return;
            }
        }
    }
}
