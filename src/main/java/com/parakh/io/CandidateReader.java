package com.parakh.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.parakh.model.Candidate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * Streams candidates.jsonl one line at a time so the 465MB / 100k-row file never lands in memory at
 * once — well within the 16GB / 5-minute compute budget. Supports plain {@code .jsonl} and
 * {@code .jsonl.gz}. Malformed lines are skipped (and counted) rather than aborting the whole run.
 */
@Service
public class CandidateReader {

    private final ObjectReader reader = new ObjectMapper().readerFor(Candidate.class);

    /** Invokes {@code sink} for each parseable candidate. Returns the number of candidates read. */
    public long stream(Path path, Consumer<Candidate> sink) {
        long ok = 0, bad = 0, lineNo = 0;
        try (BufferedReader br = open(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                try {
                    Candidate c = reader.readValue(line);
                    if (c != null && c.candidate_id != null) {
                        sink.accept(c);
                        ok++;
                    } else {
                        bad++;
                    }
                } catch (Exception parse) {
                    bad++;
                    if (bad <= 5) {
                        System.err.println("[reader] skipped malformed line " + lineNo + ": " + parse.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading " + path + ": " + e.getMessage(), e);
        }
        if (bad > 0) System.err.println("[reader] " + bad + " line(s) skipped, " + ok + " candidates read.");
        return ok;
    }

    private static BufferedReader open(Path path) throws IOException {
        InputStream in = Files.newInputStream(path);
        if (path.toString().endsWith(".gz")) in = new GZIPInputStream(in, 1 << 16);
        return new BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8), 1 << 20);
    }
}
