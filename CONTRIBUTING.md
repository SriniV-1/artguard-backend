# Contributing to ArtGuard

Thanks for your interest in contributing! ArtGuard is a distributed, real-time
surveillance backend (Java 21 · Spring Boot · Kafka · gRPC · YOLOv8) and we
welcome bug reports, fixes, docs improvements, and well-scoped features.

## Ways to contribute

- **Report a bug** — open an issue using the bug report template, with steps to
  reproduce and your environment (JDK, OS, mode).
- **Fix a bug** — small, focused PRs are easiest to review and merge.
- **Improve docs** — README, architecture notes, and inline docstrings.
- **Propose a feature** — open a feature request issue first so we can discuss
  scope before you invest time.

## Development setup

```bash
# 1. Infra (Kafka KRaft + PostgreSQL + Redis)
docker compose up -d

# 2. Gateway (Java 21) — simulation mode by default
./gradlew :gateway:bootRun        # http://localhost:8080

# 3. Dashboard (React + Vite)
cd dashboard && npm install && npm run dev   # http://localhost:5173
```

Requires JDK 21 (the Gradle build pins a Java 21 toolchain), Docker, and Node 18+.

## Before you open a pull request

1. **Branch** from `main` (`git checkout -b fix/short-description`).
2. **Build and test** locally:
   ```bash
   ./gradlew build        # compiles + runs the test suite
   ```
3. **Keep it scoped** — one logical change per PR. Large refactors are hard to
   review; prefer several small PRs.
4. **Match the surrounding style** — the codebase uses standard Google-Java
   conventions and Javadoc on public types/methods.
5. **Describe the change** clearly in the PR: what, why, and how you verified it.

## Reporting security issues

Please do **not** open public issues for security vulnerabilities. See
[SECURITY.md](SECURITY.md) for responsible disclosure.

## Code of Conduct

By participating, you agree to uphold our [Code of Conduct](CODE_OF_CONDUCT.md).
