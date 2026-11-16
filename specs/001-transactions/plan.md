# Implementation Plan: Transactions

**Branch**: `001-transactions` | **Date**: 2026-11-13 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-transactions/spec.md`

## Summary

This plan delivers the **All Transactions** list and the **Create Transaction** bottom sheet:

- Day-grouped, pinned headers.
- Note-badge expansion.
- Expense and income amounts formatted from one shared preferences source.
- Recurring series that are materialised exactly once on app start and live at midnight, and can be stopped from a long-press menu.

**Technical approach**:

- This is the project's first multi-module split: `:core:{domain,data,database,presentation,design-system}` plus `:feature:transactions:{domain,data,presentation}`.
- Storage is a SQLCipher-encrypted Room database whose passphrase is wrapped by an AndroidKeyStore key.
- The presentation layer is MVI (Root/Screen split, Koin ViewModels).
- At the user's request, every visual element was **refetched from Figma as structured data**. The buttons, FAB, segmented button, text field, icon button and dropdown (Select) are built as reusable `:core:design-system` components. Each is based on an M3 composable, with **every** shape, colour, size, padding, state layer and shadow overridden from Figma (research R3). They do not use stock M3 styling.

## Technical Context

**Language/Version**: Kotlin 2.2.10, JDK 11 target, AGP 9.1.1, Gradle 9.3.1

**Primary Dependencies**:

- Already present: Jetpack Compose and Material 3.
- Bumped to the current stable Compose BOM, which brings Compose UI ≥ 1.9 for `dropShadow` and material3 ≥ 1.4 for the fixed colour roles and `RippleConfiguration` (research R2).
- To add (versions resolved via `android-version-catalog` at implementation time):
  - Koin (android, compose)
  - Room + KSP
  - `net.zetetic:sqlcipher-android`
  - navigation-compose (type-safe)
  - kotlinx-serialization
  - lifecycle-runtime-compose, lifecycle-viewmodel-compose, lifecycle-process
  - kotlinx-collections-immutable

**Storage**: Room v1 (`transactions`, `recurring_series`), encrypted with SQLCipher. The passphrase is wrapped by an AndroidKeyStore AES-GCM key (research R13).

**Testing**:

- JUnit 5 (`de.mannodermaus.android-junit5`), AssertK, Turbine, kotlinx-coroutines-test.
- Instrumented in-memory Room tests.
- Compose UI tests with `createAndroidComposeRule`.
- `android-preview-figma-verify`, `android-ui-verify` and `android-ui-screenshot-verify`.

**Target Platform**: Android, minSdk 26 / targetSdk 36 / compileSdk 36. Portrait phones only.

**Project Type**: mobile-app (offline-first, no backend)

**Performance Goals**: The list scrolls smoothly with ≥ 1,000 transactions (SC-005), using a keyed lazy list, `contentType`, immutable UI models and deferred reads. A new row is visible within 1s of tapping Create (SC-004), because it arrives through the Room `Flow`.

**Constraints**:

- Figma is the single source of truth.
- Offline only.
- No sensitive data in logs.
- Amounts are `Long` minor units, at most 999,999,999.99.
- All text is English only.

**Scale/Scope**: 2 screens (one of them a sheet); 7 DS components plus theme tokens and icons; 1 Room DB; about 10 JVM test classes and 4 instrumented test classes.

No **NEEDS CLARIFICATION** items remain. Every open question was resolved in [research.md](./research.md) or by user decision (Complexity Tracking).

## Constitution Check

*GATE: must pass before Phase 0 research; re-checked after Phase 1 design.*

| # | Principle | Status | Evidence |
|---|---|---|---|
| 1 | **I. Figma SSoT**: nodes fetched with design-context | ✅ PASS | 18 nodes fetched with `get_design_context` (structured first), plus `get_metadata` and `get_variable_defs`. Full log in research R0. Screenshots were used only as a visual cross-check. **Two kit masters (`406:2552`, `406:2443`) were rejected by the API.** Their values come from the fetched instances (R6). The failure is reported, not hidden. |
| 2 | **II. DS reuse**: reused and new components and where they live | ✅ PASS | Reused: existing theme (colours, Figtree typography, `bodyXSmall`, `success`), moved to `:core:design-system`. **New in `:core:design-system`** (from page `0:1`): `SpendLessButton` (`406:110`), `SpendLessIconButton` (`406:3390`), `SpendLessFloatingActionButton` (`410:2338`), `SpendLessSegmentedButton` (`406:5441`), `SpendLessTextField` (`407:1793`), `SpendLessSelect` (`2415:10171`/`10118`/`10072`), `SpendLessTopAppBar` (kit instance), `SpendLessShadows`, `SpendLessIcons`, wired fixed colour roles. **Feature-internal** (from tokens only): `TransactionRow`, `CounterpartyInput`, `AmountInput`, `NoteInput`, `CreateTransactionSheet`, `StopRepeatingMenu`, `StopRepeatingDialog`, day-header style. Contracts: [design-system-components.md](./contracts/design-system-components.md), [transactions-ui.md](./contracts/transactions-ui.md). |
| 3 | **III. Icons** | ✅ PASS | All 8 icons are regenerated from DS symbol nodes with the 24×24 viewport preserved (R5). **No Material fallbacks.** The existing `:app` drawables were found to crop their viewports, so they are replaced, not reused. |
| 4 | **IV. Portrait only** | ✅ PASS (with work item) | `AndroidManifest.xml` currently has **no** orientation lock (verified 2026-11-13). This plan **adds** `android:screenOrientation="portrait"` to `MainActivity`. No other form-factor tasks, tests or previews. Width handled with `fillMaxWidth` and `weight`. |
| 5 | **V. Spec has no technology** | ✅ PASS | spec.md names no library, class or module. **Spec amended first** (Assumptions, 2026-11-13) to record the owner's decisions on the stop menu and dialog look and on note entry, so the plan adds nothing the spec does not state. |
| 6 | **VI. Skills loaded** | ✅ PASS (planning) | Loaded for this plan: `figma:figma-design-to-code`, `android-module-structure`, `android-compose-components`, `android-presentation-mvi`, `android-data-layer`, `android-background` (decided against WorkManager, R10), `android-error-handling` (one error type per `Result`, which split validation from storage), `android-navigation` (graph takes a `NavController`; the sheet is not a destination). **To load at implementation, before writing the matching code**: `android-version-catalog`, `android-di-koin`, `android-compose-architecture`, `kotlin-coroutines`, `kotlin-flows`, `figma-to-android-vector`, `android-unit-testing`, `android-integration-testing`, `android-ui-testing`, `kotlin-mutation-testing`, `android-preview-figma-verify`, `android-ui-verify`, `android-ui-screenshot-verify`. |
| 7 | **VII. Figma URL per composable** | ✅ PASS | Each composable's `Figma:` node(s), including per-variant and per-state previews, are listed in the two UI contracts. Composables without a node (stop menu and dialog, note-typing state) carry a KDoc stating the user decision instead. |
| 8 | **VIII. A preview per composable, one per distinct state** (added in constitution 1.3.0, 2026-11-16) | ✅ PASS | Every new composable, including private sub-composables, has its per-state previews listed in the UI contracts ([transactions-ui.md](./contracts/transactions-ui.md) "Previews" lines, [design-system-components.md](./contracts/design-system-components.md) rules). Pressed and focused DS states are driven through an `interactionSource` parameter. Exempt: `TransactionListRoot` and `CreateTransactionRoot` (their Screens carry the previews), `ObserveAsEvents`, and the `SpendLessIcons` getters. |

**Post-design re-check (after Phase 1)**: unchanged, all ✅. Row 8 was added on 2026-11-16 after the constitution amendment; no design change was needed. The design added no new modules beyond R1 and no unjustified deviations.

**Housekeeping noted (not a gate failure)**: AGENTS.md §6 still names fileKey `ftCIcpiLxrMhckuzDmYQWL`. The constitution's `xSFwRCFwe13SK2kAq85SLj` wins (Governance). An AGENTS.md update is due, as already listed in the constitution's TODOs.

## Project Structure

### Documentation (this feature)

```text
specs/001-transactions/
├── plan.md              # This file
├── research.md          # Phase 0: Figma fetch log, decisions R1–R14, inconsistencies F1–F8
├── data-model.md        # Phase 1: domain, Room, UI models, validation → tests
├── quickstart.md        # Phase 1: build/test/manual/Figma/security validation
├── contracts/
│   ├── design-system-components.md   # DS API + Figma token tables
│   ├── transactions-ui.md            # Screens, feature components, node map, verify targets
│   └── domain-interfaces.md          # Domain/data/DI contracts
└── tasks.md             # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
build-logic/convention/src/main/kotlin/
├── AndroidApplicationConventionPlugin.kt
├── AndroidLibraryConventionPlugin.kt
├── AndroidFeatureConventionPlugin.kt      # library + compose + koin + core deps
├── ComposeConventionPlugin.kt             # compiler + BOM
├── DomainModuleConventionPlugin.kt        # pure Kotlin/JVM + JUnit5
├── RoomConventionPlugin.kt                # Room + KSP + schema dir
└── KoinConventionPlugin.kt

