# Code Smell Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ⛔ **COMMITS ARE HELD.** The user has asked for no commits or pushes yet. Every task ends
> with a commit step written out and marked `[HELD]`. Do the work, leave it in the working
> tree, and run the commit commands only once the user approves.

**Goal:** Make PMD and detekt both genuinely functional on this Kotlin Android project, gate
`./gradlew check` on their findings, run that gate in CI, then fix 5 findings and suppress 3
with written reasons.

**Architecture:** detekt runs natively on Kotlin and supplies the bulk of smell findings, tuned
so Compose idiom does not generate noise. PMD contributes through two channels: its three
built-in Kotlin rules (via the official Gradle `pmd` plugin plus the `pmd-kotlin` language
module) and CPD copy-paste detection (via a `JavaExec` task, since Gradle's plugin does not
expose CPD). Error Prone is wired as the assignment names it and reported as structurally
inert, with evidence. All three hang off `check`.

**Tech Stack:** Gradle 9.6.0 · AGP 9.3.2 · Kotlin 2.2.10 · JBR 25 · PMD 7.27.0 + `pmd-kotlin` ·
detekt 1.23.8 (fallback `dev.detekt` 2.0.0-alpha.6) · `net.ltgt.errorprone` 5.1.1 ·
`error_prone_core` 2.50.0 · GitHub Actions

**Spec:** `docs/superpowers/specs/2026-09-11-code-smell-gate-design.md`

---

## File Structure

| File | Responsibility |
|---|---|
| `gradle/libs.versions.toml` | Version + plugin coordinates (single source of truth) |
| `build.gradle.kts` (root) | Declare detekt/errorprone plugins `apply false` |
| `app/build.gradle.kts` | Apply plugins, configure tools, register PMD/CPD tasks, wire `check` |
| `config/detekt/detekt.yml` | Tuned detekt ruleset — the Compose noise decisions live here |
| `config/pmd/kotlin-ruleset.xml` | PMD's three Kotlin rules |
| `.github/workflows/check.yml` | CI gate |
| `docs/superpowers/reports/2026-09-12-triage.md` | Evidence + triage table for the write-up |

Configuration lives under `config/` (not in the build script) so the ruleset diffs are
reviewable on their own and the reasoning survives in comments.

---

## Phase 1 — Get the tools running

### Task 1: Install detekt and determine which version works

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

The spec flags this as the top unknown: stable detekt 1.23.8 is built against Kotlin 2.0.21
while this project is on 2.2.10. Try stable first — its DSL is settled — and fall back to the
alpha only if it actually fails.

- [ ] **Step 1: Add the version and plugin alias**

In `gradle/libs.versions.toml`, under `[versions]`:

```toml
# detekt 1.23.8 is built against Kotlin 2.0.21; this project is on 2.2.10. detekt bundles its
# own parser, so this generally works without type resolution. If it fails, see Task 1 Step 4.
detekt = "1.23.8"
```

Under `[plugins]`:

```toml
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

- [ ] **Step 2: Declare it at the root, apply it in `:app`**

In `build.gradle.kts` (root), inside `plugins { }`:

```kotlin
alias(libs.plugins.detekt) apply false
```

In `app/build.gradle.kts`, inside `plugins { }`:

```kotlin
alias(libs.plugins.detekt)
```

- [ ] **Step 3: Run it and observe**

```bash
export JAVA_HOME=/opt/android-studio/jbr
./gradlew :app:detekt --console=plain
```

Expected: the task runs and reports findings (a non-zero count is success — we want findings).
Record the total; it is Task 7 input.

- [ ] **Step 4: ONLY IF Step 3 failed with parse errors or a Kotlin version error**

Switch to the 2.0 alpha. The plugin ID changed in 2.0. In `libs.versions.toml`:

```toml
detekt = "2.0.0-alpha.6"
```
```toml
detekt = { id = "dev.detekt", version.ref = "detekt" }
```

Do **not** use `2.0.0-alpha.1` — it has a group-ID resolution bug (`io.github.detekt` vs
`dev.detekt`). Re-run Step 3. The `detekt { }` extension DSL differs between 1.x and 2.x; if
`config.setFrom(...)` in Task 2 is rejected, check `./gradlew :app:help --task detekt`.

- [ ] **Step 5: Commit** `[HELD]`

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: add detekt for Kotlin static analysis"
```

