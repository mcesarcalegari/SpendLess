<!--
Sync Impact Report
==================
Version change: 1.3.0 → 1.3.1
Bump rationale: PATCH — wording clarification. Principle II now names the dedicated
`:core:design-system` module instead of "`:core:presentation` / the design-system package it
hosts". The rule itself (reuse the shared design system first) is unchanged.

Modified principles:
- II. Design-System Reuse First (module name clarified; no change in meaning)
Added sections: none
Removed sections: none

Previous amendments:
- 1.3.0 (2026-11-16): added Principle VIII (a preview for every composable, one per distinct
  state), Constitution Check item 8, and review coverage for I–VIII.
- 1.2.0 (2026-11-13): Principle I expanded with a mandatory order for Figma MCP calls.
- 1.1.0 (2026-11-13): added Principle VII (Figma Node Links on Every Designed Composable),
  item 7 in the Constitution Check, and review coverage for I–VII.
- 1.0.0 (2026-11-13): first ratification. Principles I–VI, Additional Constraints, and
  Development Workflow & Quality Gates were defined.

Templates requiring updates:
- ✅ .specify/templates/plan-template.md — not modified (reads the constitution at runtime)
- ✅ .specify/templates/spec-template.md — not modified (module names are not spec content)
- ✅ .specify/templates/tasks-template.md — not modified (reads the constitution at runtime)

Consistency:
- ✅ specs/001-transactions/plan.md already places the design system in `:core:design-system`
  (resolves /speckit-analyze finding H3).

Follow-up TODOs:
- AGENTS.md §6 still names Figma fileKey `ftCIcpiLxrMhckuzDmYQWL`. This constitution names
  `xSFwRCFwe13SK2kAq85SLj` (the user-supplied source). Update AGENTS.md so both agree
  (tracked as task T135 in specs/001-transactions/tasks.md).
- No deferred placeholders.
-->

# SpendLess Constitution

## Core Principles

### I. Figma Is the Single Source of Truth (NON-NEGOTIABLE)

The Figma file `SpendLess - Mauro Team` (fileKey `xSFwRCFwe13SK2kAq85SLj`) is the only authority
on *what* SpendLess is, *why* each screen exists, and *how* it looks.

- **Product mockups (the what & why):**
  https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=401-935
  (nodeId `401:935`). Every feature spec MUST trace back to frames on this page.
- **Design system (tokens, typography, colours, spacing, shapes, components, icons):**
  https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=0-1
  (nodeId `0:1`). Every theme value and design-system component MUST come from this page.
- Before building or changing any UI component or screen, agents MUST fetch the relevant node
  through the Figma MCP tools and build from that live result.
- **Structured design data first, screenshots never first.** The order of Figma MCP calls is:
  1. **Primary:** the design-context tool (currently `get_design_context`, or whatever the
     Figma MCP server calls its equivalent). It returns the real component hierarchy, layout,
     sizing, spacing, tokens, and text, and it MUST be the first call for every node that is
     built or changed.
  2. **Supporting structured data:** the metadata tool (`get_metadata`) for the layer and node
     hierarchy. Use it to find child node IDs when a design-context response is too large or
     truncated, then call the design-context tool on those child nodes. The variable-definitions
     tool (`get_variable_defs`) resolves design tokens.
  3. **Secondary only:** the screenshot tool (`get_screenshot`). It MAY be used only after
     structured data has been retrieved, as a visual cross-check. Agents MUST NOT read colours,
     dimensions, spacing, typography, hierarchy, or copy from a screenshot. Those values MUST come
     from the structured responses.
- A screenshot is never a fallback for a failed or empty design-context or metadata call. That
  case triggers the fail-closed rule below.
- Agents MUST NOT improvise, guess, approximate, or recall from memory any visual decision:
  colour, spacing, size, typography, corner radius, elevation, iconography, copy, or layout.
- **Fail closed:** if a Figma MCP call errors, times out, is unauthenticated, returns empty or
  partial data, or the node cannot be found, the agent MUST stop the UI work and report a
  blocking error naming the failing call and node. Work resumes only after the designs can be
  refetched from Figma. Cached screenshots, earlier conversation output, and "reasonable
  defaults" are not substitutes.

