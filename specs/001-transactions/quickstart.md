# Quickstart: Validating the Transactions feature

This is a run-and-verify guide, not implementation. Contracts: [design-system-components](./contracts/design-system-components.md), [transactions-ui](./contracts/transactions-ui.md), [domain-interfaces](./contracts/domain-interfaces.md). Model: [data-model.md](./data-model.md).

## Prerequisites

- JDK 11+, Android SDK 36, and a portrait phone emulator or device (API 26+). Check with `android` CLI diagnostics (`android-cli` skill).
- The Figma MCP is authenticated: every UI check below fetches its node live (Constitution I).

## 1. Build gate (AGENTS §5.1)

```bash
./gradlew build            # all modules, all JVM unit tests, lint — must be green
```

Expected JVM test classes, all passing:

- `AmountFormatterTest`, `AmountInputParserTest`, `AmountFormatPreferencesTest`
- `TransactionValidatorTest`, `RecurrenceScheduleTest`
- `CreateTransactionTest`, `MaterializeDueOccurrencesTest`, `StopRecurringSeriesTest`
- `TransactionListViewModelTest`, `CreateTransactionViewModelTest`

## 2. Instrumented tests

```bash
./gradlew connectedDebugAndroidTest
```

| Test | Proves |
|---|---|
| `TransactionDaoTest` (in-memory Room) | Ordering, unique `(series_id, occurrence_date)`, IGNORE on duplicates, `markStopped` is idempotent |
| `EncryptedDatabaseTest` | The DB file cannot be opened as plain SQLite. Opening with the wrong passphrase fails (Constitution security) |
| `TransactionListScreenTest` | Grouping headers, note expand/collapse, long-press menu only on running series, confirmation flow |
| `CreateTransactionFlowTest` (E2E, Robot) | US1 and US4: Create is disabled until name and amount are valid; type switch keeps input; the created row appears under TODAY |

## 3. Manual scenarios (map to spec stories)

Install with `./gradlew installDebug` and launch. **All Transactions** is the start screen.

| # | Steps | Expected |
|---|---|---|
| US1 | Tap +. Type "Netflix", 15.99. Pick Entertainment. Tap Create | The sheet opens on Expense / Other / Does not repeat with the keyboard up. Create stays disabled until both fields are valid. Row "Netflix · Entertainment · -$15.99" appears under TODAY |
| US2 | Create several rows, then scroll | Headers TODAY / YESTERDAY / "MONTH D". The current header stays pinned. Long names show an ellipsis and the amount stays whole |
| US3 | Create a row with a multi-line note. Tap its badge twice | The row expands into a raised card showing the full note, and the badge turns strong. A second tap collapses it. Rows without a note have no badge |
| US4 | Switch to Income, enter "Rick" and 20 | Placeholder "Sender", green `$`, category hidden, Repeat and Create in the same place. The row is green `$20.00` labelled "Income" |
| US5 | Create "Gym" 45.99 Monthly. Advance the device date past the next due date (`adb shell date` on an emulator, or the system clock) and reopen | Exactly one Gym row on the due day. No future rows |
| US5-live | Keep the app open across midnight (set the clock to 23:59) | At 00:00 the headers shift and a due occurrence appears under TODAY without a refresh |
| US6 | Long-press a Gym row → Stop repeating → Stop. Advance the date | Menu, then dialog "Stop repeating Gym?". After Stop no new rows appear and the existing rows remain. Long-press on a one-time row does nothing |
| US7 | Automated, with no source edits: `AmountFormatPreferencesSwapTest` (instrumented) replaces the Koin `AmountFormatPreferencesSource` binding with a fake for each combination (parentheses; `,`/`.`; space thousands) and asserts the sheet and rows. JVM: `TransactionListViewModelTest` and `CreateTransactionViewModelTest` run with a fake source per combination | The sheet reads `($ 00,00 )`, rows read `($1.234,50)` and `$12 000.00`. Nothing else in the feature changes (FR-035, SC-010) |

## 4. Figma verification (AGENTS §5.2)

For each row of the "UI verification targets" table in [transactions-ui.md](./contracts/transactions-ui.md):

1. Run `android-preview-figma-verify` (no device): preview vs node.
2. Run `android-ui-verify` (structural, ±4dp), then `android-ui-screenshot-verify` (visual), on the live screen.

Expected deviations to ignore, all documented in plan Complexity Tracking:

- Export icon absent.
- Create disabled on the empty form.
- Amount column min-width.
- Note-typing state.
- Stop menu and dialog.

## 5. Security spot-checks

```bash
adb shell run-as com.plcoding.spendlesssample ls databases/        # DB present
adb shell run-as com.plcoding.spendlesssample head -c 16 databases/spendless.db | xxd   # NOT "SQLite format 3"
adb logcat -c                                                       # after any instrumented run, before the manual scenarios
adb logcat -d -v raw | grep -v "adbd service requested" | grep -iE "netflix|15\.99|gym"   # no matches
```

- `-v raw` prints message bodies only. Without it, timestamps such as `11:52:15.998` match `15\.99`.
- `adbd service requested` lines echo your own `adb shell input text …` commands, not app logs.
- Clear logcat first: instrumented test names (for example `…StopRepeatingGym…`) also match.

## 6. Performance (SC-005)

`TransactionListScrollTest` (instrumented) seeds 1,000 transactions over 12 months into the fake data source. It scrolls to the last item, then back to the top.

Recorded 2026-11-17 (T137):

- Both tests are green on the Pixel 10 AVD (API 37).
- The list's `stickyHeader` / `items` set a `key` and `contentType`.
- `TransactionListState`, `DayGroupUi`, `TransactionUi` and `StopConfirmationUi` are `@Immutable`.
- Compose compiler reports (via a temporary init script, not checked in) flagged one unstable parameter on the list path: `TransactionList(groups: List<DayGroupUi>)`. It is now `ImmutableList`. The only unstable parameters left are ViewModels on Root composables and a preview-only helper.
- **Not run (owner decision 2026-11-17):** the release-build `dumpsys gfxinfo` jank measurement.
  - Release has no signing config.
  - On 2026-11-16 the owner ruled out a seeder for the encrypted DB.