---

### Task 2: Configure the detekt ruleset with Compose-aware thresholds

**Files:**
- Create: `config/detekt/detekt.yml`
- Modify: `app/build.gradle.kts`

This is the heart of the assignment's write-up. The key decision: `MagicNumber` and
`LongMethod` are **kept active but excluded for `@Composable` functions** via `ignoreAnnotated`,
rather than disabled globally. The rules keep protecting ViewModels, repositories and DAOs —
only the declarative UI layer is exempt.

- [ ] **Step 1: Create the config**

Create `config/detekt/detekt.yml`:

```yaml
# Tuned for a Jetpack Compose codebase. See
# docs/superpowers/specs/2026-09-11-code-smell-gate-design.md §5.2
build:
  maxIssues: 0

complexity:
  active: true
  LongMethod:
    active: true
    threshold: 60
    # A declarative UI tree is long by nature. Splitting a composable purely to satisfy a
    # line count produces single-use composables that obscure the layout. The rule stays
    # active for ViewModels, repositories and DAOs.
    ignoreAnnotated: ['Composable']
  LongParameterList:
    active: true
    functionThreshold: 6
    # Composables legitimately take many parameters (state + one lambda per event).
    ignoreAnnotated: ['Composable']
  CyclomaticComplexMethod:
    active: true
    threshold: 15
  NestedBlockDepth:
    active: true
    threshold: 4
  TooManyFunctions:
    # Off: counts top-level composables in a screen file, which is not a smell.
    active: false

style:
  active: true
  MagicNumber:
    active: true
    ignoreNumbers: ['-1', '0', '1', '2']
    # Every 16.dp / 24.dp / weight(1f) is a layout literal, self-documenting at the call
    # site. Extracting them to constants reduces readability without reducing risk.
    ignoreAnnotated: ['Composable']
  ReturnCount:
    active: true
    max: 3
  UnusedPrivateMember:
    active: true

exceptions:
  active: true
  TooGenericExceptionCaught:
    active: true
  SwallowedException:
    active: true

potential-bugs:
  active: true

naming:
  active: true
  FunctionNaming:
    active: true
    # @Composable functions are PascalCase by Compose convention, which violates Kotlin's
    # camelCase function rule. 12 of the 45 baseline findings were this, all of them
    # correct-by-convention UI code. Rule stays active for everything that is not a composable.
    ignoreAnnotated: ['Composable']

# NewLineAtEndOfFile accounted for 17 of the 45 baseline findings. Left ACTIVE and fixed in
# bulk (Task 2 Step 5) rather than disabled: it is a real, trivially correct finding, and
# switching a rule off to lower the number is exactly what the write-up should criticise.

# Off: ktlint formatting wrapper. Out of scope — would flood the report and drown the signal.
formatting:
  active: false
```

- [ ] **Step 2: Point detekt at it**

In `app/build.gradle.kts`, after the `android { }` block:

```kotlin
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    // Red build on violations, per the assignment.
    ignoreFailures = false
}
```

- [ ] **Step 3: Run and capture the finding count**

```bash
./gradlew :app:detekt --console=plain 2>&1 | tail -30
```

Expected: a list of findings and a non-zero exit. Save the full output:

```bash
./gradlew :app:detekt --console=plain > /tmp/detekt-before.txt 2>&1 || true
grep -cE '^\s+[A-Za-z]+ -' /tmp/detekt-before.txt
```

- [ ] **Step 4: Confirm the Compose exclusions actually took effect**

```bash
grep -E 'MagicNumber|LongMethod' /tmp/detekt-before.txt | grep -iE 'Screen|Composable' | head
```

Expected: **no hits** in screen composables. If `MagicNumber` still fires on `16.dp`, the
`ignoreAnnotated` key is not being applied — check that `buildUponDefaultConfig = true` and
that the YAML indentation is correct.

- [ ] **Step 5: Commit** `[HELD]`

```bash
git add config/detekt/detekt.yml app/build.gradle.kts
git commit -m "build: tune detekt ruleset for Compose"
```

---

### Task 3: Wire PMD to the Kotlin sources

**Files:**
- Create: `config/pmd/kotlin-ruleset.xml`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

Verified already: PMD 7.27.0 parses all 32 Kotlin files cleanly, and its three built-in Kotlin
rules report zero violations here. That zero is expected and is itself write-up evidence — the
point of this task is that PMD *runs*, not that it complains.