app/src/main/java/com/plcoding/spendlesssample/
├── SpendLessApp.kt                        # startKoin, RecurringOccurrencesTrigger
├── MainActivity.kt                        # SpendLessTheme + NavHost
└── navigation/SpendLessNavHost.kt

core/domain/src/main/kotlin/.../core/domain/
├── util/{Result.kt, Error.kt, DataError.kt}
├── format/{AmountFormatPreferences.kt, AmountFormatPreferencesSource.kt}
└── time/CurrentDateProvider.kt

core/data/src/main/kotlin/.../core/data/
├── format/DefaultAmountFormatPreferencesSource.kt
├── time/SystemCurrentDateProvider.kt
└── di/CoreDataModule.kt

core/database/src/main/kotlin/.../core/database/
├── SpendLessDatabase.kt, Converters.kt
├── entity/{TransactionEntity.kt, RecurringSeriesEntity.kt}
├── dao/TransactionDao.kt
├── security/DatabasePassphraseProvider.kt
└── di/DatabaseModule.kt
core/database/schemas/                     # exported Room schema (v1)

core/presentation/src/main/kotlin/.../core/presentation/
├── UiText.kt, ObserveAsEvents.kt
└── format/{AmountFormatter.kt, AmountInputParser.kt}

core/design-system/src/main/
├── kotlin/.../core/designsystem/
│   ├── theme/{Color.kt, SpendLessColorScheme.kt, Type.kt, Theme.kt, Shadows.kt}
│   ├── icon/SpendLessIcons.kt
│   └── component/{SpendLessButton.kt, SpendLessIconButton.kt, SpendLessFloatingActionButton.kt,
│                  SpendLessSegmentedButton.kt, SpendLessTextField.kt, SpendLessSelect.kt,
│                  SpendLessTopAppBar.kt}
└── res/{font/figtree_*.ttf, drawable/ic_*.xml}   # regenerated from Figma (R5)

