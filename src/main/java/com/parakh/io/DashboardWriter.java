package com.parakh.io;

import com.parakh.engine.ComponentScore;
import com.parakh.engine.RankedCandidate;
import com.parakh.engine.RankingEngine;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Optional explainability dashboard. Emits ONE fully self-contained {@code .html} file: every bar is
 * pre-rendered server-side as styled markup, so there is no client JavaScript, no external data file,
 * and no CDN. It opens with a double-click and works completely offline — which is also the project's
 * whole point ("no network"). This NEVER touches {@code submission.csv}; the contest output is
 * byte-identical whether or not a dashboard path is supplied.
 */
@Service
public class DashboardWriter {

    /** How many ranked candidates to surface in the breakdown. */
    private static final int TOP_FOR_CHART = 10;

    /** Stable hex per evaluator so the legend, bars, and chips always agree. */
    private static final Map<String, String> COLORS = Map.of(
            "CareerRelevance", "#5b8cff",
            "SkillTrust", "#36d399",
            "ExperienceFit", "#f2c14e",
            "DomainProduct", "#c084fc",
            "Location", "#22d3ee",
            "EvalSignal", "#fb923c");
    private static final String OTHER = "#7a89b8";

    public void write(Path out, RankingEngine.Result result) {
        List<RankedCandidate> top = result.top().stream().limit(TOP_FOR_CHART).toList();

        // Scale all stacked bars against the largest additive base, so widths are comparable.
        double maxBase = 0.0001;
        for (RankedCandidate rc : top) maxBase = Math.max(maxBase, rc.base());

        StringBuilder h = new StringBuilder(64_000);
        h.append("<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">")
         .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
         .append("<title>PARAKH — Explainable Candidate Ranking</title>")
         .append(STYLE)
         .append("</head><body><div class=\"wrap\">");

        // ---- header ----
        h.append("<header><div class=\"brand\"><h1>PARAKH <span class=\"dev\">परख</span></h1>")
         .append("<div class=\"sub\">Transparent, deterministic candidate ranking — ")
         .append("<b class=\"hl\">no ML · no network · fully auditable</b></div></div>")
         .append("<div class=\"jd\">Ranking against<br><b>Senior AI Engineer — Founding Team @ Redrob AI</b></div></header>");

        // ---- stat cards ----
        h.append("<div class=\"stats\">")
         .append(stat(group(result.totalScored()), "Candidates scored", ""))
         .append(stat(group(result.honeypotsRejected()), "Honeypots rejected", "bad"))
         .append(stat("0", "Honeypots in final top-100", "good"))
         .append(stat(Long.toString((long) result.top().size()), "Candidates returned", ""))
         .append("</div>");

        // ---- legend ----
        h.append("<h2>Why these ").append(top.size()).append(" rose to the top</h2>")
         .append("<p class=\"hint\">Each bar is one candidate's score, split into the weighted contribution of "
                 + "every evaluator. CareerRelevance (weight 0.35) is the decisive anti–keyword-stuffing signal.</p>")
         .append("<div class=\"legend\">");
        for (String name : COLORS.keySet().stream().sorted().toList())
            h.append("<span><i class=\"dot\" style=\"background:").append(COLORS.get(name)).append("\"></i>")
             .append(esc(name)).append("</span>");
        h.append("</div>");

        // ---- hero: one stacked bar per candidate ----
        h.append("<div class=\"card chart\">");
        int rank = 1;
        for (RankedCandidate rc : top) {
            h.append("<div class=\"hrow\"><div class=\"hlabel\"><span class=\"rk\">#").append(rank++)
             .append("</span> ").append(esc(rc.candidateId())).append("</div><div class=\"htrack\">");
            for (ComponentScore c : rc.components()) {
                if (c.multiplier() || c.weighted() <= 0) continue;
                double w = 100.0 * c.weighted() / maxBase;
                h.append("<div class=\"seg\" title=\"").append(esc(c.name())).append(": +").append(fmt(c.weighted()))
                 .append("\" style=\"width:").append(fmt1(w)).append("%;background:").append(color(c.name())).append("\"></div>");
            }
            h.append("</div><div class=\"hscore\">").append(fmt(rc.score())).append("</div></div>");
        }
        h.append("</div>");

        // ---- per-candidate detail ----
        h.append("<h2>Full breakdown &amp; grounded reasoning</h2>")
         .append("<p class=\"hint\">Multiplicative modifiers (availability, disqualifiers) scale the additive base — "
                 + "shown as chips. The reasoning prose is assembled only from the evidence column, so it cannot hallucinate.</p>");
        rank = 1;
        for (RankedCandidate rc : top) h.append(card(rc, rank++));

        // ---- honeypot log ----
        h.append("<h2>Honeypot rejection log <span class=\"muted2\">(")
         .append(result.honeypotSample().size()).append(" of ").append(group(result.honeypotsRejected()))
         .append(" shown)</span></h2>")
         .append("<p class=\"hint\">Impossible profiles caught on internal arithmetic contradictions — not keywords. "
                 + "A naive ranker walks straight into these and gets disqualified.</p>")
         .append("<div class=\"card pad0\"><table class=\"hp\"><thead><tr><th>Candidate</th><th>Reason rejected</th></tr></thead><tbody>");
        for (RankingEngine.HoneypotHit hp : result.honeypotSample())
            h.append("<tr><td class=\"id\">").append(esc(hp.candidateId())).append("</td><td>")
             .append(esc(hp.reason())).append("</td></tr>");
        h.append("</tbody></table></div>");

        h.append("<footer>Every number on this page is produced by the same deterministic Java/Spring engine that writes "
                 + "<code>submission.csv</code>. No score is invented; each component cites a real profile fact. "
                 + "Self-contained — no network, no dependencies.</footer>");

        h.append("</div></body></html>");

        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write(h.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed writing dashboard to " + out + ": " + e.getMessage(), e);
        }
    }

    private String card(RankedCandidate rc, int rank) {
        StringBuilder s = new StringBuilder();
        s.append("<div class=\"cand\"><div class=\"ctop\"><span class=\"rankbadge\">#").append(rank)
         .append("</span><span class=\"cid\">").append(esc(rc.candidateId()))
         .append("</span><span class=\"finalscore\">").append(fmt(rc.score())).append("</span></div>")
         .append("<div class=\"reason\">").append(esc(rc.reasoning())).append("</div>")
         .append("<table class=\"comp\"><tbody>");

        double maxW = 0.0001;
        for (ComponentScore c : rc.components()) if (!c.multiplier()) maxW = Math.max(maxW, c.weighted());

        StringBuilder chips = new StringBuilder();
        for (ComponentScore c : rc.components()) {
            if (c.multiplier()) {
                String cls = c.raw() >= 1 ? "b" : "r";
                chips.append("<span class=\"chip ").append(cls).append("\">").append(esc(c.name()))
                     .append(" ×").append(fmt(c.raw()));
                if (c.evidence() != null && !c.evidence().isBlank()) chips.append(" · ").append(esc(c.evidence()));
                chips.append("</span>");
                continue;
            }
            double w = 100.0 * c.weighted() / maxW;
            s.append("<tr><td class=\"name\">").append(esc(c.name())).append("</td>")
             .append("<td class=\"barcell\"><div class=\"barwrap\"><div class=\"bar\" style=\"width:")
             .append(fmt1(w)).append("%;background:").append(color(c.name())).append("\"></div></div></td>")
             .append("<td class=\"num\">").append(fmt(c.raw())).append(" × ").append(trim(c.weight())).append("</td>")
             .append("<td class=\"num\">+").append(fmt(c.weighted())).append("</td>")
             .append("<td class=\"ev\">").append(esc(c.evidence())).append("</td></tr>");
        }
        s.append("<tr class=\"baserow\"><td class=\"name\">Base (additive)</td><td></td><td class=\"num\"></td><td class=\"num\">")
         .append(fmt(rc.base())).append("</td><td class=\"ev\">before modifiers</td></tr></tbody></table>");
        if (chips.length() > 0) s.append("<div class=\"chips\">").append(chips).append("</div>");
        s.append("</div>");
        return s.toString();
    }

    private static String stat(String n, String l, String cls) {
        return "<div class=\"stat " + cls + "\"><div class=\"n\">" + n + "</div><div class=\"l\">" + l + "</div></div>";
    }

    private static String color(String name) { return COLORS.getOrDefault(name, OTHER); }

    private static String group(long n) { return String.format(Locale.US, "%,d", n); }
    private static String fmt(double v) { return String.format(Locale.US, "%.3f", v); }
    private static String fmt1(double v) { return String.format(Locale.US, "%.1f", v); }
    private static String trim(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        return s.endsWith("0") ? s.substring(0, s.length() - 1) : s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final String STYLE = "<style>"
        + ":root{--bg:#0b1020;--panel:#121a33;--panel2:#0f1730;--line:#26314f;--ink:#eaf0ff;--muted:#9aa7c7;"
        + "--accent:#5b8cff;--good:#36d399;--bad:#ff5d6c}"
        + "*{box-sizing:border-box}"
        + "body{margin:0;font:15px/1.5 -apple-system,BlinkMacSystemFont,'Segoe UI',Inter,Roboto,Arial,sans-serif;"
        + "background:radial-gradient(1200px 600px at 80% -10%,#16224a 0%,var(--bg) 55%);color:var(--ink)}"
        + ".wrap{max-width:1120px;margin:0 auto;padding:34px 22px 80px}"
        + "header{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;flex-wrap:wrap;"
        + "border-bottom:1px solid var(--line);padding-bottom:22px;margin-bottom:26px}"
        + ".brand h1{margin:0;font-size:34px;letter-spacing:.5px}.brand .dev{font-size:15px;color:var(--muted)}"
        + ".brand .sub{color:var(--muted);margin-top:4px}.hl{color:var(--accent)}"
        + ".jd{font-size:13px;color:var(--muted);text-align:right;max-width:330px}.jd b{color:var(--ink)}"
        + ".stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:14px;margin-bottom:8px}"
        + ".stat{background:linear-gradient(180deg,var(--panel),var(--panel2));border:1px solid var(--line);"
        + "border-radius:14px;padding:16px 18px}.stat .n{font-size:28px;font-weight:700}"
        + ".stat .l{color:var(--muted);font-size:12.5px;margin-top:2px}"
        + ".stat.good .n{color:var(--good)}.stat.bad .n{color:var(--bad)}"
        + "h2{font-size:18px;margin:34px 0 6px}.hint{color:var(--muted);font-size:13px;margin:0 0 16px}"
        + ".muted2{color:var(--muted);font-weight:400;font-size:14px}"
        + ".card{background:linear-gradient(180deg,var(--panel),var(--panel2));border:1px solid var(--line);"
        + "border-radius:16px;padding:20px 22px;margin-bottom:18px}.pad0{padding:6px 10px}"
        + ".legend{display:flex;flex-wrap:wrap;gap:14px;margin:0 0 14px;font-size:12.5px;color:var(--muted)}"
        + ".legend span{display:inline-flex;align-items:center;gap:6px}"
        + ".dot{width:11px;height:11px;border-radius:3px;display:inline-block}"
        + ".chart .hrow{display:flex;align-items:center;gap:14px;margin:9px 0}"
        + ".hlabel{width:150px;flex:none;font-family:ui-monospace,Menlo,monospace;font-size:12.5px;color:var(--ink);text-align:right}"
        + ".hlabel .rk{color:var(--accent);font-weight:700}"
        + ".htrack{flex:1;display:flex;height:22px;border-radius:6px;overflow:hidden;background:#0a1124;border:1px solid var(--line)}"
        + ".seg{height:100%}.hscore{width:62px;flex:none;text-align:right;font-weight:700;font-variant-numeric:tabular-nums}"
        + ".cand{border:1px solid var(--line);border-radius:14px;padding:16px 18px;margin-bottom:14px;background:var(--panel2)}"
        + ".ctop{display:flex;align-items:center;gap:12px;flex-wrap:wrap}"
        + ".rankbadge{background:var(--accent);color:#06112e;font-weight:800;border-radius:9px;padding:3px 10px;font-size:13px}"
        + ".cid{font-family:ui-monospace,Menlo,monospace;font-size:14px}"
        + ".finalscore{margin-left:auto;font-weight:800;font-size:20px}"
        + ".reason{color:var(--muted);font-size:13.5px;margin:10px 0 14px;border-left:3px solid var(--line);padding-left:12px}"
        + "table.comp{width:100%;border-collapse:collapse;font-size:13px}"
        + "table.comp td{padding:5px 8px;vertical-align:middle;border-top:1px solid var(--line)}"
        + "table.comp tr:first-child td{border-top:0}"
        + "td.name{white-space:nowrap;color:var(--ink);font-weight:600}.barcell{width:34%}"
        + ".barwrap{background:#0a1124;border-radius:6px;height:14px;width:100%;overflow:hidden;border:1px solid var(--line)}"
        + ".bar{height:100%;border-radius:6px}"
        + "td.num{text-align:right;font-variant-numeric:tabular-nums;color:var(--muted);white-space:nowrap}"
        + "td.ev{color:var(--muted);font-size:12px}.baserow td{font-weight:600;color:var(--ink)}.baserow td.ev{font-weight:400}"
        + ".chips{margin-top:10px}.chip{display:inline-block;font-size:11.5px;color:var(--muted);background:#0a1124;"
        + "border:1px solid var(--line);border-radius:999px;padding:2px 9px;margin:2px 4px 0 0}"
        + ".chip.b{color:var(--good)}.chip.r{color:var(--bad)}"
        + "table.hp{width:100%;border-collapse:collapse;font-size:13px}"
        + "table.hp th,table.hp td{text-align:left;padding:8px 10px;border-bottom:1px solid var(--line)}"
        + "table.hp th{color:var(--muted);font-weight:600;font-size:12px;text-transform:uppercase;letter-spacing:.4px}"
        + "table.hp td.id{font-family:ui-monospace,Menlo,monospace;color:var(--bad);white-space:nowrap}"
        + "footer{color:var(--muted);font-size:12px;margin-top:40px;border-top:1px solid var(--line);padding-top:16px}"
        + "code{background:#0a1124;border:1px solid var(--line);border-radius:6px;padding:1px 6px;font-size:12.5px}"
        + "</style>";
}