Gradle's `pmd` plugin creates one task per **Java** source set, and this module has none, so
the task is registered manually.

- [ ] **Step 1: Add the PMD version**

In `gradle/libs.versions.toml` under `[versions]`:

```toml
pmd = "7.27.0"
```

- [ ] **Step 2: Create the Kotlin ruleset**

Create `config/pmd/kotlin-ruleset.xml`:

```xml
<?xml version="1.0"?>
<ruleset name="kotlin-all"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0">
  <description>
    All three built-in PMD Kotlin rules. PMD's Kotlin coverage is deliberately small;
    the bulk of PMD's value on this project comes from CPD (see the cpdKotlin task).
  </description>
  <rule ref="category/kotlin/bestpractices.xml"/>
  <rule ref="category/kotlin/errorprone.xml"/>
</ruleset>
```

- [ ] **Step 3: Apply the plugin and register a Kotlin-targeted task**

In `app/build.gradle.kts`, inside `plugins { }`:

```kotlin
id("pmd")
```

After the `android { }` block:

```kotlin
pmd {
    toolVersion = libs.versions.pmd.get()
    isIgnoreFailures = false
    ruleSetFiles = files("$rootDir/config/pmd/kotlin-ruleset.xml")
    // Clear the default Java rulesets — they do not apply to Kotlin sources.
    ruleSets = emptyList()
}

dependencies {
    // The Kotlin language module must be on PMD's tool classpath or .kt files are skipped.
    pmd("net.sourceforge.pmd:pmd-kotlin:${libs.versions.pmd.get()}")
}

// Gradle's pmd plugin only creates tasks for Java source sets, of which this module has
// none. Register one against the Kotlin sources instead.
tasks.register<Pmd>("pmdKotlin") {
    group = "verification"
    description = "Runs PMD's Kotlin rules over app/src/main/java (**/*.kt)."
    // `source` exposes only a getter on SourceTask; use setSource() in the Kotlin DSL.
    setSource(fileTree("src/main/java"))
    include("**/*.kt")
    exclude("**/build/**")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

- [ ] **Step 4: Run it**

```bash
./gradlew :app:pmdKotlin --console=plain 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, zero violations. Confirm it actually read the files rather than
silently skipping them:

```bash
grep -c 'file name' app/build/reports/pmd/pmdKotlin.xml
```

Expected: a number matching roughly the Kotlin file count, **not** 0. If the report is empty
or the task reports "no source", route 1 has failed — go to Step 5.

- [ ] **Step 5: ONLY IF Step 4 showed PMD did not read the .kt files**

Fall back to invoking the PMD CLI directly. Replace the `tasks.register<Pmd>` block with:

```kotlin
val pmdCli: Configuration by configurations.creating

dependencies {
    pmdCli("net.sourceforge.pmd:pmd-dist:${libs.versions.pmd.get()}")
}

tasks.register<JavaExec>("pmdKotlin") {
    group = "verification"
    description = "Runs PMD's Kotlin rules over app/src/main/java (**/*.kt)."
    classpath = pmdCli
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    args = listOf(
        "check",
        "--dir", "$projectDir/src/main/java",
        "--rulesets", "$rootDir/config/pmd/kotlin-ruleset.xml",
        "--format", "text",
    )
}
```

Re-run Step 4's command.

- [ ] **Step 6: Commit** `[HELD]`

```bash
git add gradle/libs.versions.toml config/pmd/kotlin-ruleset.xml app/build.gradle.kts
git commit -m "build: run PMD Kotlin rules via pmd-kotlin"
```

---

### Task 4: Add CPD copy-paste detection

**Files:**
- Modify: `app/build.gradle.kts`

This is where PMD earns its keep. Verified: 8 duplications at `--minimum-tokens 50`, six of
which involve the two superseded Activity files. Gradle's `pmd` plugin does not expose CPD at
all, so this is a `JavaExec` task regardless of which route Task 3 took.

- [ ] **Step 1: Register the task**

In `app/build.gradle.kts`. If Task 3 Step 5 already created `pmdCli`, reuse it and skip the
configuration/dependency lines here:

```kotlin
val pmdCli: Configuration by configurations.creating

dependencies {
    pmdCli("net.sourceforge.pmd:pmd-dist:${libs.versions.pmd.get()}")
}

// CPD finds duplicated token sequences. Threshold chosen from the measured curve in the
// design doc §5.2b: 40 -> 10 findings, 50 -> 8, 75 -> 7, 100 -> 6, 150 -> 1.
// 50 is low enough to catch the real screen duplication without flagging boilerplate.
tasks.register<JavaExec>("cpdKotlin") {
    group = "verification"
    description = "Runs CPD copy-paste detection over the Kotlin sources."
    classpath = pmdCli
    mainClass.set("net.sourceforge.pmd.cli.PmdCli")
    args = listOf(
        "cpd",
        "--dir", "$projectDir/src/main/java",
        "--language", "kotlin",
        "--minimum-tokens", "50",
        "--format", "text",
    )
    // CPD exits 4 when duplications are found. Let that fail the build.
    // NOTE: `pmd cpd` does NOT accept --no-cache (that is `pmd check` only).
}
```

- [ ] **Step 2: Run it and confirm the known result**

```bash
./gradlew :app:cpdKotlin --console=plain 2>&1 | grep -c 'Found a'
```

Expected: **8**. If the number differs, the source directory or token threshold is wrong.

- [ ] **Step 3: Commit** `[HELD]`

```bash
git add app/build.gradle.kts
git commit -m "build: add CPD copy-paste detection for Kotlin"
```

---

### Task 5: Wire Error Prone as assigned, and record that it cannot fire

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

The assignment slide names Error Prone. It is a javac plugin and this module compiles no Java,
so it will never produce a finding. Wire it anyway and capture the evidence — that evidence is
a graded deliverable, not a workaround.

- [ ] **Step 1: Add coordinates**

`gradle/libs.versions.toml`, under `[versions]`:

```toml
errorprone = "5.1.1"
errorproneCore = "2.50.0"
```

Under `[libraries]`:

```toml
errorprone-core = { group = "com.google.errorprone", name = "error_prone_core", version.ref = "errorproneCore" }
```

Under `[plugins]`:

```toml
errorprone = { id = "net.ltgt.errorprone", version.ref = "errorprone" }
```

- [ ] **Step 2: Apply it**

`build.gradle.kts` (root), inside `plugins { }`:

```kotlin
alias(libs.plugins.errorprone) apply false
```

`app/build.gradle.kts`, inside `plugins { }`:

```kotlin
alias(libs.plugins.errorprone)
```

In `app/build.gradle.kts` `dependencies { }`:

```kotlin
// Error Prone is a javac plugin. This module has 0 Java files, so compileDebugJavaWithJavac
// is NO-SOURCE and Error Prone never executes. Wired because the assignment names it; the
// empty result is documented in docs/superpowers/reports/2026-09-12-triage.md.
// Note: error_prone_core 2.50 requires JDK 21+ (JBR 25 satisfies this), and the plugin
// automatically forks the compiler with the --add-exports/--add-opens that JDK 16+ needs.
errorprone(libs.errorprone.core)
```

- [ ] **Step 3: Capture the evidence**

```bash
./gradlew clean :app:assembleDebug --console=plain 2>&1 | grep -i 'JavaWithJavac'
```

Expected, verbatim: `> Task :app:compileDebugJavaWithJavac NO-SOURCE`

Save that line — Task 12 puts it in the report.