feature/transactions/domain/src/main/kotlin/.../transactions/domain/
├── model/{Transaction.kt, TransactionType.kt, ExpenseCategory.kt, RepeatFrequency.kt,
│          RecurringSeries.kt, TransactionDraft.kt, CreateTransactionError.kt}
├── TransactionLocalDataSource.kt
├── RecurrenceSchedule.kt, TransactionValidator.kt
└── usecase/{CreateTransaction.kt, MaterializeDueOccurrences.kt, StopRecurringSeries.kt}

feature/transactions/data/src/main/kotlin/.../transactions/data/
├── RoomTransactionDataSource.kt, TransactionMappers.kt
└── di/TransactionsDataModule.kt

feature/transactions/presentation/src/main/kotlin/.../transactions/presentation/
├── navigation/TransactionsNavigation.kt
├── list/{TransactionListScreen.kt, TransactionListViewModel.kt, TransactionListState.kt,
│         TransactionListAction.kt, model/TransactionUi.kt, model/DayGroupUi.kt,
│         component/TransactionRow.kt, component/DayHeader.kt,
│         component/StopRepeatingMenu.kt, component/StopRepeatingDialog.kt}
├── create/{CreateTransactionScreen.kt, CreateTransactionViewModel.kt, CreateTransactionState.kt,
│           CreateTransactionAction.kt, CreateTransactionEvent.kt,
│           component/CounterpartyInput.kt, component/AmountInput.kt, component/NoteInput.kt}
├── format/{DayHeaderFormatter.kt, RepeatLabelFormatter.kt}   # English, Locale.ENGLISH
├── mapper/{CategoryUiMapper.kt, TransactionUiMapper.kt}
└── di/TransactionsPresentationModule.kt

