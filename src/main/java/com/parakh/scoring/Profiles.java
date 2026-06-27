package com.parakh.scoring;

import com.parakh.model.Candidate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Derived, reused views over a {@link Candidate}: lower-cased concatenated text blobs and safe
 * date math. Keeping these here means every scorer reasons over the same normalized strings.
 */
public final class Profiles {

    private Profiles() {}

    /**
     * Fixed "today" for reproducibility. The ranking must produce identical output in the judges'
     * offline Docker run (network off, no wall clock dependency), so recency is measured against a
     * constant, not {@code LocalDate.now()}. Set just past the dataset's latest activity.
     */
    public static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 6, 27);

    /** All career-history titles + descriptions + headline + summary, lower-cased. The evidence corpus. */
    public static String careerText(Candidate c) {
        StringBuilder sb = new StringBuilder(512);
        if (c.profile != null) {
            sb.append(c.profile.headline()).append(' ')
              .append(c.profile.summary()).append(' ')
              .append(c.profile.title()).append(' ');
        }
        for (Candidate.CareerEntry e : c.careerHistory()) {
            sb.append(e.title()).append(' ')
              .append(e.description()).append(' ')
              .append(e.industry()).append(' ');
        }
        return sb.toString().toLowerCase();
    }

    /** Only the role titles (current + history), lower-cased. Used for title-class judgement. */
    public static String titleText(Candidate c) {
        StringBuilder sb = new StringBuilder(128);
        if (c.profile != null) sb.append(c.profile.title()).append(' ');
        for (Candidate.CareerEntry e : c.careerHistory()) sb.append(e.title()).append(' ');
        return sb.toString().toLowerCase();
    }

    /** Skill names joined, lower-cased. This is the keyword-stuffing surface we deliberately distrust. */
    public static String skillText(Candidate c) {
        StringBuilder sb = new StringBuilder(256);
        for (Candidate.Skill s : c.skills()) sb.append(s.name()).append(' ');
        return sb.toString().toLowerCase();
    }

    /** Days since last_active, measured from {@link #REFERENCE_DATE}. Large = stale. {@code Long.MAX} if unparseable. */
    public static long daysSinceActive(Candidate c) {
        LocalDate d = parse(c.signals().last_active_date);
        if (d == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(d, REFERENCE_DATE);
    }

    public static LocalDate parse(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDate.parse(iso.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Sum of career-history durations in months (used for internal-consistency / honeypot checks). */
    public static int totalCareerMonths(Candidate c) {
        int m = 0;
        for (Candidate.CareerEntry e : c.careerHistory()) m += Math.max(0, e.duration_months);
        return m;
    }
}