- [ ] **Step 4: Commit** `[HELD]`

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: wire Error Prone per assignment (inert on Kotlin-only module)"
```

---

### Task 6: Hang everything off `check`, and prove the gate can fail

**Files:**
- Modify: `app/build.gradle.kts`

A gate never observed failing is not known to work.

- [ ] **Step 1: Wire the tasks into `check`**

In `app/build.gradle.kts`:

```kotlin
// `check` already depends on lint and testDebugUnitTest via AGP. Adding these makes
// `./gradlew check` the single command the CI workflow runs.
tasks.named("check") {
    dependsOn("detekt", "pmdKotlin", "cpdKotlin")
}
```

- [ ] **Step 2: Confirm all four run**

```bash
./gradlew :app:check --dry-run --console=plain | grep -E 'detekt|pmdKotlin|cpdKotlin|lint|testDebug'
```

Expected: all of `detekt`, `pmdKotlin`, `cpdKotlin`, a `lint` task and `testDebugUnitTest`.

- [ ] **Step 3: Deliberately break it**

Add to the bottom of `app/src/main/java/com/example/project1/data/ProductsCache.kt`:

```kotlin
// TEMPORARY — gate verification, removed in the next step
private fun x() {
    val unused = 999999
}
```

`x` violates `FunctionNameTooShort` (PMD) and `999999` violates `MagicNumber` (detekt, and this
is not a `@Composable` so the exclusion does not apply).

- [ ] **Step 4: Confirm the build goes red**

```bash
./gradlew :app:check --console=plain; echo "exit=$?"
```

Expected: `exit=1` and at least one of the tools naming this function. **If it exits 0, the
gate is decorative — stop and fix `ignoreFailures` before continuing.**

- [ ] **Step 5: Revert the deliberate break**

```bash
git checkout -- app/src/main/java/com/example/project1/data/ProductsCache.kt
./gradlew :app:check --console=plain; echo "exit=$?"
```

Record whether this now exits 0 or 1 — a non-zero exit here is the real pre-existing finding
set, which is exactly what Phase 2 triages.

- [ ] **Step 6: Commit** `[HELD]`

```bash
git add app/build.gradle.kts
git commit -m "build: gate check on detekt, PMD and CPD"
```

---

## Phase 2 — Triage

### Task 7: Capture the baseline and build the triage table

**Files:**
- Create: `docs/superpowers/reports/2026-09-12-triage.md`

The assignment requires triaging **every** finding into fix / suppress-with-justification /
false positive. This task produces that table; Tasks 8–11 act on it.

- [ ] **Step 1: Dump every finding to one place**

```bash
mkdir -p docs/superpowers/reports
./gradlew :app:detekt --console=plain > /tmp/f-detekt.txt 2>&1 || true
./gradlew :app:pmdKotlin --console=plain > /tmp/f-pmd.txt 2>&1 || true
./gradlew :app:cpdKotlin --console=plain > /tmp/f-cpd.txt 2>&1 || true
./gradlew :app:lintDebug --console=plain > /tmp/f-lint.txt 2>&1 || true
wc -l /tmp/f-*.txt
```

- [ ] **Step 2: Create the report skeleton with the counts**

Create `docs/superpowers/reports/2026-09-12-triage.md`:

```markdown
# Static Analysis Triage — 2026-09-12

## Tool inventory

| Tool | Reads Kotlin? | Findings | Note |
|---|---|---|---|
| Error Prone 2.50.0 | No — javac plugin | 0 | `compileDebugJavaWithJavac NO-SOURCE` |
| PMD 7.27.0 built-in Kotlin rules | Yes | 0 | Only 3 rules exist for Kotlin |
| PMD CPD (50 tokens) | Yes | 8 | 6 of 8 involve superseded Activity files |
| detekt | Yes (native) | _fill from Step 1_ | Primary smell source |
| Android Lint | Yes | _fill from Step 1_ | Already in `check` via AGP |

## CPD threshold sensitivity

| `--minimum-tokens` | Duplications |
|---|---|
| 40 | 10 |
| 50 | 8 |
| 75 | 7 |
| 100 | 6 |
| 150 | 1 |

## Triage

| # | Tool | Rule | Location | Verdict | Reason |
|---|---|---|---|---|---|
| | | | | fix / suppress / false-positive | |
```

- [ ] **Step 3: Fill one row per finding**

Every finding from Step 1 gets a row. Verdicts must total **exactly 5 fixes** and **at most 3
suppressions**; everything else is `false-positive` with a reason.

- [ ] **Step 4: Commit** `[HELD]`

```bash
git add docs/superpowers/reports/2026-09-12-triage.md
git commit -m "docs: triage all static analysis findings"
```

---

## Phase 3 — The five fixes

### Task 8: Fix 1 — delete the superseded Activity files

**Files:**
- Delete: `app/src/main/java/com/example/project1/ui/login/LoginActivity.kt`
- Delete: `app/src/main/java/com/example/project1/ui/signup/SignUpActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

Highest-value single change: CPD reports these two files in **6 of 8** duplications. They were
kept deliberately so the team could see the Activity→Composable migration; that has now
happened, and CPD independently flagged them as redundant.

- [ ] **Step 1: Confirm nothing references them**

