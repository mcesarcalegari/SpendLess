# /verify review — 001-transactions

- **Date:** 2026-11-17
- **Branch:** `001-transactions` @ `cf7e527`
- **Base:** `main` (`d98aae9`, the spec commit). The skill's default base, `master`, does not exist in this repo. Comparing against `main` covers only this feature's 51 commits (233 files) and leaves out the Spec Kit setup that is already on `main`.

## Verdict

**PASS**

None of the three reviewers raised a BLOCKER or MAJOR finding, and every targeted Gradle test passed. Because the reviewers raised nothing, there was nothing for the per-issue validation (Stage 6) or the audit pass (Stage 8) to process, so neither ran.

## Tests

| Result | Task | Notes |
|---|---|---|
| PASS | `testDebugUnitTest` (all Android modules) | Gradle reported these up-to-date. It only does that when the same inputs already passed. |
| PASS | `:core:domain:test` | JVM module (reported up-to-date) |
| PASS | `:feature:transactions:domain:test` | JVM module (reported up-to-date) |
| PASS | `:app:connectedDebugAndroidTest` | Pixel 10 AVD, reported as `Pixel_10(AVD) - 17` |
| PASS | `:core:database:connectedDebugAndroidTest` | |
| PASS | `:feature:transactions:data:connectedDebugAndroidTest` | |
| PASS | `:feature:transactions:presentation:connectedDebugAndroidTest` | |

The instrumented runs covered 68 tests in four batches (16 + 9 + 39 + 4), with no failures. I'm not certain which batch belongs to which module.

## Reviewers

| Reviewer | Model | BLOCKER/MAJOR findings |
|---|---|---|
| skip-checker | haiku | `skip: false`. The diff touches production Kotlin, so it has to be reviewed. |
| bug-reviewer | opus | 0 |
| convention-reviewer | sonnet | 0 |
| security-reviewer | opus | 0 |

## Findings

None at BLOCKER or MAJOR severity.

## Observations below the reporting bar

The reviewers raised these themselves, but each one is MINOR, a design choice, or outside the spec. None of them goes through the `/verify` gate or affects the verdict. They're listed so you can decide whether any of them deserves a follow-up.

### bug-reviewer

1. **Catch-up after a failure does not run for a series stopped afterwards.**
   - **What happens:** if a catch-up run fails (for example, the disk is full) and the user then stops the series, the due dates it missed up to the stop day are never added.
   - **Why:** `runningSeries()` filters out stopped series, so the `minOf(stoppedOn, today)` guard in `MaterializeDueOccurrences` never takes effect.
   - **Why it looks intended:** FR-038 only forbids occurrences *after* the stop day, and `MaterializeDueOccurrencesTest` deliberately asserts "a stopped series adds nothing, including a catch-up". If the intent is that every day up to the stop day gets its occurrence, this needs a spec decision.
2. **`reEmittingTheSameDateAddsNone` is a weak test.** It mainly exercises the database's unique `(series_id, occurrence_date)` index rather than the trigger's own logic. It isn't wrong, but it would still pass if the trigger stopped deduplicating.
3. **Keystore work on the main thread at startup.** `koin.get()` in `SpendLessApp.onCreate` reads the database passphrase on the main thread, which does AndroidKeyStore work during startup. This is a design/performance point.
4. **Time-zone mismatch in the trigger.** The trigger compares the device's current date with due dates computed in the series' zone. After the device's time zone changes, the two can disagree (see security-reviewer note 1).

### security-reviewer

1. **Time-zone change can stamp an occurrence on the wrong day.** If the device's time zone changes after a series starts, an occurrence created at midnight can get a `recordedAt` that falls on the day before its due date in the series' zone. The spec does not cover changing time zones.
2. **`MaterializeDueOccurrences` reads every transaction** to find the series originals. This is a performance point and will matter as history grows, but it isn't a correctness problem.

### convention-reviewer

1. **`SpendLessNavHost` has no `@Preview`.** Its KDoc explains why: it draws no UI of its own and the screens it hosts need Koin. That reason isn't one of the exemptions Constitution Principle VIII lists, though, so either add the exemption to the constitution or add a stub preview.

## What the reviewers confirmed

### Security (FR-027, research R13)

- **Passphrase:** `DatabasePassphraseProvider` creates 32 bytes with `SecureRandom` and wraps them with an AndroidKeyStore AES-256-GCM key. Randomized encryption is required and the tag is 128 bits. Only the IV and ciphertext are stored, in `noBackupFilesDir`.
- **Passphrase not zeroed:** this is deliberate and documented in `DatabaseModule.kt`. `SupportOpenHelperFactory` keeps a reference to the array, so zeroing it would break later opens.
- **Backups:** `backup_rules.xml` and `data_extraction_rules.xml` leave out the database, its `-wal`, `-shm` and `-journal` files, and the wrapped key, for both cloud backup and device transfer.
- **Room builder:** plain `Room.databaseBuilder` is only used inside `buildEncryptedDatabase` (SQLCipher).
- **Exposure:** only the launcher activity is exported. The date-change receiver uses `RECEIVER_NOT_EXPORTED`.
- **Logging:** production sources contain no `Log.*`, `println`, Timber or `printStackTrace` calls.
- **Build files:** they contain no signing secrets, tokens, cleartext-traffic flags or debuggable flags.
- **SQLCipher exceptions:** SQLCipher throws the framework `SQLiteException` and `SQLiteFullException` classes, and the bug-reviewer checked this against the sqlcipher-android 4.20.0 AAR. So `safeDbCall` and `endOnStorageFailure` really do map storage errors to `DataError`.

