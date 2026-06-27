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
 * Writes the exact submission CSV the official validator demands: UTF-8, header
 * {@code candidate_id,rank,score,reasoning}, then 100 rows, rank 1..100, score non-increasing,
 * ties broken by candidate_id ascending. The ranked list arrives already in that order from the
 * engine; here we just number it and RFC-4180-quote the reasoning (which can contain commas).
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

    /** RFC-4180 quote: wrap in quotes, double any internal quotes, strip newlines. */
    private static String csv(String field) {
        String s = field == null ? "" : field.replace("\r", " ").replace("\n", " ");
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