```bash
grep -rn "LoginActivity\|SignUpActivity" app/src --include=*.kt --include=*.xml \
  | grep -v 'ui/login/LoginActivity.kt\|ui/signup/SignUpActivity.kt'
```

Expected: only hits inside the commented-out manifest block and `strings.xml`. If any live
Kotlin references appear, **stop** — the files are not dead and this fix is wrong.

- [ ] **Step 2: Delete the files**

```bash
git rm app/src/main/java/com/example/project1/ui/login/LoginActivity.kt
git rm app/src/main/java/com/example/project1/ui/signup/SignUpActivity.kt
```

- [ ] **Step 3: Remove the commented `<activity>` block**

In `app/src/main/AndroidManifest.xml`, delete the entire `<!-- COMMENTED OUT ... -->` block
that holds the two `<activity>` declarations. The `<activity>` entry for `.MainActivity` stays.

- [ ] **Step 4: Remove the orphaned strings**

In `app/src/main/res/values/strings.xml`, delete these two lines — nothing references them once
the manifest block is gone:

```xml
<string name="title_activity_login">LoginActivity</string>
<string name="title_activity_sign_up">SignUpActivity</string>
```

- [ ] **Step 5: Verify the duplication count dropped**

```bash
./gradlew :app:assembleDebug --console=plain 2>&1 | tail -3
./gradlew :app:cpdKotlin --console=plain 2>&1 | grep -c 'Found a'
```

Expected: build succeeds, and the count falls from 8 to roughly 1–2 (only the genuine
`LoginScreen` ↔ `SignUpScreen` overlap should remain).

- [ ] **Step 6: Commit** `[HELD]`

```bash
git add -A app/src/main
git commit -m "refactor: delete Activities superseded by Compose screens"
```

---

### Task 9: Fix 2 — extract the duplicated credential fields

**Files:**
- Create: `app/src/main/java/com/example/project1/ui/components/CredentialFields.kt`
- Modify: `app/src/main/java/com/example/project1/ui/login/LoginScreen.kt`
- Modify: `app/src/main/java/com/example/project1/ui/signup/SignUpScreen.kt`

The residual CPD finding after Task 8. Both screens build an identical email + password field
pair.

- [ ] **Step 1: Create the shared composable**

Create `app/src/main/java/com/example/project1/ui/components/CredentialFields.kt`:

```kotlin
package com.example.project1.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * The email + password pair shared by the login and sign-up screens.
 *
 * Extracted after CPD reported the two screens as a duplicated token sequence.
 * Stateless: both values and both callbacks are hoisted to the calling screen.
 */
@Composable
fun CredentialFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier.fillMaxWidth(),
    )
}
```

- [ ] **Step 2: Use it in `LoginScreen`**

In `LoginScreen.kt`, replace the two `OutlinedTextField` blocks (email and password) and the
`Spacer` between them with:

```kotlin
CredentialFields(
    email = email,
    onEmailChange = { email = it },
    password = password,
    onPasswordChange = { password = it },
)
```

Add the import `com.example.project1.ui.components.CredentialFields`. Remove the now-unused
`OutlinedTextField` and `PasswordVisualTransformation` imports if nothing else uses them.

- [ ] **Step 3: Use it in `SignUpScreen`**

Same replacement for the email and password fields. **Leave the Confirm Password field in
place** — it has `isError` and `supportingText` that the shared composable does not model, and
forcing it in would make `CredentialFields` worse for both callers.

- [ ] **Step 4: Verify**

```bash
./gradlew :app:assembleDebug --console=plain 2>&1 | tail -3
./gradlew :app:cpdKotlin --console=plain 2>&1 | grep -c 'Found a'
```

Expected: build succeeds and the duplication count reaches 0.

- [ ] **Step 5: Commit** `[HELD]`

```bash
git add app/src/main/java/com/example/project1/ui
git commit -m "refactor: extract CredentialFields shared by login and sign-up"
```

---

### Task 10: Fix 3 — narrow the generic exception catch

**Files:**
- Modify: `app/src/main/java/com/example/project1/data/ProductsRepository.kt`

detekt's `TooGenericExceptionCaught` fires here, and it is right: `catch (error: Exception)`
also swallows programming errors such as `IllegalStateException` from `error(...)`, hiding
bugs behind the cache fallback.

- [ ] **Step 1: Narrow the catch in `fetchProducts`**

Change:

