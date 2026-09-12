# Static Analysis / Code Smell Gate — Design

**Date:** 2026-09-11
**Project:** `project1_android_api` (Kotlin Android, Compose + Room + OkHttp)
**Assignment:** Add PMD and Error Prone to the Gradle build; wire `./gradlew check` into CI;
triage every finding; fix ≥5; suppress ≤3 with written reasons; open a passing PR; review a
classmate's PR; write a one-page report on which rules produced noise and which caught
something real.

---

## 1. The finding that shapes this design

**The assigned tools cannot analyse this codebase.** This is not an obstacle to work around —
it is the most defensible thing the write-up can be built on, provided it is demonstrated
rather than asserted.

Evidence, measured on this repository:

```
.java files under app/src:  0
.kt   files under app/src:  32

> Task :app:compileDebugJavaWithJavac NO-SOURCE
```

Two independent reasons:

| Tool | Why it finds nothing here |
|---|---|
| **Error Prone** | It is a **javac plugin**. It inspects the AST as the Java compiler builds it. `compileDebugJavaWithJavac` is `NO-SOURCE`, so there is no compilation for it to hook. |
| **PMD** | It parses **Java source sets**. Gradle's PMD plugin creates one `Pmd` task per Java source set; an Android module with only Kotlin has none, so no tasks are created. PMD's Kotlin support remains experimental and is not exposed by the Gradle plugin. |

Two corroborating details from the tool documentation:

- The Gradle PMD plugin is built on the Java plugin's model. Android modules use a different
  source-set model, which is why third-party plugins exist purely to bridge the two.
- `gradle-errorprone-plugin` states plainly: **"There's no specific support for the Android
  Gradle Plugin."** It creates an `errorprone` configuration but does not wire it to Android
  compile tasks automatically.

So even in a hypothetical Android project that *did* contain Java, both tools need manual
wiring. Here they additionally have zero input.

**Design consequence:** wire both tools anyway, exactly as assigned, and capture their empty
output as evidence. Add detekt — the Kotlin-native equivalent — to produce the findings that
steps 2–6 of the assignment require.

---

## 2. Goals and non-goals

**Goals**

1. Satisfy the assignment literally: PMD and Error Prone present in the Gradle build.
2. Produce a real, triageable set of findings on the Kotlin sources.
3. `./gradlew check` fails the build on violations (`ignoreFailures = false`).
4. A GitHub Actions workflow runs that gate on every push and pull request.
5. Exactly 5 fixes and at most 3 suppressions, each suppression carrying a written reason.
6. Evidence for the write-up: per-tool finding counts, and which rules were noise.

**Non-goals**

- Cleaning up every finding in the codebase. The assignment grades triage reasoning, not
  volume.
- Introducing a baseline file (see §4, Approach B rejected).
- Refactoring the Compose UI for its own sake.
- Adding ktlint/spotless formatting. Out of scope and would drown the signal.

---

## 3. Tool inventory — confirmed versions and compatibility

Versions resolved from Maven Central and the Gradle Plugin Portal on 2026-09-11.

| Tool | Version | Requirement | This project | Status |
|---|---|---|---|---|
| Gradle | 9.6.0 | — | 9.6.0 | ✅ |
| AGP | 9.3.2 | JDK 17+ | JBR 25 | ✅ |
| Kotlin | 2.2.10 | — | 2.2.10 | ✅ |
| **PMD** | 7.27.0 | Java source sets | none | ⚠️ *no tasks created — expected* |
| **Error Prone** | `error_prone_core` 2.50.0 | **JDK 21+** | JDK 25 | ✅ runs, but ⚠️ *no javac invocation* |
| `net.ltgt.errorprone` | 5.1.1 | Gradle 7.1+ | 9.6.0 | ✅ |
| **detekt (stable)** | 1.23.8 | built against **Kotlin 2.0.21** | Kotlin 2.2.10 | ⚠️ version skew |
| **detekt (alpha)** | 2.0.0-alpha.6 | built against Kotlin 2.2.20+ | Kotlin 2.2.10 | ✅ but pre-release |

### 3.1 JDK 16+ module encapsulation — already handled

Error Prone needs `--add-exports` / `--add-opens` to reach internal javac packages on JDK 16
and above. `gradle-errorprone-plugin` **detects this and forks the compiler with the required
JVM arguments automatically**, so no manual `jvmArgs` configuration is needed. Error Prone
2.43+ requires JDK 21; JBR 25 satisfies that.

### 3.2 The detekt version decision — top open risk