Rationale: Without a single reference, design drift builds up and no one can say which version
is correct. Failing loudly is cheaper than shipping an invented UI that has to be redone. A
screenshot only shows how a design looks: it doesn't give exact values, token names, or the
component structure, so building from one is guessing. The structured hierarchy is the design.

### II. Design-System Reuse First

- Every UI component MUST first be built from existing components and tokens in the shared
  design-system module (`:core:design-system`).
- Before writing a new composable, agents MUST search the design-system module for a component
  that matches the Figma element and reuse or parameterize it.
- A new component MAY be created only when no existing one fits. Then:
  - If it is a design-system component on the Figma design-system page (`0:1`), it MUST be added
    to the design-system module so every feature can reuse it.
  - If it appears only in a single feature's mockups, it MUST live in that feature's
    presentation module and MUST still be composed from design-system tokens and primitives.
- Hard-coded colours, text styles, dimensions, or shapes in feature code are forbidden when a
  design-system token exists.

Rationale: Reuse keeps the app visually consistent, means a token change in Figma maps to one
change in code, and stops copies of the same component piling up across features.

### III. Icon Sourcing: Figma First, Material 3 Fallback

- If an icon exists on the Figma design-system page (`0:1`), agents MUST export its real SVG from
  Figma and convert it into an Android vector asset using the
  [`figma-to-android-vector`](../../.claude/skills/figma-to-android-vector/SKILL.md) skill. Path
  data MUST be copied verbatim, and the viewBox aspect ratio MUST be scaled correctly.
- Such icons MUST NOT be replaced by Android Studio's bundled Material Design / Vector Asset
  Studio icons, even if they look similar.
- Material 3 icons MAY be used **only as a fallback** when the design-system page has no matching
  icon. Each fallback use MUST be noted in the feature's `plan.md` with the Figma node it stands
  in for.
- If the SVG for a Figma icon cannot be retrieved, Principle I's fail-closed rule applies.

Rationale: Icons are a large part of the brand's look. Hand-picked look-alikes or redrawn paths
are the most common source of visual drift.

### IV. Portrait Mobile Only

- SpendLess supports exactly one form factor: **portrait orientation on mobile phones**.
- The app MUST be locked to portrait. Landscape, tablet, foldable, and desktop layouts are out of
  scope and MUST NOT be designed, specified, or implemented. Larger screens render the portrait
  phone layout.
- Responsive sizing within portrait phones (different widths and heights) MUST still be handled
  with relative sizing such as fill-width, weights, and max-width constraints.
- Specs and plans MUST NOT add acceptance criteria, tasks, or tests for other orientations or
  form factors.

Rationale: The Figma mockups define portrait phone frames only. Building other variants would
mean inventing designs, which Principle I forbids.

### V. Spec Is What & Why, Plan Is How

Each feature directory holds a `spec.md` and a `plan.md` with strictly separate purposes.

| | `spec.md` | `plan.md` |
|---|---|---|
| Answers | **What** is the goal of this feature, and **why** does it matter? | **How** will the spec be implemented? |
| Audience | Product owner, designer, any stakeholder | Engineers and implementing agents |
| Language | Domain language only (e.g. expense, income, category, balance, PIN) | Technical: modules, classes, libraries, APIs, patterns |
| Technology | Technology-agnostic | Names technologies, libraries, versions, and skills |
| Contains | User goals, user stories, scenarios, functional requirements, success criteria, edge cases, links to the Figma frames that motivate the feature | Architecture, module placement, data model, state/actions/events, DI wiring, test strategy, icon sourcing decisions, Constitution Check |
| MUST NOT contain | Implementation details, technical terms beyond domain language, class/function/module names, library or framework names, code | New requirements the spec does not state |

- `spec.md` MUST be understandable by a non-engineer and stay valid even if the whole technology
  stack changed.
- `plan.md` MUST trace every implementation decision back to a requirement in `spec.md`. If the
  plan needs behaviour the spec does not describe, the spec MUST be amended first.
- Reviewers MUST reject a `spec.md` that names a library, class, module, or technology.

Rationale: Keeping intent apart from implementation lets the spec be checked against the Figma
mockups and the plan be checked against the spec, with neither one hiding drift in the other.

### VI. Skill-Driven Implementation