# Tests (mirrors of the above):
#   */src/test/...           JUnit5 JVM tests (formatters, parser, validator, schedule, use cases, ViewModels)
#   core/database/src/androidTest/...      TransactionDaoTest, EncryptedDatabaseTest
#   feature/transactions/presentation/src/androidTest/...  TransactionListScreenTest, CreateTransactionFlowTest
```

**Structure Decision**: This is the feature-layered Clean Architecture from `android-module-structure`, which AGENTS.md §2 makes mandatory. Domain modules are pure Kotlin/JVM. Features never depend on each other. All Gradle setup lives in `:build-logic` and every version lives in `gradle/libs.versions.toml`.

## Key Design Decisions (details in research.md)

1. **DS components on M3 bases, fully re-skinned from Figma (R3)**. The structural exceptions, where M3 cannot model the design, are:
   - Segmented button: `Surface(selected)` row.
   - Text field: `BasicTextField`.
   - Select: `Popup`-based.
2. **Shadows**: Three tinted shadow tokens via `Modifier.dropShadow` (R4). Elevation is never used for looks.
3. **Recurrence**: Runs in-process on app start and at the date change. The exactly-once guarantee comes from a unique index. Due dates are computed from the anchor (R10). WorkManager is not used.
4. **Formatting**: All four preferences come from one `AmountFormatPreferencesSource` binding (R12). Money is held as `Long` minor units.
5. **Sheet layout**: The filters block has a fixed height and bottom alignment, reproducing the income frame where Create does not move (R7).
6. **UI state**: Row expansion state lives in `TransactionListState`, and is not saved across visits (FR-031).

## Complexity Tracking

> Deviations from Figma or the constitution that need justification. The project owner approved them on 2026-11-13 where noted.

| Deviation | Why needed | Simpler alternative rejected because |
|---|---|---|
| **Note-typing UI has no Figma frame**. It reuses the idle "Add Note" row tokens as an inline, centred, multi-line field (R8) | Spec FR-016 requires note entry. No frame shows it. **The user chose "Reuse name-input style"** | Blocking the task would leave US1-5 unbuildable |
| **Stop-repeating menu and dialog have no Figma component**. They are composed from DS Select-list and Button tokens (R11) | Spec US6 / FR-037–040. `search_design_system` found no dialog or menu. **The user chose "Compose from DS parts"** | Blocking would leave US6 unbuildable |
| Export icon hidden (F8) | Spec FR-009 (recorded clarification) | — |
| Create drawn enabled in empty mockups, implemented disabled (F6) | Designer note plus FR-015 | — |
| Amount column `min 100dp` instead of fixed `100dp` (F3) | A fixed width cannot show 999,999,999.99 untruncated (edge case) | A fixed width breaks FR-007 |
| Top-bar icons follow the kit instance (circle, onSurface), not DS Icon button `406:3390` (F5, R6) | The screens' own instances differ from the DS page | Using the DS icon button would drift from the screen frames |
| Create-failure message shown in DS supporting-text style under Create (no Figma error design) | A storage failure (for example disk full) must not lose the input silently | A silent failure would leave the user unsure whether the transaction was saved |
| Day-header style derived in the feature (F4) | No Figma text style matches Medium 12/16 | Using `bodyXSmall` (Regular) would drift |
| `#180040` shadow tint as a DS-internal constant (R4) | Not a Figma variable, but used by three effects | — |
| First multi-module split happens inside this feature (R1) | AGENTS.md §2 makes the layout mandatory; this is the first feature | — |

## Risks

- **Decimal separator vs. IME**: `,` may be missing from some numeric keyboards. This is irrelevant with the default `.`, but matters for the future preferences feature (R8).
- **Compose BOM bump** may change the behaviour of existing M3 defaults. This is mitigated because every DS component overrides its visuals explicitly.
- **Emoji rendering** varies by device font (Noto Color Emoji vs. OEM fonts). It is accepted, since Figma uses emoji text, not assets.
- **Midnight trigger**: `ACTION_DATE_CHANGED` can be late on some OEMs. Mitigated by the clock-based timer merged into the same flow (R10).