Stable detekt (1.23.8) is built against Kotlin 2.0.21; this project is on 2.2.10.

detekt bundles its own Kotlin parser, so **without type resolution** it will generally parse
newer sources fine — the compatibility table primarily governs type-resolution mode and a
classpath-conflict warning. This project uses no exotic Kotlin 2.2 syntax, so 1.23.8 is
plausible.

**Plan:** attempt `dev.detekt` **2.0.0-alpha.6** first (plugin ID changed from
`io.gitlab.arturbosch.detekt` in 2.0; it matches the project's Kotlin line). If the alpha is
unstable, fall back to 1.23.8 **without type resolution**. Decide empirically — see §8.

Note: `2.0.0-alpha.1` had a group-ID resolution bug (`io.github.detekt` vs `dev.detekt`),
fixed in later alphas. Do not pin alpha.1.

### 3.3 Android Lint — free, already present

`lint` and `lintDebug` already exist and are already wired into `check` by AGP. It catches
Android-specific defects the other tools cannot. It costs nothing to include and gives the
write-up a third data point.

---

## 4. Approaches considered

**A — Curated ruleset, no baseline.** *(chosen)* A hand-picked detekt ruleset tuned for a
Compose codebase, `ignoreFailures = false`, PMD and Error Prone wired as assigned. Produces a
triageable number of findings; the build can realistically reach green.

**B — Full default ruleset + baseline ratchet.** *(rejected)* Generate `detekt-baseline.xml`
and gate only on new findings. This is correct practice for a legacy codebase and **wrong for
this assignment**: a baseline suppresses existing findings wholesale, leaving nothing to
triage, and it violates "suppress no more than three, each with a written reason" via a
generated file containing dozens with no reasons at all.

**C — Everything on, no baseline, fix it all.** *(rejected)* On 32 files at default thresholds
this likely yields 100+ findings. It blows past "fix five" and spends the assignment on
cleanup rather than on the triage reasoning being graded.

---

## 5. Gate design

### 5.1 detekt rule sets

Enable, with reasoning:

| Rule set | Why | Expected signal |
|---|---|---|
| `potential-bugs` | Closest analogue to Error Prone — real defects | **High** — keep strict |
| `exceptions` | `ProductsRepository` catches bare `Exception` | High |
| `complexity` | Fowler's Long Method / Long Parameter List | Mixed — thresholds need tuning |
| `naming` | Cheap, low false-positive | Low volume |
| `style` | Contains `MagicNumber` — the main noise source | **Noisy on Compose** |

Disable outright: `formatting` (ktlint wrapper — out of scope, would flood output).

### 5.2 Threshold tuning, with justification

Compose UI code violates two default thresholds structurally, not accidentally:

- **`MagicNumber`** fires on every `16.dp`, `24.dp`, `weight(1f)`. These are layout literals,
  not unexplained constants.
- **`LongMethod`** (default 60 lines) fires on screen composables. A UI tree *is* long;
  splitting one purely to satisfy a line count usually harms readability.

Both are configured — not silently disabled — in `config/detekt/detekt.yml`, with the reason
recorded in a comment. The distinction matters for the write-up: a *tuned threshold* is an
engineering judgment; a *disabled rule* is an abdication.

### 5.3 Suppression policy

- Suppressions use `@Suppress("RuleName")` at the narrowest possible scope.
- Every suppression carries an adjacent comment stating **why**, not what.
- Hard cap: **3**. Exceeding it means the finding should have been fixed or the threshold
  tuned instead.

### 5.4 Failure behaviour

`ignoreFailures = false` on detekt, matching the assignment's "red build on violations."
PMD keeps the same setting for consistency, even though it will have nothing to analyse.

---

## 6. CI design

No CI currently exists — `.github/workflows/` is absent. This is built from scratch.

`.github/workflows/check.yml`:

- Triggers: `push` and `pull_request`.
- `actions/setup-java` with **Temurin 21** — satisfies AGP 9 (17+) and Error Prone 2.50 (21+).
  Local builds use JBR 25; this difference is intentional (CI should use a standard JDK) and
  is itself a verification item.
- `actions/setup-gradle` for dependency caching.
- Step: `./gradlew check --no-daemon` (per the assignment slide).
- Upload `app/build/reports/detekt/` as an artifact so findings are reviewable from the PR.

`check` already depends on `lint` and `testDebugUnitTest` via AGP, so this one command covers
unit tests, Android Lint, detekt, and PMD together.

---

## 7. Triage plan

### 7.1 Fix candidates (need 5; six listed for slack)

All are real smells observed in the code, not manufactured:

| # | Smell (Fowler) | Location | Fix |
|---|---|---|---|
| 1 | Duplicated Code | `LoginScreen` / `SignUpScreen` | Extract a shared password-field composable; the two form scaffolds are near-identical |
| 2 | Magic Number | `HomeScreen` → `tradeCard(..., 2, ...)` | Hardcoded target user ID; name it or make it a real parameter |
| 3 | Long Parameter List | `HomeViewModel.tradeCard(5 params)` | `username` is derivable from state; collapse |
| 4 | Misplaced package | `ui/login/admin/` | Admin is not part of the login flow; move to `ui/admin/` |
| 5 | Hardcoded secret | `API_KEY` in `ProductsRepository.kt` | Move to `local.properties` → `BuildConfig` |
| 6 | Overly generic catch | `ProductsRepository` `catch (error: Exception)` | Narrow to `IOException`; a generic catch also swallows programming errors |

Items 5 and 6 are the strongest "caught something real" material. Item 5 is a genuine security
issue currently committed to the repository.

### 7.2 Suppression candidates (max 3)

| Rule | Where | Written reason |
|---|---|---|
| `MagicNumber` | Compose `dp` / `weight` literals | Layout constants are self-documenting at the call site; extracting `private const val PADDING_16 = 16` reduces readability without reducing risk |
| `LongMethod` | Screen composables | A declarative UI tree is long by nature; splitting for line count alone creates single-use composables that obscure the layout |
| `TooGenericExceptionCaught` | *only if* the narrowing in fix #6 proves infeasible | Prefer the fix; hold this in reserve |

---

## 8. Verification steps (do these before writing the final config)

Ordered, each with a pass criterion. Assumption-checking, not hope.

1. **detekt version** — apply `dev.detekt` 2.0.0-alpha.6; run `./gradlew detekt`.
   *Pass:* completes and reports findings. *Fail:* fall back to 1.23.8 without type
   resolution and re-test.
2. **PMD produces zero tasks** — apply the `pmd` plugin; run `./gradlew tasks --all | grep -i pmd`.
   *Expected:* no per-variant tasks, or tasks that analyse 0 files. **Capture this output —
   it is write-up evidence.**
3. **Error Prone never runs** — apply `net.ltgt.errorprone`; run
   `./gradlew clean :app:assembleDebug`. *Expected:* `compileDebugJavaWithJavac NO-SOURCE`.
   **Capture this too.**
4. **Gate actually fails** — introduce a deliberate violation, confirm `./gradlew check`
   exits non-zero, then revert. A gate never observed failing is not known to work.
5. **CI parity** — confirm the workflow passes on Temurin 21, not just JBR 25 locally.

---

## 9. Write-up outline (the one-page deliverable)

1. **Tool/language mismatch.** PMD and Error Prone are Java-only; this project is 32 Kotlin
   files and 0 Java files. Evidence: `NO-SOURCE`, zero PMD tasks.
2. **What caught something real.** The hardcoded `API_KEY`; the bare `catch (Exception)`;
   the duplicated form scaffolds.
3. **What produced noise.** `MagicNumber` and `LongMethod` on Compose — not because the rules
   are wrong, but because their defaults assume imperative Kotlin, not declarative UI.
4. **Threshold change.** Google's static-analysis work found that code review checks need
   **under 10% effective false positives**, and that **developers, not tool authors, determine
   the perceived false-positive rate**; when warnings were merely filed as bugs, **84% went
   unfixed**. A rule firing on correct idiomatic code in the project's dominant paradigm burns
   the trust the gate depends on. Concrete proposal: raise `LongMethod` for `@Composable`
   functions, and exclude `dp`/`sp` literals from `MagicNumber`.

**References:** Sadowski et al., "Lessons from Building Static Analysis Tools at Google,"
CACM 61(4), 2018 · Fowler, *Refactoring* (code smell catalogue) · Tufano et al., "When and Why
Your Code Starts to Smell Bad," IEEE TSE 43(11), 2017 · detekt compatibility table ·
`gradle-errorprone-plugin` README.

---

## 10. Open questions

1. **Instructor sign-off.** Adding detekt is a deviation from the literal wording, even though
   PMD and Error Prone are still present as assigned. Worth confirming before submission.
2. **detekt alpha vs stable** — resolved by verification step 1, not by argument.
3. **Does fix #5 (`API_KEY` → `BuildConfig`) belong in this PR?** It is a genuine security fix
   and a legitimate finding, but it touches shared build config and the key needs rotating
   regardless, since it is already in git history.
