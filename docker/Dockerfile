# =====================================================================
#  OmiinQA :: Test-runner image
#  Multi-stage: stage 1 warms the Maven dependency cache, stage 2 is the
#  lean runtime that executes the suite against a Grid.
# =====================================================================

# ---- Stage 1: dependency cache ----
FROM maven:3.9.15-eclipse-temurin-26 AS deps
WORKDIR /build
COPY pom.xml .
# Resolve dependencies offline-friendly so subsequent builds are cached.
RUN mvn -B -q dependency:go-offline

# ---- Stage 2: runner ----
FROM maven:3.9.15-eclipse-temurin-26
LABEL org.opencontainers.image.title="OmiinQA Selenium Framework" \
      org.opencontainers.image.source="https://github.com/omiinayak25/SELQA"
WORKDIR /omiinqa

# Reuse the warmed local repository.
COPY --from=deps /root/.m2 /root/.m2
COPY . .

# Default: run the UI suite against a Grid hub reachable as 'selenium-hub'.
ENV EXECUTION_MODE=grid \
    GRID_URL=http://selenium-hub:4444/wd/hub \
    BROWSER=remote-chrome \
    SUITE=testng-ui.xml

ENTRYPOINT ["sh", "-c", "mvn -B test -Dsuite.file=${SUITE} -Dexecution.mode=${EXECUTION_MODE} -Dgrid.url=${GRID_URL} -Dbrowser=${BROWSER}"]