Project skills hold the binding technical conventions. Agents MUST load the relevant skill
**before** writing or reviewing the code it covers, by invoking it through the runtime's skill
mechanism, not by reading the file. This constitution links to the skills and does not copy them.
If a skill conflicts with this constitution or with `AGENTS.md`, the constitution and `AGENTS.md`
win.

**Project structure & build**

- [`android-module-structure`](../../.claude/skills/android-module-structure/SKILL.md): when
  creating the project or any module, or deciding where code lives (`:app`, `:core:*`,
  `:feature:*:presentation|domain|data`).
- [`android-version-catalog`](../../.claude/skills/android-version-catalog/SKILL.md): for any
  dependency, plugin, bundle, or version change, or a new convention plugin.
- [`android-cli`](../../.claude/skills/android-cli/SKILL.md): for SDK management, emulator/device
  orchestration, project scaffolding, and environment diagnostics.

**Dependency injection**

- [`android-di-koin`](../../.claude/skills/android-di-koin/SKILL.md): when defining Koin modules,
  providing ViewModels, repositories, or data sources, or wiring modules in `:app`.

**Presentation (Compose)**

- [`android-presentation-mvi`](../../.claude/skills/android-presentation-mvi/SKILL.md): when
  creating or changing a ViewModel, State/Action/Event, the Root/Screen split, `UiText`, or
  process-death handling. This is the only allowed shape for a screen.
- [`android-compose-architecture`](../../.claude/skills/android-compose-architecture/SKILL.md):
  for screen-level decisions such as recomposition, stability, state ownership, side effects,
  lazy-layout performance, and deferred reads.
- [`android-compose-components`](../../.claude/skills/android-compose-components/SKILL.md): when
  writing any composable that is not a Root/Screen. Covers design-system reuse (Principle II),
  previews per state (Principle VIII), icon objects, and slot APIs.
- [`compose-adaptive-layouts`](../../.claude/skills/compose-adaptive-layouts/SKILL.md): rarely
  needed under Principle IV. Consult it only to confirm that a requirement is plain responsive
  sizing within portrait phones, or if a future amendment brings back other form factors. It
  MUST NOT be used to add landscape or tablet variants.
- [`android-navigation`](../../.claude/skills/android-navigation/SKILL.md): when defining routes,
  adding screens to nav graphs, or navigating between features.

**Domain & data**

- [`android-data-layer`](../../.claude/skills/android-data-layer/SKILL.md): for data sources,
  repositories, Room entities, DAOs, mappers, and anything that crosses an I/O boundary.
- [`android-error-handling`](../../.claude/skills/android-error-handling/SKILL.md): whenever typed
  errors, validation results, or success/failure flows are defined, in any layer.
- [`android-background`](../../.claude/skills/android-background/SKILL.md): when scheduling
  WorkManager jobs, foreground services, or work observation.

**Concurrency**

- [`kotlin-coroutines`](../../.claude/skills/kotlin-coroutines/SKILL.md): for suspend functions,
  scopes, dispatchers, parallelism, cancellation safety, synchronization, and wrapping blocking
  code.
- [`kotlin-flows`](../../.claude/skills/kotlin-flows/SKILL.md): for choosing between
  Flow/StateFlow/SharedFlow/Channel, `stateIn`/`shareIn`, operators, `flowOn`, backpressure, and
  lifecycle-aware collection.

**Testing & coverage**

- [`android-unit-testing`](../../.claude/skills/android-unit-testing/SKILL.md): for every new
  pure-logic unit (ViewModel, validator, mapper, use case, formatter, reducer).
- [`android-integration-testing`](../../.claude/skills/android-integration-testing/SKILL.md): for
  tests that cross an I/O boundary (in-memory Room, fake clock, WorkManager test helpers).
- [`android-ui-testing`](../../.claude/skills/android-ui-testing/SKILL.md): for instrumented
  Compose UI and E2E tests of screens with non-trivial behaviour.
- [`kotlin-mutation-testing`](../../.claude/skills/kotlin-mutation-testing/SKILL.md): after
  writing or reviewing a test class, to confirm it actually catches regressions.
- [`android-jvm-test-coverage`](../../.claude/skills/android-jvm-test-coverage/SKILL.md): when a
  JVM unit-test coverage report for a module is needed without changing build files.
- [`android-jacoco-setup`](../../.claude/skills/android-jacoco-setup/SKILL.md): only when the
  project decides to commit a permanent JaCoCo coverage setup to the build.

