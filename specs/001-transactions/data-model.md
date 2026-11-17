# Data Model: Transactions (Phase 1)

**Feature**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

The model has three layers. Domain models are pure Kotlin and live in `:feature:transactions:domain`, or in `:core:domain` when they are shared. Persistence is a Room entity in `:core:database`, encrypted with SQLCipher. Presentation adds UI models in `:feature:transactions:presentation`, each suffixed `Ui`.

---

## 1. Domain models

### `Transaction` (`:feature:transactions:domain`)

| Field | Type | Rules | Spec |
|---|---|---|---|
| `id` | `Long` | Assigned by storage; `0` before insert | — |
| `type` | `TransactionType` | `EXPENSE` or `INCOME` | FR-011 |
| `counterparty` | `String` | Trimmed. Must not be blank after trimming. 1–50 chars | FR-012 |
| `amountMinor` | `Long` | Always positive: 1 ≤ value ≤ 99_999_999_999 (0.01 – 999,999,999.99). The sign comes from `type` | FR-013 |
| `note` | `String?` | `null` when empty after trimming. ≤ 500 chars | FR-016 |
| `category` | `ExpenseCategory?` | Required (non-null) when `type == EXPENSE`; must be `null` when `type == INCOME` | FR-017, FR-018 |
| `repeat` | `RepeatFrequency` | `DOES_NOT_REPEAT` for every occurrence | FR-019, FR-025b |
| `recordedAt` | `Instant` | Creation instant. For an occurrence: `min(due date + the original's local time of day, the instant it is materialised)` | FR-002, FR-022 |
| `zoneId` | `ZoneId` | Zone at recording. Used to compute the local date and time-of-day anchor | FR-002 |
| `seriesId` | `Long?` | `null` for a one-time transaction. Equals its own `id` for a recurring original. The original's `id` for an occurrence | FR-025b, FR-037 |
| `occurrenceDate` | `LocalDate?` | Due date for occurrences only; `null` otherwise | FR-025a |

`TransactionType`: `EXPENSE`, `INCOME`.

`ExpenseCategory` (FR-026, alphabetical, fixed): `CLOTHING_AND_ACCESSORIES` 👔, `EDUCATION` 🎓, `ENTERTAINMENT` 💻, `FOOD_AND_GROCERIES` 🍕, `HEALTH_AND_WELLNESS` ❤️, `HOME` 🏠, `OTHER` ⚙️. The default is `OTHER`.

The emoji and display name map from the enum in presentation. The domain holds only the enum.