```kotlin
} catch (error: Exception) {
    Log.e(TAG, "API request failed. Loading cached data.", error)
    cache.getProducts() ?: error("No API or cached data available.")
}
```

to:

```kotlin
} catch (error: IOException) {
    // Only network/IO failures should fall back to the cache. A generic `Exception` catch
    // also swallowed IllegalStateException from the error(...) calls above, hiding real
    // bugs behind a cache hit.
    Log.e(TAG, "API request failed. Loading cached data.", error)
    cache.getProducts() ?: error("No API or cached data available.")
}
```

Add the import:

```kotlin
import java.io.IOException
```

- [ ] **Step 2: Verify**

```bash
./gradlew :app:assembleDebug :app:detekt --console=plain 2>&1 | grep -iE 'TooGenericExceptionCaught|BUILD'
```

Expected: the `TooGenericExceptionCaught` finding for this file is gone.

- [ ] **Step 3: Commit** `[HELD]`

```bash
git add app/src/main/java/com/example/project1/data/ProductsRepository.kt
git commit -m "fix: catch IOException rather than Exception in ProductsRepository"
```

---

### Task 11: Fixes 4 and 5 — the magic target user, and the misplaced admin package

**Files:**
- Modify: `app/src/main/java/com/example/project1/ui/home/HomeScreen.kt`
- Move: `app/src/main/java/com/example/project1/ui/login/admin/` → `app/src/main/java/com/example/project1/ui/admin/`
- Modify: `app/src/main/java/com/example/project1/ui/nav/AppNavHost.kt`

- [ ] **Step 1: Name the magic number**

In `HomeScreen.kt`, `tradeCard` is called with a bare `2` as `targetUserId`. Add at the top of
the file, below the imports:

```kotlin
/**
 * Placeholder recipient for the "Trade Away" action until real trading partners exist.
 * TODO(team): replace with the actual selected user once trading is implemented.
 */
private const val PLACEHOLDER_TRADE_TARGET_USER_ID = 2
```

and change the call site from `2,` to `PLACEHOLDER_TRADE_TARGET_USER_ID,`.

- [ ] **Step 2: Move the admin package**

The admin panel is not part of the login flow; nesting it under `ui/login/` is a structural
smell.

```bash
mkdir -p app/src/main/java/com/example/project1/ui/admin
git mv app/src/main/java/com/example/project1/ui/login/admin/AdminScreen.kt \
       app/src/main/java/com/example/project1/ui/admin/AdminScreen.kt
git mv app/src/main/java/com/example/project1/ui/login/admin/AdminViewModel.kt \
       app/src/main/java/com/example/project1/ui/admin/AdminViewModel.kt
rmdir app/src/main/java/com/example/project1/ui/login/admin
```

- [ ] **Step 3: Update the package declarations and imports**

In both moved files, change the first line from:

```kotlin
package com.example.project1.ui.login.admin
```

to:

```kotlin
package com.example.project1.ui.admin
```

Then fix every reference:

```bash
grep -rln 'ui\.login\.admin' app/src | xargs sed -i 's/ui\.login\.admin/ui.admin/g'
grep -rn 'ui\.login\.admin' app/src || echo "  no stale references"
```

This updates the `AdminScreen` import in `AppNavHost.kt` and the `ADMIN_EMAIL` import in
`HomeScreen.kt`.

- [ ] **Step 4: Verify**

```bash
./gradlew :app:assembleDebug --console=plain 2>&1 | grep -E '^e:|BUILD'
```

Expected: BUILD SUCCESSFUL, no errors.

- [ ] **Step 5: Commit** `[HELD]`

```bash
git add -A app/src/main/java/com/example/project1
git commit -m "refactor: name placeholder trade target, move admin out of login package"
```

---

### Task 12: The three suppressions

**Files:**
- Modify: whichever files the Task 7 triage table marked `suppress`

Hard cap of three, each with a written reason. Note that the `MagicNumber` and `LongMethod`
noise on Compose is handled by `ignoreAnnotated` in `config/detekt/detekt.yml` (Task 2) — that
is a *tuned threshold*, not a suppression, and does not count against the three.

- [ ] **Step 1: Apply each suppression at the narrowest possible scope**

Pattern — annotate the smallest declaration that silences the finding, never a whole file:

