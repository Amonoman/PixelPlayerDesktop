# Contributing to PixelPlayer Desktop

Thank you for your interest in contributing to PixelPlayer Desktop! Whether you are fixing bugs, proposing new features, improving documentation, or optimizing performance, your contributions are greatly appreciated.

---

## Code of Conduct & Standards

- Be respectful and constructive in all discussions, issues, and pull requests.
- Ensure all code conforms to Kotlin coding standards and project formatting.
- Respect user privacy: PixelPlayer Desktop is an offline-first application with zero telemetry.

---

## Development Prerequisites

Before setting up the project, make sure you have the following installed:

- **JDK 21** (Eclipse Temurin or OpenJDK recommended)
- **Git**
- **libmpv** runtime:
  - **Linux (Ubuntu/Debian)**: `sudo apt install libmpv2 libmpv-dev mpv`
  - **Linux (Fedora)**: `sudo dnf install mpv-libs mpv`
  - **macOS**: `brew install mpv`
  - **Windows**: Bundled automatically by the Gradle build scripts during execution.

---

## Getting Started

1. **Fork the repository** on GitHub.
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/adlifarizi/PixelPlayerDesktop.git
   cd PixelPlayerDesktop
   ```
3. **Run the desktop app** in development mode:
   ```bash
   # Linux / macOS
   ./gradlew run

   # Windows
   .\gradlew.bat run
   ```

---

## Building & Verification

Before submitting any code changes, ensure that your code compiles, builds distributable artifacts, and passes all checks:

```bash
# Run tests and checks
./gradlew check

# Package application locally for your OS
./gradlew createDistributable
```

---

## Commit Guidelines

We follow [Conventional Commits](https://www.conventionalcommits.org/) to maintain a clean git history and power automated changelog generation:

| Type | Purpose | Example |
| --- | --- | --- |
| `feat` | New user-facing feature or enhancement | `feat: add word-level synchronized lyrics support` |
| `fix` | Bug fix | `fix: restore persisted volume on startup` |
| `perf` | Performance optimization | `perf: optimize artwork decoding and memory caching` |
| `refactor` | Code restructuring without behavioral change | `refactor: migrate audio engine from vlcj to libmpv` |
| `build` | Build system, packaging, or dependency updates | `build: streamline desktop packaging and jvm options` |
| `ci` | CI/CD workflow updates | `ci: create github releases as draft` |
| `docs` | Documentation updates | `docs: polish and modernize README structure` |
| `chore` | General maintenance tasks | `chore: update license headers` |

---

## Submitting Pull Requests

1. **Create a branch** for your feature or fix from `main`:
   ```bash
   git checkout -b feat/my-awesome-feature
   ```
2. **Make your changes** and keep commits atomic and well-described.
3. **Run verification** (`./gradlew check`) to make sure tests pass.
4. **Push the branch** to your fork:
   ```bash
   git push origin feat/my-awesome-feature
   ```
5. **Open a Pull Request** against the `main` branch:
   - Provide a concise summary of the changes and motivation.
   - Attach screenshots or recordings if the changes affect the UI.
   - Link any related issues (`Fixes #123`).

---

## Reporting Issues & Feature Requests

- **Bug Reports**: Include your OS version, steps to reproduce, expected vs. actual behavior, and relevant logs if available.
- **Feature Requests**: Describe the problem you're trying to solve, why the feature is needed, and any alternative solutions considered.