`RepeatFrequency` (FR-019): `DOES_NOT_REPEAT`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`. The default is `DOES_NOT_REPEAT`.

### `RecurringSeries` (`:feature:transactions:domain`)

This is a read model of a series. It is derived from the original transaction plus its series row.

| Field | Type | Rules |
|---|---|---|
| `seriesId` | `Long` | Equals the original transaction's `id` |
| `frequency` | `RepeatFrequency` | Never `DOES_NOT_REPEAT` |
| `anchorDate` | `LocalDate` | The original's local date. Fixes the weekday, day-of-month and month-day |
| `anchorTime` | `LocalTime` | The original's local time of day |
| `stoppedOn` | `LocalDate?` | `null` while running. Once set it is never cleared (FR-039) |
| `lastMaterializedDate` | `LocalDate` | Highest due date that has been processed. Starts at `anchorDate` |

**State transitions**

```
RUNNING ──(user confirms "Stop", FR-038)──▶ STOPPED(stoppedOn = today)
STOPPED ── terminal (cannot be restarted in this feature, FR-039)
```

### `RecurrenceSchedule` (pure function, `:feature:transactions:domain`)

`dueDatesBetween(anchor: LocalDate, frequency, fromExclusive: LocalDate, toInclusive: LocalDate): List<LocalDate>`

- Each due date is computed **from the anchor**, as anchor + n periods, clamped to the month length. It is never computed from the previous due date.
- Example, monthly on the 31st: Jan 31 → Feb 28 (or 29) → Mar 31 → Apr 30.
- Example, yearly on Feb 29: Feb 28 in non-leap years, Feb 29 in leap years.
- The caller passes `toInclusive = min(today, stoppedOn ?: today)` (FR-038).

### Shared: `AmountFormatPreferences` (`:core:domain`)

| Field | Type | Default (FR-034) | Rule |
|---|---|---|---|
| `currencySymbol` | `String` | `"$"` | Always placed before the number |
| `negativeStyle` | `NegativeStyle` | `MINUS_PREFIX` | `MINUS_PREFIX` → `-$10`; `PARENTHESES` → `($10)` |
| `decimalSeparator` | `Char` | `'.'` | Must differ from `thousandsSeparator` (`init` check; edge case) |
| `thousandsSeparator` | `Char` | `','` | Can be `' '` (FR-032 example `$12 000.00`) |

It is exposed through `AmountFormatPreferencesSource.preferences: Flow<AmountFormatPreferences>`, which is the single shared place (FR-033). Its implementation lives in `:core:data` and emits the defaults.

### Shared: `Result<T, E>`, `EmptyResult<E>`, `DataError` (`:core:domain`)

These follow `android-error-handling`. Feature errors:

- `TransactionValidationError : Error` (enum): `NAME_BLANK`, `NAME_TOO_LONG`, `AMOUNT_NOT_POSITIVE`, `AMOUNT_TOO_LARGE`, `NOTE_TOO_LONG`, `CATEGORY_MISSING_FOR_EXPENSE`, `CATEGORY_NOT_ALLOWED_FOR_INCOME`. Returned only by `TransactionValidator`.
- Storage failures are `DataError.Local` (`DISK_FULL`, `NOT_FOUND`, `UNKNOWN`), returned by data sources and use cases. Each `Result` carries a single error type (android-error-handling).

---

## 2. Domain interfaces (`:feature:transactions:domain`)

Only the signatures are listed here. The full contract is in [contracts/domain-interfaces.md](./contracts/domain-interfaces.md).

- `TransactionLocalDataSource`: see the contract (`observeTransactions`, `insert`, `insertRecurring`, `runningSeries`, `materialize`, `stopSeries`, `observeRunningSeriesIds`).
- Use cases (plain classes, constructor-injected):
  - `TransactionValidator` (sync) followed by `CreateTransaction` (persists a validated `Transaction`). When `repeat != DOES_NOT_REPEAT`, `CreateTransaction` calls `insertRecurring`; the DAO's `insertWithSeries` sets `seriesId = id` and creates the series row atomically.
  - `MaterializeDueOccurrences(today)`: catch-up and live (FR-025, FR-025a, FR-025c).
  - `StopRecurringSeries(seriesId, today)`: FR-038.
  - `ObserveTransactions`: `Flow<List<Transaction>>`, newest first.

---

## 3. Persistence (`:core:database`, SQLCipher-encrypted Room)

### `transactions` table: `TransactionEntity`

| Column | Type | Constraint |
|---|---|---|
| `id` | INTEGER | PK, autoGenerate |
| `type` | TEXT | NOT NULL. `EXPENSE`/`INCOME`, stored as the enum name (no TypeConverter: `:core:database` cannot see the domain enums; mapping happens in `:feature:transactions:data`) |
| `counterparty` | TEXT | NOT NULL |
| `amount_minor` | INTEGER | NOT NULL, > 0 (enforced by `TransactionValidator`; Room has no CHECK) |
| `note` | TEXT | NULL |
| `category` | TEXT | NULL, enum name |
| `repeat` | TEXT | NOT NULL, enum name |
| `recorded_at_epoch_ms` | INTEGER | NOT NULL, **indexed** (sort key, FR-002) |
| `zone_id` | TEXT | NOT NULL |
| `series_id` | INTEGER | NULL, indexed |
| `occurrence_date_epoch_day` | INTEGER | NULL |

Unique index: `(series_id, occurrence_date_epoch_day)`. SQLite treats NULLs as distinct, so one-time rows and originals never collide. Occurrences can never be duplicated (FR-025a).

### `recurring_series` table: `RecurringSeriesEntity`

| Column | Type | Constraint |
|---|---|---|
| `series_id` | INTEGER | PK; FK → `transactions.id` |
| `frequency` | TEXT | NOT NULL, enum name |
| `anchor_epoch_day` | INTEGER | NOT NULL |
| `anchor_seconds_of_day` | INTEGER | NOT NULL |
| `zone_id` | TEXT | NOT NULL |
| `stopped_on_epoch_day` | INTEGER | NULL |
| `last_materialized_epoch_day` | INTEGER | NOT NULL |

### DAO (`TransactionDao`)

- `observeAll(): Flow<List<TransactionEntity>>`: `ORDER BY recorded_at_epoch_ms DESC`.
- `insert(entity): Long`.
- `@Insert(onConflict = IGNORE) insertOccurrences(list)`.
- `runningSeries(): List<RecurringSeriesEntity>`: `WHERE stopped_on_epoch_day IS NULL`.
- `markStopped(seriesId, epochDay)`: only when `stopped_on_epoch_day IS NULL`.
- `updateLastMaterialized(seriesId, epochDay)`.
- `@Transaction` wrappers: `insertWithSeries`, `materialize(seriesId, occurrences, newLast)`.

Mappers `TransactionEntity ↔ Transaction` live in `:feature:transactions:data` as extension functions.

**Migrations**: schema v1. `exportSchema = true`, with the schema JSON checked in.

---

## 4. Presentation models (`:feature:transactions:presentation`)

### `TransactionListState`

| Field | Type | Notes |
|---|---|---|
| `groups` | `ImmutableList<DayGroupUi>` | Newest day first |
| `expandedIds` | `ImmutableSet<Long>` | Not saved across visits (FR-031) |
| `isCreateSheetOpen` | `Boolean` | Persisted in `SavedStateHandle`, so the sheet survives process death |
| `contextMenuFor` | `Long?` | Row id whose long-press menu is open (FR-037) |
| `stopConfirmationFor` | `StopConfirmationUi?` | Non-null shows the dialog (FR-038) |

`DayGroupUi(dateKey: Long, header: UiText /* StringResource "TODAY"/"YESTERDAY", or DynamicString "JANUARY 9" (Locale.ENGLISH, uppercased) */, items: ImmutableList<TransactionUi>)`

`TransactionUi`

| Field | Type |
|---|---|
| `id` | `Long` |
| `type` | `TransactionType` |
| `iconEmoji` | `String` (category emoji, or 💰 for income) |
| `title` | `String` (counterparty) |
| `label` | `UiText` (category name, or "Income") |
| `formattedAmount` | `String` (from `AmountFormatter`, for example `-$45.99` or `($45.99)`) |
| `note` | `String?` |
| `seriesId` | `Long?` |
| `isInRunningSeries` | `Boolean` |

`StopConfirmationUi(seriesId: Long, name: String)`

`TransactionListAction`: `OnCreateClick`, `OnCreateSheetDismiss`, `OnNoteBadgeClick(id)`, `OnRowLongPress(id)`, `OnContextMenuDismiss`, `OnStopRepeatingClick`, `OnStopConfirm`, `OnStopCancel`.

Back is not routed through actions. The Root calls `onNavigateBack` directly. No `Event` type is needed.

### `CreateTransactionState`

| Field | Type | Default | Notes |
|---|---|---|---|
| `type` | `TransactionType` | `EXPENSE` | FR-011 |
| `name` | `String` | `""` | Raw; trimmed on save. Input capped at 50 |
| `amountInput` | `String` | `""` | Raw digits plus at most one preferred separator, at most 2 decimals and 9 whole digits (FR-013) |
| `amountParts` | `AmountPartsUi` | — | `prefix` (`-$`, `($`, `$`), `suffix` (`)` or `""`), `isPlaceholder`, `signTone` (`EXPENSE`/`INCOME`) |
| `note` | `String` | `""` | Capped at 500 |
| `isNoteEditing` | `Boolean` | `false` | Shows the note input (approved deviation, research R8) |
| `category` | `ExpenseCategory` | `OTHER` | Kept while the type is `INCOME` (edge case) |
| `repeat` | `RepeatFrequency` | `DOES_NOT_REPEAT` | |
| `repeatOptions` | `ImmutableList<RepeatOptionUi>` | — | Long labels from today ("Weekly on Monday", "Monthly on the 14th", "Yearly on Feb 14th"). Always English |
| `openSelect` | `OpenSelect?` | `null` | `CATEGORY`, `REPEAT`, or `null` |
| `isCreateEnabled` | `Boolean` | `false` | Derived: name not blank **and** amount valid (FR-015) |
| `error` | `UiText?` | `null` | Storage failure on create. The sheet stays open |

`CreateTransactionAction`: `OnTypeSelect(type)`, `OnNameChange(text)`, `OnAmountChange(text)`, `OnAddNoteClick`, `OnNoteChange(text)`, `OnNoteFocusLost`, `OnCategorySelectClick`, `OnCategoryPick(category)`, `OnRepeatSelectClick`, `OnRepeatPick(freq)`, `OnSelectDismiss`, `OnCreateClick`, `OnCloseClick`.

`CreateTransactionEvent`: `Created`. This event is justified because only the ViewModel knows when the async insert has succeeded and the sheet may close (FR-022). Close (✕) is handled directly by the parent's callback.

**Process death**: the ViewModel saves `type`, `name`, `amountInput`, `note`, `category` and `repeat` in `SavedStateHandle` (android-presentation-mvi, Process Death). The form is **reset on close** (`OnCloseClick`, sheet dismiss, or after `Created`), never on open. A restored form therefore reappears in the restored sheet, and a freshly opened sheet is always empty.

---

## 5. Validation rules (summary → tests)

| Rule | Where | Test |
|---|---|---|
| Name trimmed, not blank, ≤ 50 | `TransactionValidator` + input cap in the ViewModel | `TransactionValidatorTest`, `CreateTransactionViewModelTest` |
| Amount > 0 and ≤ 999,999,999.99, ≤ 2 decimals, digits plus the preferred separator only | `AmountInputParser` | `AmountInputParserTest` |
| Category present iff expense | `TransactionValidator` | `TransactionValidatorTest` |
| Note ≤ 500 | ViewModel cap + validator | both |
| Separators distinct | `AmountFormatPreferences.init` | `AmountFormatPreferencesTest` |
| Due dates anchored and clamped | `RecurrenceSchedule` | `RecurrenceScheduleTest` (Jan 31 chain, Feb 29, weekly, daily, stoppedOn bound) |
| Exactly once | Unique index + IGNORE | `TransactionDaoTest` (instrumented, in-memory) |
