# syntax=docker/dockerfile:1
#
# PARAKH — reproducible, network-free candidate ranker.
#
# Two stages:
#   1) build   — JDK + Gradle wrapper compile the fat jar. Network is used HERE
#                (and only here) to download Gradle + Maven dependencies.
#   2) runtime — a slim JRE that runs the ranking step. The ranking step itself
#                makes NO network calls, so judges can run it with `--network none`.
#
# Build:   docker build -t parakh .
# Run:     docker run --rm --network none -m 16g \
#            -v "$PWD/data:/data" parakh /data/candidates.jsonl /data/submission.csv
#
# ---------------------------------------------------------------------------
# Stage 1: build the jar (network allowed — dependency download only)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy the Gradle wrapper + build scripts first so dependency resolution is
# cached as its own layer and is not re-run on every source edit.
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now copy sources and build the fat jar -> build/libs/parakh.jar
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---------------------------------------------------------------------------
# Stage 2: runtime (no build tools, no source, just a JRE + the jar)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Heap is bounded well under the 16 GB limit; the ranker streams JSONL and keeps
# only a size-100 min-heap in memory, so it needs very little. Override with -e
# JAVA_OPTS="-Xmx8g" if you mount an unusually large file.
ENV JAVA_OPTS="-Xmx4g"

COPY --from=build /app/build/libs/parakh.jar ./parakh.jar

# Default I/O paths assume a volume mounted at /data. Override by passing
# different positional args to `docker run`.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/parakh.jar \"$@\"", "--"]
CMD ["/data/candidates.jsonl", "/data/submission.csv"]
