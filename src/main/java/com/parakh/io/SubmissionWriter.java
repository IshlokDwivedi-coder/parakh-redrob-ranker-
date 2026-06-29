package com.parakh.io;

import com.parakh.engine.RankedCandidate;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes the submission CSV exactly the way the official validator wants it: UTF-8, the header
 * candidate_id,rank,score,reasoning, then 100 rows with rank 1..100, scores non-increasing, and
 * ties broken by candidate_id. The list already comes in that order from the engine, so here we
 * just number the rows and quote the reasoning properly (it can contain commas).
 */
@Service
public class SubmissionWriter {

    public void write(Path out, List<RankedCandidate> ranked) {
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("candidate_id,rank,score,reasoning");
            w.newLine();
            int rank = 1;
            for (RankedCandidate rc : ranked) {
                w.write(rc.candidateId());
                w.write(',');
                w.write(Integer.toString(rank++));
                w.write(',');
                w.write(String.format("%.6f", rc.score()));
                w.write(',');
                w.write(csv(rc.reasoning()));
                w.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed writing submission to " + out + ": " + e.getMessage(), e);
        }
    }

    /** CSV-quote a field: wrap it in quotes, double any quotes inside, drop newlines. */
    private static String csv(String field) {
        String s = field == null ? "" : field.replace("\r", " ").replace("\n", " ");
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
