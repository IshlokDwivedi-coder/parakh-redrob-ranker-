# PARAKH — परख

> *parakh* (परख): "to assess, to gauge the true quality of something."

A **transparent, deterministic, network-free** candidate ranker for the Redrob ×
Hack2Skill **Intelligent Candidate Discovery & Ranking** challenge. It ranks the
top **100** candidates out of **100,000** for the *Senior AI Engineer — Founding
Team* role using an explainable panel of feature-engineered scorers — **no
embeddings, no ML model weights, and zero network calls during ranking.**

Every score is fully decomposable and every line of the `reasoning` column is
assembled **only** from real fields on the candidate's profile, so hallucination
is impossible by construction.

---

## TL;DR — reproduce the submission

**Single command (local, requires JDK 17):**

```bash
./gradlew bootJar && java -jar build/libs/parakh.jar <path-to>/candidates.jsonl ./submission.csv
```

**Single command (Docker — no JDK needed, runs with the network physically off):**

```bash
# 1. put the contest file at ./data/candidates.jsonl
mkdir -p data && cp <path-to>/candidates.jsonl ./data/

# 2. build once (this is the ONLY step that uses the network — dependency download)
docker build -t parakh .

# 3. run the ranking step with networking disabled, RAM capped at the contest limit
docker run --rm --network none -m 16g -v "$PWD/data:/data" parakh \
  /data/candidates.jsonl /data/submission.csv
```

`./data/submission.csv` is the contest deliverable. Validate it with the
organizer's script:

```bash
python3 validate_submission.py ./data/submission.csv   # -> "Submission is valid."
```

---

## Measured performance (full 100,000-candidate pool)

Run on a MacBook (Apple Silicon), JDK 17, single process:

| Metric | Limit | PARAKH |
| --- | --- | --- |
| Candidates scored | 100,000 | **100,000** |
| Wall-clock (ranking step) | ≤ 5 min | **~22 s** |
| Peak memory (RSS) | ≤ 16 GB | **~0.28 GB** |
| GPU | none | none |
| Network during ranking | off | **off** (`--network none` verified) |
| Honeypots forced out of top 100 | DQ if > 10% | **5,634 rejected; 0 in final top 100** |

The ranker **streams** `candidates.jsonl` line-by-line through Jackson and keeps
only a size-100 min-heap of the best candidates, so memory is flat regardless of
pool size — there is no in-memory materialisation of the 100k records.

---

## How it ranks (methodology)

A pipeline of explainable `@Service` evaluators. Each contributes a bounded,
named component to a composite score; a final multiplicative stage applies hard
penalties and behavioural modifiers:

1. **HoneypotGate** — detects impossible / contradictory profiles (the ~80
   planted honeypots and obvious fabrications) and forces them out of the top
   100 before scoring. *DQ-prevention.*
2. **CareerRelevanceScorer** (~0.35) — title + trajectory match. The decisive
   anti-keyword-stuffing signal: a real ranking/retrieval career beats a profile
   that merely lists the right nouns.
3. **SkillTrustScorer** (~0.20) — skill match weighted by a *trust* function of
   endorsements, skill duration, and assessment score, so padded skill lists
   don't win.
4. **DomainProductScorer** (~0.15) — product-company vs services-only, and
   NLP/IR vs CV/speech/robotics, per the JD.
5. **ExperienceFitScorer** (~0.10) — years-of-experience fit to the band.
6. **LocationScorer** (~0.10) — Pune/Noida or willing-to-relocate.
7. **EvalSignalScorer** (~0.10) — nice-to-have evidence (ranking-eval
   familiarity, shipped recsys/search, etc.).
8. **DisqualifierPenalizer** — multiplicative penalty for JD disqualifiers
   (services-only career, recent-LangChain-only, no-code gaps, title-chasing,
   research-only-no-prod, needs-visa).
9. **AvailabilityModifier** — multiplicative behavioural modifier from recency,
   response rate, open-to-work, and notice period (down-weights the unavailable).

`ReasoningComposer` then writes a grounded, per-candidate explanation using only
the values that actually drove the score — see the `reasoning` column in
`submission.csv`.

Top-10 quality is **half** the composite metric (0.50·NDCG@10 + 0.30·NDCG@50 +
0.15·MAP + 0.05·P@10), so the panel is tuned for precision at the very top.

---

## Output format

`submission.csv` — header + exactly 100 rows:

```
candidate_id,rank,score,reasoning
CAND_0077337,1,0.772900,"Staff Machine Learning Engineer @ Paytm, 7.0 yrs. ..."
```

`rank` is 1..100 (each once), `score` is non-increasing, equal scores are
tie-broken by `candidate_id` ascending — conformant with the organizer's
`validate_submission.py`.

---

## Project layout

```
src/main/java/com/parakh/
  ParakhApplication.java        CLI entrypoint (CommandLineRunner)
  model/Candidate.java          JSONL record model
  io/CandidateReader.java       streaming JSONL reader
  io/SubmissionWriter.java      CSV writer (spec-conformant)
  scoring/                      the evaluator panel + Lexicon/Profiles knowledge base
  engine/RankingEngine.java     streaming rank + size-100 min-heap
  engine/ReasoningComposer.java grounded reasoning text
```

## Requirements

- JDK 17 (toolchain pinned in `build.gradle`; Gradle 8.10.2 via the wrapper), **or**
- Docker (no local JDK required — see the Docker recipe above).

Dependencies (Spring Boot 3.3.5 + Jackson) are declared in `build.gradle` and
resolved from Maven Central at build time only.

## AI tools

Built with AI assistance (see `submission_metadata.yaml`). **No candidate data
was ever sent to any LLM** — the ranker is pure deterministic Java and makes no
network calls at all during ranking.
