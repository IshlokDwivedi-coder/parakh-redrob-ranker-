package com.parakh.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One candidate, parsed straight from a line of candidates.jsonl. @JsonIgnoreProperties lets us
 * ignore fields we don't use, and the list/map getters return empty instead of null so the scorers
 * don't have to null-check everywhere. The nested classes keep the profile shape in one file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candidate {

    public String candidate_id;
    public Profile profile;
    public List<CareerEntry> career_history;
    public List<Education> education;
    public List<Skill> skills;
    public List<Certification> certifications;
    public RedrobSignals redrob_signals;

    public List<CareerEntry> careerHistory() {
        return career_history == null ? Collections.emptyList() : career_history;
    }

    public List<Education> education() {
        return education == null ? Collections.emptyList() : education;
    }

    public List<Skill> skills() {
        return skills == null ? Collections.emptyList() : skills;
    }

    public RedrobSignals signals() {
        return redrob_signals == null ? new RedrobSignals() : redrob_signals;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        public String anonymized_name;
        public String headline;
        public String summary;
        public String location;
        public String country;
        public double years_of_experience;
        public String current_title;
        public String current_company;
        public String current_company_size;
        public String current_industry;

        public String headline() { return headline == null ? "" : headline; }
        public String summary() { return summary == null ? "" : summary; }
        public String title() { return current_title == null ? "" : current_title; }
        public String company() { return current_company == null ? "" : current_company; }
        public String industry() { return current_industry == null ? "" : current_industry; }
        public String location() { return location == null ? "" : location; }
        public String country() { return country == null ? "" : country; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CareerEntry {
        public String company;
        public String title;
        public String start_date;
        public String end_date;
        public int duration_months;
        public boolean is_current;
        public String industry;
        public String company_size;
        public String description;

        public String title() { return title == null ? "" : title; }
        public String company() { return company == null ? "" : company; }
        public String industry() { return industry == null ? "" : industry; }
        public String description() { return description == null ? "" : description; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Education {
        public String institution;
        public String degree;
        public String field_of_study;
        public int start_year;
        public int end_year;
        public String grade;
        public String tier;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Skill {
        public String name;
        public String proficiency;
        public int endorsements;
        public int duration_months;

        public String name() { return name == null ? "" : name; }
        public String proficiency() { return proficiency == null ? "" : proficiency; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Certification {
        public String name;
        public String issuer;
        public int year;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SalaryRange {
        public double min;
        public double max;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedrobSignals {
        public double profile_completeness_score;
        public String signup_date;
        public String last_active_date;
        public boolean open_to_work_flag;
        public int profile_views_received_30d;
        public int applications_submitted_30d;
        public double recruiter_response_rate;
        public double avg_response_time_hours;
        public Map<String, Double> skill_assessment_scores;
        public int connection_count;
        public int endorsements_received;
        public int notice_period_days;
        public SalaryRange expected_salary_range_inr_lpa;
        public String preferred_work_mode;
        public boolean willing_to_relocate;
        public double github_activity_score;
        public int search_appearance_30d;
        public int saved_by_recruiters_30d;
        public double interview_completion_rate;
        public double offer_acceptance_rate;
        public boolean verified_email;
        public boolean verified_phone;
        public boolean linkedin_connected;

        public Map<String, Double> assessments() {
            return skill_assessment_scores == null ? Collections.emptyMap() : skill_assessment_scores;
        }
    }
}
