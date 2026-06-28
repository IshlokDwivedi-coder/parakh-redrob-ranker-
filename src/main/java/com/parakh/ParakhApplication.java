package com.parakh;

import com.parakh.engine.RankedCandidate;
import com.parakh.engine.RankingEngine;
import com.parakh.io.DashboardWriter;
import com.parakh.io.SubmissionWriter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * PARAKH (परख — "to assess true quality"): a transparent, deterministic, network-free candidate
 * ranker for the Redrob Intelligent Candidate Discovery & Ranking Challenge.
 *
 * <p>Runs as a one-shot CLI:
 * <pre>  java -jar parakh.jar &lt;input.jsonl&gt; &lt;output.csv&gt;</pre>
 * Defaults: {@code ./candidates.jsonl} -> {@code ./submission.csv}.
 */
@SpringBootApplication
public class ParakhApplication implements CommandLineRunner {

    private final RankingEngine engine;
    private final SubmissionWriter writer;
    private final DashboardWriter dashboardWriter;

    public ParakhApplication(RankingEngine engine, SubmissionWriter writer, DashboardWriter dashboardWriter) {
        this.engine = engine;
        this.writer = writer;
        this.dashboardWriter = dashboardWriter;
    }

    public static void main(String[] args) {
        SpringApplication.run(ParakhApplication.class, args);
    }

    @Override
    public void run(String... args) {
        Path input = Path.of(args.length > 0 ? args[0] : "candidates.jsonl");
        Path output = Path.of(args.length > 1 ? args[1] : "submission.csv");
        // Optional 3rd arg: write the explainability dashboard data here. Omitting it keeps the
        // contest reproduce command (input + output only) byte-for-byte identical.
        Path dashboardData = args.length > 2 ? Path.of(args[2]) : null;

        if (!Files.exists(input)) {
            System.err.println("ERROR: input file not found: " + input.toAbsolutePath());
            System.err.println("Usage: java -jar parakh.jar <input.jsonl> <output.csv> [dashboard-data.js]");
            return;
        }

        long start = System.nanoTime();
        System.out.println("PARAKH ranking: " + input.toAbsolutePath());
        RankingEngine.Result result = engine.rank(input);
        writer.write(output, result.top());
        if (dashboardData != null) {
            dashboardWriter.write(dashboardData, result);
            System.out.println("Dashboard data written to " + dashboardData.toAbsolutePath());
        }
        double secs = (System.nanoTime() - start) / 1e9;

        System.out.printf("Scored %,d candidates | rejected %,d honeypots | top-100 written to %s | %.1fs%n",
                result.totalScored(), result.honeypotsRejected(), output.toAbsolutePath(), secs);
        printTop(result.top(), 10);
    }

    private static void printTop(List<RankedCandidate> top, int n) {
        System.out.println("\n--- Top " + n + " ---");
        int r = 1;
        for (RankedCandidate rc : top) {
            if (r > n) break;
            System.out.printf("%2d. %s  score=%.4f%n    %s%n", r++, rc.candidateId(), rc.score(), rc.reasoning());
        }
    }
}
