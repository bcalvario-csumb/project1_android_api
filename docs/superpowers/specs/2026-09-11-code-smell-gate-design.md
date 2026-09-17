# Static Analysis / Code Smell Gate — Design

**Date:** 2026-09-11
**Project:** `project1_android_api` (Kotlin Android, Compose + Room + OkHttp)
**Assignment:** Add PMD and Error Prone to the Gradle build; wire `./gradlew check` into CI;
triage every finding; fix ≥5; suppress ≤3 with written reasons; open a passing PR; review a
classmate's PR; write a one-page report on which rules produced noise and which caught
something real.

---

## 1. The finding that shapes this design

> **Revised 2026-09-12.** The first draft of this spec claimed PMD's Kotlin support was
> "experimental and not exposed by the Gradle plugin." **That was wrong.** PMD has had
> *stable* Kotlin support since 7.0.0, and it has been verified working on this repository.
> The corrected picture is below. Error Prone's situation is unchanged.

Requirement (confirmed with instructor, 2026-09-12): **PMD and detekt must both actually
work** — not merely be present in the build.

### 1.1 What was measured

All figures below were produced by running PMD 7.27.0 directly against this repository.

```
.java files under app/src:  0
.kt   files under app/src:  32

> Task :app:compileDebugJavaWithJavac NO-SOURCE
```

| Tool | Can it read Kotlin? | Findings here | Verdict |
|---|---|---|---|
| **Error Prone** | ✗ javac plugin, no javac runs | 0, structurally | Wire as assigned; **will never fire** |
| **PMD — built-in Kotlin rules** | ✅ parses cleanly | **0** | Works, but only 3 rules exist |
| **PMD — CPD (copy-paste)** | ✅ | **8 duplications** | ✅ **This is what makes PMD productive** |
| **detekt** | ✅ native | TBD, expected dozens | Primary smell source |

### 1.2 Error Prone remains structurally inert

It is a **javac plugin** — it inspects the AST as the Java compiler builds it.
`compileDebugJavaWithJavac` is `NO-SOURCE`, so there is nothing to hook. Separately,
`gradle-errorprone-plugin` states plainly: **"There's no specific support for the Android
Gradle Plugin."** It creates an `errorprone` configuration but does not wire it to Android
compile tasks. Both facts are worth reporting; neither is fixable without inventing Java.

### 1.3 PMD works on Kotlin — with a caveat that becomes the story

PMD parses all 32 Kotlin files without error. But PMD ships only **three** built-in Kotlin
rules — `FunctionNameTooShort`, `LocalVariableShadowsParameter` (best practices) and
`OverrideBothEqualsAndHashcode` (error prone) — and **none of them fire on this codebase**:

```
[INFO] Found no violations.
```

The value comes from **CPD**, PMD's copy-paste detector, which fully supports Kotlin. At
`--minimum-tokens 50` it reports **8 duplications**, the largest being 36 lines / 179 tokens.

**The Gradle wiring problem is separate from the capability question.** Gradle's `pmd` plugin
creates one `Pmd` task per *Java* source set, and an Android module has none. That is a
task-creation limitation, not a PMD limitation, and it is solved by registering a `Pmd` task
manually against the Kotlin sources (see §5.5).

**Design consequence:** PMD earns its place through CPD plus its three built-in rules, with
custom XPath rules as an optional extension (AST dumping is available — verified). detekt
carries the bulk of the smell detection. Error Prone is wired as assigned and reported as
inert, with evidence.

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
| **PMD** | 7.27.0 | — | Kotlin 2.2.10 | ✅ *parses Kotlin; verified* |
| **`pmd-kotlin`** | 7.27.0 | stable since PMD 7.0 | — | ✅ *must be on the PMD classpath* |
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

### 5.2b CPD (PMD copy-paste detection) — measured

CPD is where PMD produces real signal. Threshold sensitivity, measured on this repo:

| `--minimum-tokens` | Duplications found |
|---|---|
| 40 | 10 |
| **50** | **8** ← proposed |
| 75 | 7 |
| 100 | 6 |
| 150 | 1 |

**This table is the write-up's "what threshold would you change" evidence.** It shows the
finding count is a tuning artefact, not a property of the code.

At 50 tokens, **6 of the 8 duplications involve `LoginActivity.kt` or `SignUpActivity.kt`** —
the superseded Activities deliberately kept in the repo for the team. CPD independently
rediscovered dead code that was already known to be redundant, which is a strong "caught
something real" result: deleting those two files removes most reported duplication in one
move. The residual `LoginScreen` ↔ `SignUpScreen` overlap is the genuine smell to fix by
extraction.

