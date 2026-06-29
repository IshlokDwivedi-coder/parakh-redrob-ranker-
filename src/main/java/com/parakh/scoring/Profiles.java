package com.parakh.scoring;

import com.parakh.model.Candidate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Shared helpers over a Candidate: the lower-cased text blobs the scorers search through, plus
 * some safe date math. Keeping them here means every scorer works off the same normalised strings.
 */
public final class Profiles {

    private Profiles() {}

    /**
     * A fixed "today" so the run is reproducible. The output has to be identical in the judges'
     * offline Docker run, so we measure recency against this constant instead of LocalDate.now(),
     * which would change from one day to the next. Set just after the latest activity in the dataset.
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
}
