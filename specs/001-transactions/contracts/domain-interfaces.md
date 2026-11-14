# Contract: Domain & data interfaces

All signatures are pure Kotlin (no Android types), in `:core:domain` or `:feature:transactions:domain`. Implementations live in the `data` modules and are bound in Koin. Errors use `Result<T, E>` (`android-error-handling`).

## `:core:domain`

```kotlin
interface Error
sealed interface DataError : Error {
    enum class Local : DataError { DISK_FULL, NOT_FOUND, UNKNOWN }
}
sealed interface Result<out D, out E : Error> { /* Success / Error + map, onSuccess, onFailure, asEmptyResult */ }
typealias EmptyResult<E> = Result<Unit, E>

enum class NegativeStyle { MINUS_PREFIX, PARENTHESES }
data class AmountFormatPreferences(
    val currencySymbol: String,
    val negativeStyle: NegativeStyle,
    val decimalSeparator: Char,
    val thousandsSeparator: Char,
) {
    init { require(decimalSeparator != thousandsSeparator) }
    companion object { val Default = AmountFormatPreferences("$", NegativeStyle.MINUS_PREFIX, '.', ',') }
}

/** The one shared place for amount formatting preferences (FR-033). */
interface AmountFormatPreferencesSource { val preferences: Flow<AmountFormatPreferences> }

/** Emits today's local date now and on every date change (midnight, time/zone change). */
interface CurrentDateProvider { val today: Flow<LocalDate> }
```

Bindings in `:core:data`:

- `DefaultAmountFormatPreferencesSource` → `flowOf(AmountFormatPreferences.Default)`.
- `SystemCurrentDateProvider(context, clock)` → research R10.
- `Clock` → `Clock.systemDefaultZone()`.

## `:core:presentation`

```kotlin
class AmountFormatter {
    fun format(amountMinor: Long, isNegative: Boolean, prefs: AmountFormatPreferences): String   // "-$1,234.50" / "($1,234.50)"
    fun parts(amountMinor: Long?, isNegative: Boolean, prefs: AmountFormatPreferences): AmountParts // prefix "-$"|"($"|"$", number, suffix ")"|""
    fun groupInput(raw: String, prefs: AmountFormatPreferences): String                          // thousands grouping for the field
}
class AmountInputParser {
    /** Returns the accepted raw text, or null to reject the keystroke. ≤9 whole digits, ≤2 decimals, only prefs.decimalSeparator. */
    fun accept(current: String, proposed: String, prefs: AmountFormatPreferences): String?
    fun toMinorUnits(raw: String, prefs: AmountFormatPreferences): Long?   // null when empty or 0
}
sealed interface UiText { /* DynamicString / StringResource */ }
@Composable fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit)
```

## `:feature:transactions:domain`

```kotlin
interface TransactionLocalDataSource {
    fun observeTransactions(): Flow<List<Transaction>>                       // newest first (FR-001, FR-002)
    suspend fun insert(transaction: Transaction): Result<Long, DataError.Local>
    suspend fun insertRecurring(original: Transaction, frequency: RepeatFrequency): Result<Long, DataError.Local> // tx row + series row, atomic
    suspend fun runningSeries(): Result<List<RecurringSeries>, DataError.Local>
    suspend fun materialize(seriesId: Long, occurrences: List<Transaction>, lastMaterialized: LocalDate): EmptyResult<DataError.Local> // IGNORE dupes, atomic
    suspend fun stopSeries(seriesId: Long, stoppedOn: LocalDate): EmptyResult<DataError.Local>
    fun observeRunningSeriesIds(): Flow<Set<Long>>                           // drives isInRunningSeries (FR-037)
}

/** Pure, synchronous. One error type per Result (android-error-handling). */
class TransactionValidator(private val clock: Clock) {
    fun validate(draft: TransactionDraft): Result<Transaction, TransactionValidationError> // stamps recordedAt/zoneId = now
}

/** Persists an already-validated transaction. Storage errors only. */
class CreateTransaction(private val dataSource: TransactionLocalDataSource) {
    suspend operator fun invoke(transaction: Transaction): Result<Long, DataError.Local>   // FR-022; recurring → insertRecurring
}
class MaterializeDueOccurrences(private val dataSource: TransactionLocalDataSource, private val schedule: RecurrenceSchedule) {
    suspend operator fun invoke(today: LocalDate): EmptyResult<DataError.Local> // FR-025, -025a, -025c; respects stoppedOn
}
class StopRecurringSeries(private val dataSource: TransactionLocalDataSource) {
    suspend operator fun invoke(seriesId: Long, today: LocalDate): EmptyResult<DataError.Local> // FR-038, -039
}
object RecurrenceSchedule {
    fun dueDatesBetween(anchor: LocalDate, frequency: RepeatFrequency, fromExclusive: LocalDate, toInclusive: LocalDate): List<LocalDate>
}
```

`TransactionDraft(type, name, amountMinor, note, category, repeat)` holds the unvalidated user input. It is built by the create ViewModel.

**Create flow in the ViewModel**: `validator.validate(draft)` → on success `createTransaction(tx)` → on success send `CreateTransactionEvent.Created`. Validation errors cannot normally occur, because Create is disabled until the input is valid and the input caps enforce the limits, so they are a programming-error guard. On `DataError.Local` the sheet stays open with its input intact and `CreateTransactionState.error: UiText?` is set through `DataError.toUiText()` in `:core:presentation`. Figma has no error design, so it is rendered as the DS Text field's supporting-text style (`bodySmall`, `error`) under the Create button. This fallback is noted in the plan's Complexity Tracking.

## `:feature:transactions:data`

- `RoomTransactionDataSource(dao: TransactionDao) : TransactionLocalDataSource`. It catches `SQLiteFullException` → `DISK_FULL` and other `SQLiteException` → `UNKNOWN`, and nothing leaks upward.
- Mappers: `TransactionEntity.toTransaction()`, `Transaction.toEntity()`, `RecurringSeriesEntity.toRecurringSeries()`.

## `:core:database`

- `SpendLessDatabase` (Room, v1, `exportSchema = true`) with `TransactionEntity`, `RecurringSeriesEntity` and `TransactionDao`.
- `DatabasePassphraseProvider` (AndroidKeyStore AES-GCM wrap, research R13).
- `databaseModule`: `single { Room.databaseBuilder(...).openHelperFactory(SupportOpenHelperFactory(passphrase)).build() }`.

## App wiring (`:app`)

- `SpendLessApp : Application` → `startKoin { modules(coreDataModule, databaseModule, transactionsDataModule, transactionsDomainModule, transactionsPresentationModule) }`.
- `RecurringOccurrencesTrigger` (app-scoped): `ProcessLifecycleOwner` `ON_START` plus `CurrentDateProvider.today` while started → `MaterializeDueOccurrences(today)` (research R10).