**Figma fidelity & verification**

- [`figma-to-android-vector`](../../.claude/skills/figma-to-android-vector/SKILL.md): every time
  a Figma icon becomes a vector drawable or `ImageVector` (Principle III).
- [`android-preview-figma-verify`](../../.claude/skills/android-preview-figma-verify/SKILL.md):
  to compare a rendered Compose preview with its Figma frame when no device is needed or
  available.
- [`android-ui-verify`](../../.claude/skills/android-ui-verify/SKILL.md): after a screen is
  built, for a structural check (hierarchy, text, dp sizing ±4dp) on a live device.
- [`android-ui-screenshot-verify`](../../.claude/skills/android-ui-screenshot-verify/SKILL.md):
  after a screen is built, for a visual check (colour, clipping, overlap, icon shape, contrast)
  on a live device.

Rationale: The skills hold detailed, evolving conventions. Linking to them keeps one source per
topic and keeps this constitution short enough to enforce.

### VII. Figma Node Links on Every Designed Composable

Every composable that implements something shown in Figma MUST carry a comment with the full
Figma URL of the exact frame or component it implements, including the `node-id` query
parameter.

- **Required format:** a `Figma:` line in the composable's KDoc, placed directly above the
  function, with the full URL. For example:

  ```kotlin
  /**
   * Primary call-to-action button.
   *
   * Figma: https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=123-456
   */
  @Composable
  fun SpendLessPrimaryButton(...)
  ```

- **Which node to link:**
  - Screen and Root/Screen composables link the screen's frame on the mockups page (`401:935`).
  - Design-system components link their component (or component set) on the design-system page
    (`0:1`).
  - Feature-specific components link the selected frame or layer for that element inside the
    feature's mockup, not the parent screen or page.
  - If a state or variant has its own Figma frame (empty, error, loading, selected, disabled,
    and so on), the `@Preview` for that state MUST link that frame. If one composable implements
    several variants, its KDoc MUST list one `Figma:` line per variant, labelled with the variant
    name.
  - Icons created via `figma-to-android-vector` MUST link the icon's node on the design-system
    page in a comment on the vector asset or `ImageVector`.
- Linking only the page (`node-id=401-935` or `node-id=0-1`) when a more specific frame exists
  does not satisfy this rule.
- **Retrieval:** agents changing or verifying a composable MUST use the linked node as the first
  lookup. They extract the `node-id` from the comment, converting the URL's `-` to the MCP's
  `:` (for example, `123-456` → `123:456`), and fetch it with the Figma MCP tools under
  Principle I.
- **Keep links live:** a missing, page-level-only, or stale link (one that points to a deleted
  or unrelated node) is a defect. Fix it in the same change that touches the composable. If the
  correct node can't be found, Principle I's fail-closed rule applies.
- **Exempt:** purely structural helpers with no counterpart in Figma (for example, private layout
  wrappers or modifier extensions) don't need a link. Their callers do.

Rationale: A node link on each composable makes reviews instant. It lets the verification skills
target the exact frame, and it means future agents can pull the design data straight from Figma
without searching the file, which lowers the temptation to improvise.

### VIII. A Preview for Every Composable, One per Distinct State

Every composable function that is written or changed MUST have at least one `@Preview`. This
applies equally to Screen composables, design-system components, feature components, and the
private sub-composables a screen or component is split into.

- **One preview per distinct state.** When a composable can render in more than one distinct
  state, it MUST have one preview for each of them, so that every state can be checked at a
  glance without running the app. A state is distinct when it looks different on screen. Typical
  states are:
  - content states: empty, populated with data, loading, error;
  - interaction states: enabled, disabled, pressed, focused, selected, expanded, collapsed;
  - input states: placeholder, typing, filled, invalid;
  - variants: each style or type the composable supports (for example expense vs. income);
  - stress cases the spec names: very long text, maximum values, overflow.
- **Coverage follows the composable, not only the screen.** A screen's previews do not count as
  previews for the sub-composables it contains. Each sub-composable with several distinct states
  gets its own per-state previews.