### Behavior against the contracts (`contracts/domain-interfaces.md`)

- `recordedAt = min(due + anchorTime, now)`.
- Each occurrence is added exactly once, through the unique `(series_id, occurrence_date)` index plus `IGNORE` on insert. `insertWithSeries` and `materialize` each run in a single database transaction.
- The last processed date never moves back. Due dates are always computed from the anchor.
- A stopped series is guarded and can never be restarted.
- Disk-full and unknown errors are mapped as the contract says. Read flows end instead of throwing on a storage failure.
- Every handler on a visible control is wired up. The `= {}` defaults only appear in previews and in optional parameters whose call sites pass real handlers.
- ✕, scrim, swipe and back all close the sheet and reset the form. Back finishes the activity when there is no earlier screen.
- In the Create ViewModel, a double tap can't save twice. The input cap of 999,999,999.99 matches the validator's maximum of 99,999,999,999 minor units.

### Conventions (AGENTS.md, constitution)

- Versions only appear in `libs.versions.toml`.
- `try`/`catch` only appears in the data layer.
- Per-state previews and `Figma:` node links are present. `StopRepeatingDialog` and `StopRepeatingMenu` have no Figma node, and their comments say so and cite research R11.
- Raw `Color(0x…)` values only appear in the design-system token file.
- The tests use no mocking libraries, no `Thread.sleep` and no `@Ignore`.

### Test coverage

| Requirement | Covered by |
|---|---|
| FR-009 | `onlyBackAndAddAreActionableSoNoExportActionIsShown` |
| FR-012 / FR-016 | VM tests for the 50 and 500 character caps and the whitespace-only name |
| FR-018 | `switchingToIncomeShowsSenderHidesTheCategory…` |
| FR-021 | `sheetOpensWithDefaultsAmountFocusedAndKeyboardShown` |
| FR-022a | `disk full keeps the input, shows the storage message and emits nothing` |
| FR-023 | `closeDiscardsTheInputWithoutCreatingARow` |
| FR-024 | `name, amount, note and repeat are kept across Expense to Income and back` |
| FR-025, FR-025a, FR-025b | `MaterializeDueOccurrencesTest` suite |
| FR-025c | `RecurringOccurrencesTriggerTest` |
| FR-027 | `EncryptedDatabaseTest` (plain-header check, wrong and all-zero passphrase checks) |
| FR-028 to FR-031 | expand and collapse tests in the list screen and VM |
| FR-032 to FR-035 | formatter and parser tests, VM tests, `AmountFormatPreferencesSwapTest` |
| FR-037 to FR-040 | long-press, menu, dialog and stop tests at both UI and VM level |
| Data contract error mapping | `RoomTransactionDataSourceTest` (`diskFull_mapsToDiskFull`, `observeFlows_endInsteadOfThrowing_onStorageFailure`) |

Every test file that `tasks.md` names exists, apart from the template tests that were deliberately deleted.

## Mutation testing

Three `mutation-worker` agents ran in parallel, each in its own detached git worktree at `cf7e527`, following `kotlin-mutation-testing`. Every unit test class passed before mutation. Each mutation reran the whole test class, and each mutated file was restored afterwards. All three worktrees ended clean.

| Worker | Module(s) | Classes | Tried | Caught | Survived |
|---|---|---|---|---|---|
| domain | `:feature:transactions:domain` | 7 | 74 | 55 | 19 (4 equivalent, 1 near-equivalent, 14 real gaps) |
| core | `:core:data`, `:core:domain`, `:core:presentation`, `:feature:transactions:data` | 7 | 102 | 99 | 3 |
| presentation | `:feature:transactions:presentation` | 7 | — | — | **not finished**: the session ended mid-run, so there are no results yet |

The domain worker's summary table and its per-mutation list disagreed. The domain counts above come from the per-mutation list. Line numbers refer to the unmutated files.

### Surviving mutations: `:feature:transactions:domain`

Paths are under `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/`.

**The stop-day limit in `MaterializeDueOccurrences` is never exercised.** This is the same point as bug-reviewer note 1.