Proposed setting: `--minimum-tokens 50`, failing the build on any duplication *after* the
Activity deletion lands.

### 5.3 Suppression policy

- Suppressions use `@Suppress("RuleName")` at the narrowest possible scope.
- Every suppression carries an adjacent comment stating **why**, not what.
- Hard cap: **3**. Exceeding it means the finding should have been fixed or the threshold
  tuned instead.

### 5.5 Wiring PMD to Kotlin sources in Gradle

Gradle's `pmd` plugin will create no tasks here (no Java source sets). Two routes, try in
order:

1. **Register a `Pmd` task manually** — `tasks.register<Pmd>("pmdKotlin")` with
   `source = fileTree("src/main/java") { include("**/*.kt") }`, `ruleSetFiles` pointing at a
   Kotlin ruleset XML, and **`pmd-kotlin` added to the `pmd` configuration** so the language
   module is on the tool classpath. Keeps the assignment's "add the PMD plugin" literal.
2. **Fallback: `JavaExec` against PMD CLI** — guaranteed to work (it is exactly what was
   verified above) but bypasses the Gradle plugin.

CPD is *not* exposed by Gradle's `pmd` plugin at all, so it needs its own `JavaExec` task
invoking `pmd cpd` regardless of which route above succeeds.

Note the CLI difference found during verification: `pmd check` accepts `--no-cache`,
`pmd cpd` does **not** (`--no-fail-on-violation` etc. are its options).

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
| 0 | Duplicated Code (**CPD-confirmed**) | `LoginActivity.kt`, `SignUpActivity.kt` | **Delete both.** Superseded by the Screen composables; involved in 6 of 8 CPD duplications. Single highest-value fix |
| 1 | Duplicated Code (**CPD-confirmed**) | `LoginScreen` / `SignUpScreen` | Extract a shared password-field composable; residual overlap after the deletion above |
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

**Already verified (2026-09-12, PMD 7.27.0 CLI against this repo):**

- ✅ PMD parses all 32 Kotlin files — no parse errors.
- ✅ Built-in Kotlin rules run and report `Found no violations` (0 findings, as expected from
  a 3-rule set).
- ✅ CPD finds 8 duplications at 50 tokens; threshold curve captured in §5.2b.
- ✅ `pmd ast-dump --language kotlin` works, so custom XPath rules are authorable.
- ⚠️ PMD warns `No auxClasspath configured for Kotlin; type resolution will not work`. Harmless
  for CPD and the current rules; would matter for type-dependent custom rules.

**Still to verify during implementation:**

1. **detekt version** — apply `dev.detekt` 2.0.0-alpha.6; run `./gradlew detekt`.
   *Pass:* completes and reports findings. *Fail:* fall back to 1.23.8 without type
   resolution and re-test.
2. **PMD via Gradle, not CLI** — does a manually registered `Pmd` task actually route `.kt`
   files to the Kotlin module (§5.5 route 1)? *Fail:* fall back to `JavaExec`.
3. **Error Prone never runs** — apply `net.ltgt.errorprone`; run
   `./gradlew clean :app:assembleDebug`. *Expected:* `compileDebugJavaWithJavac NO-SOURCE`.
   **Capture this output — it is write-up evidence.**
4. **Gate actually fails** — introduce a deliberate violation, confirm `./gradlew check`
   exits non-zero, then revert. A gate never observed failing is not known to work.
5. **CI parity** — confirm the workflow passes on Temurin 21, not just JBR 25 locally.

---

## 9. Write-up outline (the one-page deliverable)

1. **Tool/language fit is not all-or-nothing.** Error Prone is structurally inert here (javac
   plugin, `NO-SOURCE`). PMD *does* read Kotlin — but its rule *coverage* is three rules,
   which found nothing, while its *duplicate detector* found eight real duplications. "Does
   the tool support the language" and "does the tool have rules worth running" are different
   questions, and only the second one matters.
2. **What caught something real.** CPD independently rediscovered the two superseded Activity
   files (6 of 8 duplications); the hardcoded `API_KEY`; the bare `catch (Exception)`.
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

1. ~~**Instructor sign-off.**~~ **Resolved 2026-09-12:** requirement confirmed as *PMD working
   and detekt working*. Both are now genuinely functional (PMD via CPD + built-in rules).
   Error Prone is retained because the assignment slide names it, but it cannot fire; drop it
   if the instructor confirms it is not required.
2. **detekt alpha vs stable** — resolved by verification step 1, not by argument.
3. **Does fix #5 (`API_KEY` → `BuildConfig`) belong in this PR?** It is a genuine security fix
   and a legitimate finding, but it touches shared build config and the key needs rotating
   regardless, since it is already in git history.