- **How previews are written:**
  - Each preview is a separate `@Preview` function named after the state it shows (for example
    `TransactionListScreenEmptyPreview`, `SpendLessButtonDisabledPreview`).
    `@PreviewParameter` MAY be used if each state still renders as its own named preview entry.
  - Previews are wrapped in the app theme and use fixed sample data. They MUST NOT depend on a
    ViewModel, Koin, the database, the network, or the system clock.
  - If a state can't be reached through parameters alone (for example pressed or focused), the
    composable MUST expose a way to drive it in previews, such as an `interactionSource`
    parameter, rather than leaving the state without a preview.
  - Previews are light-scheme and portrait-phone only (Principle IV; Figma ships one scheme).
  - If the state has its own Figma frame, its preview links that frame (Principle VII).
- **Exempt:**
  - Root composables that only obtain a ViewModel and forward state. Their Screen composable
    carries the previews.
  - Composable functions that emit no UI, such as effect helpers (`ObserveAsEvents`),
    `remember…` helpers, and resource accessors (icon or painter getters).
- A missing preview, or a distinct state without its own preview, is a defect. It is fixed in the
  same change that adds or touches the composable.

Rationale: Per-state previews make a UI reviewable at a glance. They expose states that a manual
run rarely reaches (error, disabled, overflow). They are also what `android-preview-figma-verify`
compares against Figma without a device, so a missing preview is a state that is never checked.

## Additional Constraints

These are carried over from `AGENTS.md`, which remains the operating manual.

- **Platform & stack:** native Android, Kotlin, Jetpack Compose with Material 3 as the base for
  design-system components, Gradle version catalog plus convention plugins. Exact versions live
  in `gradle/libs.versions.toml` and `AGENTS.md`.
- **Architecture:** feature-layered Clean Architecture (`:app` + `:core:*` +
  `:feature:<name>:presentation|domain|data`). Domain depends on nothing, and features never
  depend on each other. Koin is used for DI. Shared `Result<T, E>` is used for typed errors.
- **Offline-first:** no remote backend. Agents MUST NOT invent network calls, sync, or
  server-backed auth.
- **Security:** financial data MUST be encrypted at rest (SQLCipher-backed Room, with the
  passphrase wrapped by an AndroidKeyStore key). No sensitive values (amounts, notes, balances,
  PINs, tokens) in logs, analytics, or crash reports. No sensitive data leaves the device.

## Development Workflow & Quality Gates

- **Spec Kit flow:** `/speckit-specify` (spec.md, what and why, linked to Figma frames) →
  `/speckit-clarify` → `/speckit-plan` (plan.md, how, with the Constitution Check) →
  `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement`.
- **Constitution Check (in every plan.md):** the plan MUST confirm:
  1. the Figma nodes that were fetched successfully with the design-context tool (I);
  2. the design-system components reused, and any new ones with where they live (II);
  3. each icon's source, Figma vector or Material 3 fallback with a reason (III);
  4. portrait-only scope (IV);
  5. a spec.md free of technology (V);
  6. the skills loaded (VI);
  7. the Figma node URL each new or changed composable will carry in its KDoc (VII);
  8. the previews each new or changed composable will have, one per distinct state (VIII).
- **Definition of done:** the acceptance criteria and verification method in `AGENTS.md` §4–§5
  apply. `./gradlew build` passes, tests exist and pass, UI is verified against Figma, the
  security invariants hold, and work is committed atomically.
- A Figma MCP failure during implementation is a **blocker**, not a warning. The task is marked
  blocked until the designs can be refetched.

## Governance

- This constitution overrides all other project practices and skills. `AGENTS.md` is the
  runtime operating manual and MUST stay consistent with it. When they conflict, the
  constitution wins and `AGENTS.md` MUST be updated.
- **Amendments** are made through `/speckit-constitution`. Each amendment MUST update the Sync
  Impact Report, bump the version, and set the Last Amended date.
- **Versioning (semantic):** MAJOR for removing or redefining a principle in a backward-
  incompatible way; MINOR for a new principle or section, or materially expanded guidance;
  PATCH for clarifications and wording.
- **Compliance review:** every plan.md MUST pass the Constitution Check before tasks are
  generated, and every review MUST verify Principles I–VIII. Any deviation MUST be justified in
  the plan's Complexity Tracking section and approved by the project owner.

**Version**: 1.3.1 | **Ratified**: 2026-11-13 | **Last Amended**: 2026-11-16