| # | Location | Mutation | Missing test |
|---|---|---|---|
| M8 | `usecase/MaterializeDueOccurrences.kt:58` | `minOf(it, today)` → `it` | A fake whose `runningSeries()` returns a series with `stoppedOn < today`, asserting no occurrence is added after `stoppedOn` |
| M8b | `usecase/MaterializeDueOccurrences.kt:58` | `stoppedOn?.let { minOf(it, today) } ?: today` → `today` | same as M8 |
| M14 | `usecase/MaterializeDueOccurrences.kt:68` | `toInclusive = upTo` → `today` | same as M8 |
| M23 | `usecase/MaterializeDueOccurrences.kt:84` | `lastMaterialized = upTo` → `today` | same as M8, and also assert the stored last-processed date equals the stop date |

The fake's `runningSeries()` filters out stopped series, so `upTo == today` in every test. Either add the test above, or remove the guard as unreachable once the spec settles whether a stopped series catches up to its stop day.

**Other gaps in `MaterializeDueOccurrences`:**

| # | Location | Mutation | Missing test |
|---|---|---|---|
| M2 | `:36` | `if (running.isEmpty()) return Success` → `if (false)` | With no running series and a read flow that never emits, the result is `Success` |
| M3 | `:41` | `?: return Error(UNKNOWN)` → `?: return Success` | A read flow that ends without emitting while series exist gives `Error(UNKNOWN)` |
| M5 | `:45` | `?: return@forEach` → `?: return Success` | An orphan series (no original) followed by a valid series: the valid one is still processed |
| M6 | `:47` | `result is Error && firstError == null` → `result is Error` | Two failing series with different errors: the first error is returned |
| M11 | `:65` | `anchor = series.anchorDate` → `series.lastMaterializedDate` | A monthly or yearly series whose last processed date ≠ its anchor (e.g. anchor Jan 31, last processed Feb 28) |
| M13 | `:67` | `fromExclusive = series.lastMaterializedDate` → `series.anchorDate` | A recording fake asserting the occurrences passed to `materialize` exclude dates on or before the last processed date. The current fake removes duplicates, which hides this |
| M18 | `:73` | `id = 0` → `id = original.id` | Assert each new occurrence has `id == 0`. The test currently overwrites ids with `copy(id = 0)` (test line 77), which hides this |
| M20 | `:76` | `zoneId = series.zoneId` → `original.zoneId` | A series whose zone differs from its original's: the occurrence takes the series zone |
| M9 | `:60` | `upTo <= lastMaterializedDate` → `<` | Near-equivalent: a same-day rerun makes an empty `materialize` call and leaves the stored state unchanged. Only matters if that extra call matters to you |

**`CreateTransaction` and `TransactionValidator`:**

| # | Location | Mutation | Missing test |
|---|---|---|---|
| C3 | `usecase/CreateTransaction.kt:19` | `original = transaction` → `transaction.copy(note = null)` | Create a repeating transaction with a note and assert the stored original equals the input |
| V17 | `TransactionValidator.kt:21-22` | `NAME_TOO_LONG` and `AMOUNT_NOT_POSITIVE` checks swapped | A 51-character name with amount 0 returns `NAME_TOO_LONG`, which pins the rule order |

**Equivalent mutations, not gaps:**
- R1 and R10 (`RecurrenceSchedule.kt:26`): the empty-window guard can be loosened or removed and the result is still an empty list.
- R8 (`RecurrenceSchedule.kt:38`): `plusYears(n)` → `plusMonths(12 * n)` gives the same date.
- V16: a no-op rewrite.

### Surviving mutations: `:core:data`

Paths are under `core/data/src/main/kotlin/com/plcoding/spendlesssample/core/data/time/`.

| Location | Mutation | Missing test |
|---|---|---|
| `SystemCurrentDateProvider.kt:38` | `coerceAtLeast(1)` → `coerceAtLeast(3_600_000)` | A clock at 23:30 emits the new date after 30 minutes, not after an hour |
| `SystemCurrentDateProvider.kt:37` | `atStartOfDay(now.zone)` → `atStartOfDay(ZoneOffset.UTC)` | Run the midnight timer under a non-UTC zone (e.g. `America/Sao_Paulo`). The current timer tests only use UTC |
| `SystemDefaultZoneClock.kt:22` | `millis()` → `0L` | `millis()` falls between `System.currentTimeMillis()` readings taken before and after the call |

`AmountFormatPreferences`, `DataErrorUiText`, `AmountFormatter`, `AmountInputParser` and `TransactionMappers` caught every mutation.

### Still to do

- Run the mutation pass on the 7 `:feature:transactions:presentation` classes: both ViewModels, `GroupedAmountTransformation`, `DayHeaderFormatter`, `RepeatLabelFormatter`, `CategoryUiMapper` and `TransactionUiMapper`.

## Notes

- The convention-reviewer spot-checked files with targeted greps rather than reading every changed line. Following AGENTS.md, it did not read the skill files directly.
- The bug-reviewer read every production file in the transactions feature, `:core:*` and `:app` in full. It read the fakes and `RecurringOccurrencesTriggerTest` in full and skimmed the larger ViewModel and use-case tests.
- This review does not cover the Figma UI verification (`android-ui-verify` / `android-ui-screenshot-verify`) or a full `./gradlew build` (which includes lint). Mutation testing is covered above, except for the presentation module.
