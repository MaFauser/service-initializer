# Why tests pass locally but fail in CI

Common reasons and what to check.

## 1. **Different code / fixes not pushed**

The pipeline runs the code on the branch. If you fixed something locally (e.g. deprecation, imports) but didn't push, CI still runs the old code. **Fix:** Push your latest commits and re-run the workflow.

## 2. **Deprecated / stricter APIs in CI**

CI often uses a clean environment (new Gradle cache, same or newer JDK). Deprecated APIs can behave differently or emit errors there while still working locally.
**Example:** `content().json(String, boolean)` was deprecated; we switched to `content().json(String, JsonCompareMode.LENIENT)`.
**Fix:** Remove deprecation warnings and use the recommended APIs so local and CI behave the same.

## 3. **Testcontainers / Docker**

Integration tests start PostgreSQL, Redis, and Kafka via Testcontainers. GitHub's `ubuntu-latest` has Docker, so Testcontainers usually works, but:

- **Slower:** CI has less CPU/memory; containers start slower. Flaky timeouts can appear only in CI.
- **Network:** Image pulls or DNS can fail occasionally in the runner.
- **No Docker:** If the job runs in an environment without Docker (e.g. wrong runner), Testcontainers will fail.

**Fix:** Re-run the workflow (transient failures). If it keeps failing, check the logs for Testcontainers/Docker errors.

## 4. **Gradle / JVM differences**

- **Cache:** CI often starts with an empty Gradle cache; first run is slower and can hit timeouts or different resolution.
- **Parallelism:** `./gradlew build` can run more tasks in parallel; test order can differ and expose shared-state bugs.
- **JDK:** Same major version (e.g. 21) is used, but the exact build can differ; rare but can affect reflection or deprecated API behavior.

**Fix:** Use the same Gradle and JDK version locally as in the workflow (see `pr-validation.yml`).

## 5. **What we fixed that could have broken CI**

- Replaced deprecated `content().json(String, boolean)` with `content().json(String, JsonCompareMode.LENIENT)` in REST integration tests (avoids deprecation-related issues on CI).
- Kept `jacksonObjectMapper()` in the REST test instead of injecting `ObjectMapper` (avoids "no qualifying bean" in the MOCK web environment, which can differ between local and CI).

---

**Bottom line:** After pushing the latest changes (deprecation fix, package naming, etc.), re-run the PR workflow. If it still fails, open the "Build & Test" job logs and check the exact failure (test name, exception, Testcontainers/Docker errors).