```kotlin
// Suppressed: <one sentence saying WHY this finding is not worth acting on here.>
@Suppress("RuleName")
private fun theSpecificFunction() { }
```

- [ ] **Step 2: Confirm the count**

```bash
grep -rn '@Suppress' app/src/main --include=*.kt | wc -l
```

Expected: **3 or fewer**. If higher, one of them should have been a fix or a threshold change.

- [ ] **Step 3: Confirm every suppression carries a reason**

```bash
grep -rn -B2 '@Suppress' app/src/main --include=*.kt | grep -c 'Suppressed:'
```

Expected: equal to the count from Step 2.

- [ ] **Step 4: Commit** `[HELD]`

```bash
git add app/src/main
git commit -m "chore: suppress three findings with written justification"
```

---

## Phase 4 — CI and write-up

### Task 13: The CI workflow

**Files:**
- Create: `.github/workflows/check.yml`

No CI exists in this repo yet.

- [ ] **Step 1: Create the workflow**

Create `.github/workflows/check.yml`:

```yaml
name: check

on:
  push:
  pull_request:

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Temurin 21 satisfies AGP 9 (JDK 17+) and error_prone_core 2.50 (JDK 21+).
      # Local builds use JBR 25; CI deliberately uses a standard JDK.
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - uses: gradle/actions/setup-gradle@v4

      - name: Run the static analysis gate
        run: ./gradlew check --no-daemon

      # Findings stay reviewable from the PR even when the gate fails.
      - name: Upload reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: analysis-reports
          path: |
            app/build/reports/detekt/
            app/build/reports/pmd/
```

- [ ] **Step 2: Verify the file parses as YAML**

```bash
python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/check.yml')); print('  valid YAML')"
```

- [ ] **Step 3: Confirm the gate passes locally before pushing**

```bash
./gradlew check --console=plain; echo "exit=$?"
```

Expected: `exit=0`. CI cannot pass if local does not.

- [ ] **Step 4: Commit** `[HELD]`

```bash
git add .github/workflows/check.yml
git commit -m "ci: run ./gradlew check on push and pull request"
```

---

### Task 14: Finish the write-up evidence

**Files:**
- Modify: `docs/superpowers/reports/2026-09-12-triage.md`

- [ ] **Step 1: Append the after-state**

Re-run each tool and record before/after counts in the report:

```bash
./gradlew :app:detekt :app:pmdKotlin :app:cpdKotlin --console=plain 2>&1 | tail -40
```

- [ ] **Step 2: Write the four-part conclusion**

Append to the report, following spec §9:

1. **Tool/language fit is not all-or-nothing.** Error Prone is structurally inert (javac
   plugin, `NO-SOURCE`). PMD *does* read Kotlin — but ships 3 rules, which found 0, while its
   duplicate detector found 8. "Supports the language" and "has rules worth running" are
   different questions.
2. **What caught something real.** CPD independently rediscovered the two superseded Activity
   files; `TooGenericExceptionCaught` on the cache fallback; the duplicated credential fields.
3. **What produced noise.** `MagicNumber` and `LongMethod` on Compose — their defaults assume
   imperative Kotlin, not declarative UI.
4. **Threshold change.** Cite the measured CPD curve (§5.2b) and Sadowski et al. (CACM 61(4),
   2018): code review checks need under 10% effective false positives, developers rather than
   tool authors determine the perceived rate, and 84% of findings merely filed as bugs went
   unfixed. Proposal: keep `MagicNumber`/`LongMethod` active but `ignoreAnnotated: ['Composable']`,
   which is what this build now does.

- [ ] **Step 3: Commit** `[HELD]`

```bash
git add docs/superpowers/reports/2026-09-12-triage.md
git commit -m "docs: complete static analysis write-up evidence"
```

---

## Still outstanding after this plan

Deliberately **not** included, and why:

- **`API_KEY` → `BuildConfig`.** A genuine finding and a real security issue, but the key is
  already in git history and needs rotating regardless. It touches shared build config and
  deserves its own PR rather than riding along with a tooling change. Raise it separately.
- **The classmate PR review** (assignment step 5) — a human task, not a code change.
- **Restoring the JVM unit tests.** `RouteSerializationTest` and `HomeViewModelTest` were lost
  in commit `d7edfac`; only `ExampleUnitTest` runs on the JVM now. Out of scope here, but it
  means `check` has thin test coverage behind the new gate.
