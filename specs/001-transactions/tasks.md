---

description: "Task list for the Transactions feature (001-transactions)"
---

# Tasks: Transactions

**Propagated**: 2026-11-16 — Updated from spec.md refinement (FR-021a: amount uses the system keyboard in decimal-number mode; no custom numeric keypad)

**Input**: Design documents from `/specs/001-transactions/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: REQUIRED. AGENTS.md §4.3 makes tests mandatory for every pure-logic unit (ViewModel, validator, mapper, use case, formatter), every I/O boundary and every screen with non-trivial behaviour. The test classes come from plan.md and quickstart.md. Within each story, write the tests first and confirm they fail before implementing.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: Which user story the task belongs to (US1–US7)
- Every task names its exact file path(s)

## Path Conventions

Multi-module Android project (plan.md "Project Structure"). Package roots:

| Module | Source root | Package |
|---|---|---|
| `:core:domain` | `core/domain/src/main/kotlin/com/plcoding/spendlesssample/core/domain/` | `com.plcoding.spendlesssample.core.domain` |
| `:core:data` | `core/data/src/main/kotlin/com/plcoding/spendlesssample/core/data/` | `…core.data` |
| `:core:database` | `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/` | `…core.database` |
| `:core:presentation` | `core/presentation/src/main/kotlin/com/plcoding/spendlesssample/core/presentation/` | `…core.presentation` |
| `:core:design-system` | `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/` | `…core.designsystem` |
| `:feature:transactions:domain` | `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/` | `…transactions.domain` |
| `:feature:transactions:data` | `feature/transactions/data/src/main/kotlin/com/plcoding/spendlesssample/transactions/data/` | `…transactions.data` |
| `:feature:transactions:presentation` | `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/` | `…transactions.presentation` |
| `:app` | `app/src/main/java/com/plcoding/spendlesssample/` | `com.plcoding.spendlesssample` |

JVM tests mirror the source root under `src/test/kotlin/…`; instrumented tests under `src/androidTest/kotlin/…`. Full paths are written out in every task.

## Rules that apply to every task

- **AGENTS.md wins** over any skill. Load the skill named in a task **before** writing its code (Constitution VI). Never `Read` a skill file; invoke it with the `Skill` tool.
- **Figma** (Constitution I): file `xSFwRCFwe13SK2kAq85SLj`. Before building or changing any composable, call `get_design_context` on its node(s) first (then `get_metadata` / `get_variable_defs` if needed; `get_screenshot` only as a cross-check). If a call fails or returns empty data, **stop and report** the failing call and node. Every designed composable's KDoc carries its `Figma:` line(s) from the UI contracts (Constitution VII).
- **Previews** (Constitution VIII): every composable written or changed gets at least one `@Preview`, and one named preview **per distinct state** (empty, populated, loading, error, enabled/disabled, pressed/focused, selected, expanded/collapsed, placeholder/typing/filled, each type variant, long text, max values). This includes private sub-composables extracted from a screen or component: a screen's previews don't count for its parts. Previews use `SpendLessTheme` and fixed sample data (no ViewModel, Koin, DB or clock), light scheme, portrait only, and link the state's Figma frame when one exists. States that parameters can't reach (pressed, focused) are driven through an `interactionSource` parameter. Exempt: Root composables (their Screen carries the previews) and composables that emit no UI (`ObserveAsEvents`, icon getters). The preview lists in the tasks below are the minimum; add one for any further distinct state you create.
- **No hard-coded colours/dimensions/text styles** in feature code when a DS token exists (Constitution II). Colours only from `MaterialTheme.colorScheme` / `MaterialTheme.spendLessColors`.
- **Security**: no `Log.*` anywhere in the transactions data/presentation path; plain `Room.databaseBuilder` without the SQLCipher factory is forbidden outside tests.
- **English only**: every date/ordinal/weekday/month string is built with `Locale.ENGLISH`.
- **Portrait only**: no landscape/tablet previews, tasks or tests (Constitution IV).
- After each logical unit with a green `./gradlew build`, commit via the `git-commit` skill (no `--amend`, no `--no-verify`).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: The first multi-module split, the version catalog and the convention plugins (research R1, R2, R14).

- [X] T001 Load the `android-version-catalog` skill, fetch the current stable versions, and update `gradle/libs.versions.toml`: bump `composeBom` from `2024.09.00` to the current stable BOM (must provide Compose UI ≥ 1.9 for `Modifier.dropShadow` and material3 ≥ 1.4 for the fixed colour roles and `RippleConfiguration`, research R2); add Koin (`koin-bom`, `koin-android`, `koin-androidx-compose`, `koin-core`, `koin-test`), Room (`room-runtime`, `room-ktx`, `room-compiler`, `room-testing`) + KSP plugin, `net.zetetic:sqlcipher-android`, `androidx.sqlite:sqlite-ktx`, `navigation-compose`, `kotlinx-serialization-json` + serialization plugin, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`, `lifecycle-process`, `kotlinx-collections-immutable`, `kotlinx-coroutines-core`/`-android`/`-test`, JUnit 5 (`junit-jupiter` BOM, api, params, engine), `de.mannodermaus.android-junit5` plugin, AssertK, Turbine, `androidx.test:runner`/`rules`/`core`, and the `kotlin-jvm` / `android-library` plugin aliases. Never inline a version in a `build.gradle.kts`.
- [X] T002 Create the `:build-logic` included build: `build-logic/settings.gradle.kts` (uses the root version catalog), `build-logic/convention/build.gradle.kts` (registers the plugins below with `gradlePlugin { plugins { … } }` and depends on AGP/Kotlin/KSP/Room/compose-compiler Gradle plugins via the catalog), and add `includeBuild("build-logic")` to `pluginManagement` in `settings.gradle.kts`. Load `android-module-structure` first.
- [X] T003 [P] Create `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` (compileSdk 36, minSdk 26, targetSdk 36, JDK 11, Kotlin Android) and `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt` (same SDK/JDK config for libraries, applies `de.mannodermaus.android-junit5`, adds JUnit 5 + AssertK + Turbine + coroutines-test to `testImplementation`, sets `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`), plus a shared `build-logic/convention/src/main/kotlin/KotlinAndroid.kt` helper.
- [X] T004 [P] Create `build-logic/convention/src/main/kotlin/ComposeConventionPlugin.kt` (Kotlin compose compiler plugin, `buildFeatures.compose = true`, Compose BOM platform, ui, ui-graphics, ui-tooling-preview, material3, `debugImplementation` ui-tooling + ui-test-manifest, `androidTestImplementation` ui-test-junit4) and `build-logic/convention/src/main/kotlin/KoinConventionPlugin.kt` (Koin BOM + `koin-android` + `koin-androidx-compose`).
- [X] T005 [P] Create `build-logic/convention/src/main/kotlin/DomainModuleConventionPlugin.kt` (pure `kotlin("jvm")`, JVM toolchain 11, `kotlinx-coroutines-core`, JUnit 5 platform `tasks.test { useJUnitPlatform() }`, JUnit 5 + AssertK + Turbine + coroutines-test test deps) and `build-logic/convention/src/main/kotlin/RoomConventionPlugin.kt` (KSP + Room plugin, `room { schemaDirectory("$projectDir/schemas") }`, room-runtime, room-ktx, `ksp(room-compiler)`, `androidTestImplementation(room-testing)`).
- [X] T006 Create `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt` that applies the library + compose + koin conventions and adds `:core:domain`, `:core:presentation`, `:core:design-system`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`, `navigation-compose`, `kotlinx-serialization-json`, `kotlinx-collections-immutable` (depends on T003, T004).
- [X] T007 Register the modules in `settings.gradle.kts` (`:core:domain`, `:core:data`, `:core:database`, `:core:presentation`, `:core:design-system`, `:feature:transactions:domain`, `:feature:transactions:data`, `:feature:transactions:presentation`) and create each module's `build.gradle.kts` applying its convention plugin, with namespaces `com.plcoding.spendlesssample.core.domain|data|database|presentation|designsystem` and `com.plcoding.spendlesssample.transactions.data|presentation`: `core/domain/build.gradle.kts` (domain), `core/data/build.gradle.kts` (library + koin; deps `:core:domain`), `core/database/build.gradle.kts` (library + room + koin; deps `sqlcipher-android`, `sqlite-ktx`), `core/presentation/build.gradle.kts` (library + compose; deps `:core:domain`, lifecycle-runtime-compose), `core/design-system/build.gradle.kts` (library + compose; dep `kotlinx-collections-immutable`), `feature/transactions/domain/build.gradle.kts` (domain; dep `:core:domain`), `feature/transactions/data/build.gradle.kts` (library + koin; deps `:core:domain`, `:core:database`, `:feature:transactions:domain`), `feature/transactions/presentation/build.gradle.kts` (feature convention + serialization plugin; dep `:feature:transactions:domain`). Domain modules depend on nothing Android; features never depend on each other.
- [X] T008 Convert `app/build.gradle.kts` to the application + compose + koin convention plugins, add deps on every `:core:*` and `:feature:transactions:*` module plus `navigation-compose`, `lifecycle-process`, and the Koin Android + compose libraries; remove the now-unused JUnit 4 example tests `app/src/test/java/com/plcoding/spendlesssample/ExampleUnitTest.kt` and `app/src/androidTest/java/com/plcoding/spendlesssample/ExampleInstrumentedTest.kt`.
- [X] T009 [P] Lock the app to portrait: add `android:screenOrientation="portrait"` to `MainActivity` in `app/src/main/AndroidManifest.xml` (plan Constitution Check IV).
- [X] T010 Run `./gradlew build` with the empty modules and fix configuration until it is green (gate for Phase 2).

**Checkpoint**: All modules exist, configure, and build. No feature code yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared core types, the encrypted database, the formatting engine, the design system (theme, shadows, icons, 7 components), the feature's domain/data layer, and the app shell. Every user story depends on this phase.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### 2a. `:core:domain` (load `android-error-handling` first)

- [X] T011 [P] Create `core/domain/src/main/kotlin/com/plcoding/spendlesssample/core/domain/util/Error.kt` (`interface Error`), `core/domain/src/main/kotlin/com/plcoding/spendlesssample/core/domain/util/DataError.kt` (`sealed interface DataError : Error { enum class Local : DataError { DISK_FULL, NOT_FOUND, UNKNOWN } }`) and `core/domain/src/main/kotlin/com/plcoding/spendlesssample/core/domain/util/Result.kt` (`sealed interface Result<out D, out E : Error>` with `Success`/`Error`, `map`, `onSuccess`, `onFailure`, `asEmptyResult`, `typealias EmptyResult<E> = Result<Unit, E>`), per contracts/domain-interfaces.md.
- [X] T012 [P] Write `core/domain/src/test/kotlin/com/plcoding/spendlesssample/core/domain/format/AmountFormatPreferencesTest.kt` (load `android-unit-testing`): `Default` is `"$"`, `MINUS_PREFIX`, `'.'`, `','`; constructing with `decimalSeparator == thousandsSeparator` throws `IllegalArgumentException`; `' '` is accepted as thousands separator.
- [X] T013 [P] Create `core/domain/src/main/kotlin/com/plcoding/spendlesssample/core/domain/format/AmountFormatPreferences.kt` (`enum class NegativeStyle { MINUS_PREFIX, PARENTHESES }`; `data class AmountFormatPreferences(currencySymbol: String, negativeStyle: NegativeStyle, decimalSeparator: Char, thousandsSeparator: Char)` with `init { require(decimalSeparator != thousandsSeparator) }` and `companion object { val Default = AmountFormatPreferences("$", NegativeStyle.MINUS_PREFIX, '.', ',') }`) and `core/domain/src/main/kotlin/com/plcoding/spendlesssample/core/domain/format/AmountFormatPreferencesSource.kt` (`interface AmountFormatPreferencesSource { val preferences: Flow<AmountFormatPreferences> }` — KDoc: "The one shared place for amount formatting preferences (FR-033)"). Make T012 pass.
- [X] T014 [P] Create `core/domain/src/main/kotlin/com/plcoding/spendlesssample/core/domain/time/CurrentDateProvider.kt`: `interface CurrentDateProvider { val today: Flow<LocalDate> }` (emits now and on every date change).
- [X] T014b [P] Create `core/domain/src/testFixtures/kotlin/com/plcoding/spendlesssample/core/domain/fake/FakeCurrentDateProvider.kt` and `core/domain/src/testFixtures/kotlin/com/plcoding/spendlesssample/core/domain/fake/FakeAmountFormatPreferencesSource.kt` (each backed by a `MutableStateFlow`), apply `java-test-fixtures` in `core/domain/build.gradle.kts`, and consume them via `testImplementation(testFixtures(project(":core:domain")))` / `androidTestImplementation(testFixtures(project(":core:domain")))` from `:feature:transactions:presentation` and `:app` (used by T061, T073, T117, T129, T131).

### 2b. `:core:data` (load `kotlin-flows`, `kotlin-coroutines`, `android-di-koin` first)

- [X] T015 [P] Create `core/data/src/main/kotlin/com/plcoding/spendlesssample/core/data/format/DefaultAmountFormatPreferencesSource.kt` emitting `flowOf(AmountFormatPreferences.Default)` (FR-034).
- [X] T016 [P] Write `core/data/src/test/kotlin/com/plcoding/spendlesssample/core/data/time/SystemCurrentDateProviderTest.kt` with a fake `Clock` and a fake broadcast source: emits today immediately; emits the next date after the midnight delay elapses (`advanceTimeBy`); a broadcast-triggered re-read with an unchanged date emits nothing (`distinctUntilChanged`).
- [X] T017 Create `core/data/src/main/kotlin/com/plcoding/spendlesssample/core/data/time/SystemCurrentDateProvider.kt` (`(context, clock)`): a `callbackFlow` over `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` (register with `ContextCompat.RECEIVER_NOT_EXPORTED` semantics, `awaitClose { unregister }`) merged with a timer that delays until the next local midnight from the injected `Clock`, mapped to `LocalDate.now(clock)`, `distinctUntilChanged()` (research R10). Isolate the receiver registration behind a small internal seam so T016 can fake it. Make T016 pass.
- [X] T018 Create `core/data/src/main/kotlin/com/plcoding/spendlesssample/core/data/di/CoreDataModule.kt`: `single<Clock> { SystemDefaultZoneClock() }` (re-reads the default zone on each call; `Clock.systemDefaultZone()` would freeze the zone), `singleOf(::DefaultAmountFormatPreferencesSource) bind AmountFormatPreferencesSource::class`, `singleOf(::SystemCurrentDateProvider) bind CurrentDateProvider::class`.

### 2c. `:core:database` — SQLCipher Room (load `android-data-layer`, then `android-integration-testing` for tests)

- [X] T019 [P] Create `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/entity/TransactionEntity.kt`: table `transactions`; columns `id` INTEGER PK autoGenerate, `type` TEXT NOT NULL (`EXPENSE`/`INCOME`), `counterparty` TEXT NOT NULL, `amount_minor` INTEGER NOT NULL (> 0), `note` TEXT NULL, `category` TEXT NULL, `repeat` TEXT NOT NULL, `recorded_at_epoch_ms` INTEGER NOT NULL **indexed**, `zone_id` TEXT NOT NULL, `series_id` INTEGER NULL indexed, `occurrence_date_epoch_day` INTEGER NULL; **unique index** `(series_id, occurrence_date_epoch_day)` (data-model §3). Store enums as `String` names (the domain enums are not visible from `:core:database`).
- [X] T020 [P] Create `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/entity/RecurringSeriesEntity.kt`: table `recurring_series`; `series_id` INTEGER PK with FK → `transactions.id`, `frequency` TEXT NOT NULL, `anchor_epoch_day` INTEGER NOT NULL, `anchor_seconds_of_day` INTEGER NOT NULL, `zone_id` TEXT NOT NULL, `stopped_on_epoch_day` INTEGER NULL, `last_materialized_epoch_day` INTEGER NOT NULL.
- [X] T021 Create `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/dao/TransactionDao.kt`: `observeAll(): Flow<List<TransactionEntity>>` `ORDER BY recorded_at_epoch_ms DESC`; `insert(entity): Long`; `@Insert(onConflict = IGNORE) insertOccurrences(list)`; `insertSeries(entity)`; `runningSeries(): List<RecurringSeriesEntity>` `WHERE stopped_on_epoch_day IS NULL`; `observeRunningSeriesIds(): Flow<List<Long>>`; `markStopped(seriesId, epochDay)` updating only `WHERE stopped_on_epoch_day IS NULL`; `updateLastMaterialized(seriesId, epochDay)`; `@Transaction` wrappers `insertWithSeries(transaction, frequency, anchorEpochDay, anchorSecondsOfDay, zoneId): Long` (inserts the row, then updates its `series_id = id`, then inserts the series row with `last_materialized_epoch_day = anchor`) and `materialize(seriesId, occurrences, newLastEpochDay)` (depends on T019, T020).
- [X] T022 Create `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/SpendLessDatabase.kt` (Room v1, entities `TransactionEntity` + `RecurringSeriesEntity`, `exportSchema = true`, abstract `transactionDao()`) and `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/Converters.kt` (only if a non-primitive column remains; otherwise omit the file and the `@TypeConverters` annotation). Build once so `core/database/schemas/…/1.json` is generated, and check it in.
- [X] T023 Create `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/security/DatabasePassphraseProvider.kt` (research R13): on first call generate 32 bytes with `SecureRandom`, encrypt with an AndroidKeyStore AES-256-GCM key (alias `spendless_db_key`, `setRandomizedEncryptionRequired(true)`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`), store only IV + ciphertext in a file under `context.noBackupFilesDir`; on later calls decrypt and return the `ByteArray`. No logging of any value.
- [X] T024 Create `core/database/src/main/kotlin/com/plcoding/spendlesssample/core/database/di/DatabaseModule.kt`: load `net.zetetic` native lib (`System.loadLibrary("sqlcipher")`), `single { val passphrase = get<DatabasePassphraseProvider>().passphrase(); Room.databaseBuilder(androidContext(), SpendLessDatabase::class.java, "spendless.db").openHelperFactory(SupportOpenHelperFactory(passphrase)).build() }`, `single { get<SpendLessDatabase>().transactionDao() }`, `singleOf(::DatabasePassphraseProvider)`. Do **not** zero the passphrase: `SupportOpenHelperFactory` keeps a reference to the array (no copy) and SQLCipher reads it on every lazy open, so zeroing it would key the database with 32 zero bytes (research R13). The array stays only inside this one Koin singleton.
- [X] T025 [P] Exclude the database (`spendless.db`, `-wal`, `-shm`) and the key blob from backup in `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml` (cloud-backup and device-transfer sections) (research R13).
- [X] T026 [P] Write instrumented `core/database/src/androidTest/kotlin/com/plcoding/spendlesssample/core/database/dao/TransactionDaoTest.kt` using `Room.inMemoryDatabaseBuilder` (test only): `observeAll` is ordered by `recorded_at_epoch_ms DESC`; `insertOccurrences` with a duplicate `(series_id, occurrence_date_epoch_day)` keeps exactly one row (IGNORE); one-time rows with NULL `series_id` never collide; `insertWithSeries` sets `series_id == id` and creates the series row; `markStopped` is idempotent (second call does not change `stopped_on_epoch_day`); `runningSeries` excludes stopped series; `materialize` updates `last_materialized_epoch_day` atomically.
- [X] T027 [P] Write instrumented `core/database/src/androidTest/kotlin/com/plcoding/spendlesssample/core/database/EncryptedDatabaseTest.kt`: build the DB through `DatabaseModule`'s factory on disk, insert a row, close; the first 16 bytes of the file are **not** `"SQLite format 3\u0000"`; opening with a wrong passphrase fails; opening the on-disk file with a 32-byte all-zero passphrase fails (guards against the database being keyed with a zeroed array); `DatabasePassphraseProvider` returns the same passphrase across two instances.

### 2d. `:core:presentation` (load `android-presentation-mvi`, `android-unit-testing` first)

- [X] T028 [P] Create `core/presentation/src/main/kotlin/com/plcoding/spendlesssample/core/presentation/UiText.kt` (`sealed interface UiText { DynamicString(value); StringResource(@StringRes id, args) }` + `asString()` composable/context overloads), `core/presentation/src/main/kotlin/com/plcoding/spendlesssample/core/presentation/ObserveAsEvents.kt` (lifecycle-aware `@Composable fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit)` collecting on `Dispatchers.Main.immediate` in `repeatOnLifecycle(STARTED)`), and `core/presentation/src/main/kotlin/com/plcoding/spendlesssample/core/presentation/DataErrorUiText.kt` (`fun DataError.toUiText(): UiText` with English strings in `core/presentation/src/main/res/values/strings.xml`: disk full → "Not enough storage to save this transaction.", others → "Couldn't save the transaction. Please try again."; copy approved by the owner, spec FR-022a).
- [X] T028b [P] Write `core/presentation/src/test/kotlin/com/plcoding/spendlesssample/core/presentation/DataErrorUiTextTest.kt`: `DISK_FULL` → the storage-full string resource; `NOT_FOUND` and `UNKNOWN` → the generic string resource.
- [X] T029 [P] Write `core/presentation/src/test/kotlin/com/plcoding/spendlesssample/core/presentation/format/AmountFormatterTest.kt` covering every spec example: default `format(123450, true)` → `-$1,234.50`; `,`/`.` → `-$1.234,50`; thousands `' '` → `$12 000.00` for income 1200000; parentheses `format(4599, true)` → `($45.99)`; income `format(2000, false)` → `$20.00`; max `99_999_999_999` → `-$999,999,999.99`; `parts(null, true, Default)` → prefix `-$`, number placeholder `00.00`, suffix `""`, `isPlaceholder = true`; `parts(null, true, parentheses)` → prefix `($`, suffix `)`; income parts prefix `$`; placeholder uses the preferred decimal separator (`00,00`); `groupInput("1234567.5", Default)` → `1,234,567.5`.
- [X] T030 [P] Write `core/presentation/src/test/kotlin/com/plcoding/spendlesssample/core/presentation/format/AmountInputParserTest.kt` (parameterized): accepts digits; accepts one preferred decimal separator; rejects a second separator, the non-preferred separator, letters, `-`, `+`, spaces and the thousands separator; rejects a 3rd decimal digit; rejects a 10th whole digit; `toMinorUnits("15.99")` → 1599, `"15.9"` → 1590, `"15."` → 1500, `"0"`/`"0.00"`/`""` → `null`, `"999999999.99"` → 99_999_999_999; works with `,` as decimal separator.
- [X] T031 Create `core/presentation/src/main/kotlin/com/plcoding/spendlesssample/core/presentation/format/AmountFormatter.kt` per contracts/domain-interfaces.md (`format`, `parts` returning `AmountParts(prefix, number, suffix, isPlaceholder)`, `groupInput`); `Long` minor units only, never floating point; currency symbol always before the number. Make T029 pass.
- [X] T032 Create `core/presentation/src/main/kotlin/com/plcoding/spendlesssample/core/presentation/format/AmountInputParser.kt` (`accept(current, proposed, prefs): String?` — ≤ 9 whole digits, ≤ 2 decimals, only `prefs.decimalSeparator`; `toMinorUnits(raw, prefs): Long?` — `null` when empty or 0). Make T030 pass.

### 2e. `:core:design-system` — theme, shadows, icons (load `android-compose-components`, `figma:figma-design-to-code`)

- [X] T033 Move `app/src/main/java/com/plcoding/spendlesssample/ui/theme/{Color.kt,Type.kt,Theme.kt,SpendLessColorScheme.kt}` to `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/theme/` and `app/src/main/res/font/figtree_{regular,medium,semibold}.ttf` to `core/design-system/src/main/res/font/`; change the package to `…core.designsystem.theme`; rename `SpendLessSampleTheme` → `SpendLessTheme`; delete the old `app/…/ui/theme/` directory.
- [X] T034 Fetch Figma variables (`get_variable_defs` on `1:972`, and typography `2416:30065`) and wire the fixed roles into `lightColorScheme(...)` in `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/theme/SpendLessColorScheme.kt`: `primaryFixed #EADDFF`, `primaryFixedDim #D2BCFF`, `onPrimaryFixed #24005A`, `onPrimaryFixedVariant #5900C7`, `secondaryFixed #E5EA58`, `secondaryFixedDim #C9CE3E`, `onSecondaryFixed #1C1D00`, `onSecondaryFixedVariant #484A00` (values must match the fetched variables, research R2). Confirm `spendLessColors.success = #29AC08` and the typography table in contracts/design-system-components.md §0 match `Type.kt`; fix any drift from the fetched data.
- [X] T035 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/theme/Shadows.kt`: internal `ShadowTint = #180040`; `object SpendLessShadows { select = Shadow(radius 10dp, spread 0, offset (0,6), tint@8%); raised = (radius 12, spread 0, offset (0,8), @10%); fab = (radius 24, spread 4, offset (0,8), @16%) }` plus a `Modifier.spendLessShadow(token, shape)` extension over `Modifier.dropShadow` (research R4).
- [X] T036 Load `figma-to-android-vector`. For each DS symbol node call `get_design_context`, take the real SVG and copy each path `d` verbatim into `core/design-system/src/main/res/drawable/`: `ic_arrow_back.xml` (`406:4177`), `ic_close.xml` (`410:2796`), `ic_add.xml` (`410:2550`), `ic_check.xml` (`2415:10110`), `ic_arrow_drop_down.xml` (`407:1569`), `ic_notes.xml` (`423:4875`), `ic_trending_down.xml` (`2417:12683`), `ic_trending_up.xml` (`2417:12682`). **Keep the 24×24 icon frame as the viewport** (translate the glyph group; do not crop to glyph bounds), single colour, tinted at the call site (research R5). No Material fallbacks.
- [X] T037 Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/icon/SpendLessIcons.kt` exposing `ArrowBack`, `Close`, `Add`, `Check`, `ArrowDropDown`, `Notes`, `TrendingDown`, `TrendingUp` as `@Composable` `painterResource` getters (depends on T036), and delete the replaced, viewport-cropped duplicates from `app/src/main/res/drawable/` (`ic_arrow_back.xml`, `ic_close.xml`, `ic_add.xml`, `ic_check.xml`, `ic_arrow_drop_down.xml`, `ic_notes.xml`, `ic_trending_down.xml`, `ic_trending_up.xml`). Leave unrelated icons (fingerprint, lock, logout, …) untouched.

### 2f. `:core:design-system` — components (each: fetch every listed node with `get_design_context` first; KDoc `Figma:` lines for the set and every variant; previews per state, light only; elevation 0; state layer via `LocalRippleConfiguration` with `RippleAlpha(pressed = .12f, focused = .12f, hovered = .08f, dragged = .12f)` unless noted; every component takes `interactionSource: MutableInteractionSource? = null` for runtime and tests, while Pressed/Focused previews are forced through the internal `LocalPreviewInteraction` (research R15 D10); private sub-composables inside a component get their own per-state previews, Constitution VIII)

- [X] T038 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/component/SpendLessButton.kt` per contracts/design-system-components.md §1 (nodes `406:110`, `406:283`, `406:274`, `406:277`, `406:271`, `406:267`, `406:467`, `406:213`, `406:204`, `406:207`, `406:201`, `406:197`, `406:427`): `enum class SpendLessButtonStyle { Filled, Text }`, base M3 `Button` / `TextButton`, height 48dp, radius 16, colours/disabled/state layer/paddings/18dp icon + 8dp gap/`titleMedium` exactly as the table. Previews: Filled & Text × Enabled, Pressed, Focused, Disabled, icon left, icon right, long text.
- [X] T039 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/component/SpendLessIconButton.kt` per §2 (`406:3390` and variant nodes): `enum class SpendLessIconButtonStyle { Filled, Standard, Error, OnPrimary }`, 48dp outer box with 2dp padding → 44dp container radius 16, 24dp icon, colours per table. Previews per style × state: Filled, Standard, Error, OnPrimary × Enabled, Pressed, Focused, plus Disabled for Filled and Standard.
- [X] T040 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/component/SpendLessFloatingActionButton.kt` per §3 (`410:2338`, `410:2375`, `410:2369`, `410:2365`): M3 `FloatingActionButton` with all elevations 0, 64dp, radius 20, `secondaryContainer`/`onSecondaryContainer`, 28dp icon, `SpendLessShadows.fab`, state layer `onSecondaryContainer@12%`. Previews Enabled/Focused/Pressed.
- [X] T041 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/component/SpendLessSegmentedButton.kt` per §4 (`406:5441`, `406:5462`, `406:5583`, `406:5564`, `406:5499`, `406:5480`, `406:5527`, `406:5508`): `@Immutable data class SegmentedOption<T>(value, label, icon: Painter? = null)`, `Row(Modifier.selectableGroup())` of M3 `Surface(selected, onClick)`; track 48dp, radius 16, `primaryContainer@8%`, h-padding 4; segment `weight(1f)`, v-padding 4, radius 12; selected `surfaceContainerLowest` + `primary`; unselected transparent + `onPrimaryFixed@70%`; content padding 12/10, 18dp icon, gap 6dp; `titleMedium`; state layer `primaryContainer@8%` with `hovered/pressed = .08f`; `Role.Tab` + `selected` semantics. Previews: first selected, second selected, with icons, without icons, pressed, focused; single segment selected / unselected.
- [X] T042 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/component/SpendLessTextField.kt` per §5 (`407:1793` and the 8 variant nodes): `BasicTextField` + `decorationBox`; label above (`labelSmall`, gap 7dp); field 48dp, radius 16, `surfaceContainerLowest`, 1dp border `outline`/`primary` focused/`error`; padding start 16, end 12, vertical 4; `bodyMedium` input, cursor `primary`; placeholder `onSurfaceVariant` (70% focused); optional 20dp `ArrowDropDown`; supporting `bodySmall`; disabled at 38%. Previews: Enabled, Focused, Error, Disabled × input / placeholder.
- [X] T043 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/component/SpendLessSelect.kt` per §6 (`2415:10171`, `2415:10170`, `2415:10169`, `2415:10277`, `2415:10331`, `2415:10118`, `2415:10072`): `@Immutable data class SelectOption<T>(value, label, leadingEmoji?, leadingSymbol?, supportingText?)`, `enum class SelectListItemStyle { Icon, Text, TextIndented }`; anchor M3 `Surface(onClick)` 48dp, radius 16, `surfaceContainerLowest`, shadow `select`, start padding 4 (Icon) / 16 (Text), end 12, gap 8; 40dp leading tile radius 12 in `leadingTileColor` with emoji in `titleLarge`; field text `labelMedium` `weight(1f)`; trailing `ArrowDropDown` 24dp that does **not** flip (F7); list = `Popup` with a custom `PopupPositionProvider` placing it 4dp below the anchor at anchor width, `Surface` `surfaceContainerLowest` radius 16 shadow `select`, no vertical padding, `LazyColumn` of 48dp `Modifier.selectable` rows (start padding 4 / 16 / 52, end 12, gap 8, `labelMedium`), `Check` 24dp `primary` on the selected row, max 5 visible rows (240dp) then scroll, scrollbar 4dp × 100dp `onSurface@12%` fully rounded 2dp from the end with 14dp top inset; dismiss on outside tap and on pick via `onExpandedChange(false)`; anchor `Role.DropdownList`, rows `selected`. Previews: anchor Icon Enabled, Icon Focused (open), Text Enabled, Text Focused, with supporting text, long label; list Icon / Text / TextIndented styles, list with ≤ 5 rows, list with > 5 rows (scrollbar visible); list row selected / unselected.
- [X] T044 [P] Create `core/design-system/src/main/kotlin/com/plcoding/spendlesssample/core/designsystem/component/SpendLessTopAppBar.kt` per §7 (instances `2417:12066`, `2415:6632`; master `406:2552` unreachable, research R6): M3 `TopAppBar` transparent, `windowInsets = WindowInsets(0)`, `expandedHeight = 64.dp`; padding with nav icon h 4dp, title-only start 16 / end 4, vertical 8; `titleLarge` `onSurface`; `SpendLessTopAppBarIconButton(icon, contentDescription, onClick)` = M3 `IconButton` 48dp touch, 40dp `CircleShape` state layer, 24dp `onSurface` icon. Previews: with back nav, title + close action, title only, long title; `SpendLessTopAppBarIconButton` enabled and pressed.
- [X] T045 Run `android-preview-figma-verify` for every DS component preview (T038–T044) against its variant node(s) from contracts/design-system-components.md and fix every reported delta.

### 2g. `:feature:transactions:domain` — shared models and data-source contract

- [X] T046 [P] Create the domain models in `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/model/`: `TransactionType.kt` (`EXPENSE`, `INCOME`); `ExpenseCategory.kt` (alphabetical, fixed: `CLOTHING_AND_ACCESSORIES`, `EDUCATION`, `ENTERTAINMENT`, `FOOD_AND_GROCERIES`, `HEALTH_AND_WELLNESS`, `HOME`, `OTHER`; default `OTHER`; no emoji/strings in domain); `RepeatFrequency.kt` (`DOES_NOT_REPEAT`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`; default `DOES_NOT_REPEAT`); `Transaction.kt` with fields and rules verbatim from data-model §1 — `id: Long` ("Assigned by storage; `0` before insert"), `type`, `counterparty: String` ("Trimmed. Must not be blank after trimming. 1–50 chars"), `amountMinor: Long` ("Always positive: 1 ≤ value ≤ 99_999_999_999"), `note: String?` ("`null` when empty after trimming. ≤ 500 chars"), `category: ExpenseCategory?` ("Required (non-null) when `type == EXPENSE`; must be `null` when `type == INCOME`"), `repeat: RepeatFrequency` ("`DOES_NOT_REPEAT` for every occurrence"), `recordedAt: Instant`, `zoneId: ZoneId`, `seriesId: Long?` ("`null` for a one-time transaction. Equals its own `id` for a recurring original. The original's `id` for an occurrence"), `occurrenceDate: LocalDate?` ("Due date for occurrences only"); `RecurringSeries.kt` (`seriesId`, `frequency` "Never `DOES_NOT_REPEAT`", `anchorDate`, `anchorTime: LocalTime`, `zoneId`, `stoppedOn: LocalDate?` "Once set it is never cleared", `lastMaterializedDate: LocalDate` "Starts at `anchorDate`").
- [X] T047 Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/TransactionLocalDataSource.kt` exactly as contracts/domain-interfaces.md (`observeTransactions`, `insert`, `insertRecurring`, `runningSeries`, `materialize`, `stopSeries`, `observeRunningSeriesIds`) (depends on T046).
- [X] T048 [P] Create `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/fake/FakeTransactionLocalDataSource.kt`: in-memory `MutableStateFlow` implementation honouring the same rules as the DAO (newest-first ordering, `(seriesId, occurrenceDate)` de-duplication, stop only when running) with a switch to return `DataError.Local.DISK_FULL`. Place it in `feature/transactions/domain/src/testFixtures/kotlin/com/plcoding/spendlesssample/transactions/domain/fake/FakeTransactionLocalDataSource.kt`, apply `java-test-fixtures` in `feature/transactions/domain/build.gradle.kts`, and add `testImplementation(testFixtures(project(":feature:transactions:domain")))` and `androidTestImplementation(testFixtures(project(":feature:transactions:domain")))` to `feature/transactions/presentation/build.gradle.kts` (needed by T061, T073, T077, T120).
- [X] T049 [P] Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/ObserveTransactions.kt` (`operator fun invoke(): Flow<List<Transaction>>` delegating to the data source, newest first).
- [X] T049b [P] Write `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/ObserveTransactionsTest.kt` (fake data source, Turbine): emits the data source's list unchanged, newest first, and re-emits on insert.

### 2h. `:feature:transactions:data` (load `android-data-layer`, `android-di-koin`)

- [X] T050 [P] Write `feature/transactions/data/src/test/kotlin/com/plcoding/spendlesssample/transactions/data/TransactionMappersTest.kt`: round-trip `Transaction ↔ TransactionEntity` for expense, income (category `null`), one-time, original (`seriesId == id`) and occurrence (`occurrenceDate` → epoch day); `RecurringSeriesEntity.toRecurringSeries()` maps epoch day / seconds-of-day / zone and nullable `stoppedOn`.
- [X] T051 Create `feature/transactions/data/src/main/kotlin/com/plcoding/spendlesssample/transactions/data/TransactionMappers.kt` (`TransactionEntity.toTransaction()`, `Transaction.toEntity()`, `RecurringSeriesEntity.toRecurringSeries()`). Make T050 pass.
- [X] T052 Create `feature/transactions/data/src/main/kotlin/com/plcoding/spendlesssample/transactions/data/RoomTransactionDataSource.kt` (`(dao: TransactionDao) : TransactionLocalDataSource`): catches `SQLiteFullException` → `DataError.Local.DISK_FULL`, other `SQLiteException` → `UNKNOWN`, rethrows `CancellationException`; nothing leaks upward; no logging; `observeRunningSeriesIds` maps to `Set<Long>`.
- [X] T053 [P] Write instrumented `feature/transactions/data/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/data/RoomTransactionDataSourceTest.kt` (in-memory Room): insert + observe newest first; `insertRecurring` creates both rows atomically; `materialize` ignores duplicates and advances `lastMaterialized`; `stopSeries` removes the id from `observeRunningSeriesIds`; with a DAO wrapper that throws, `SQLiteFullException` → `DISK_FULL`, other `SQLiteException` → `UNKNOWN`, and `CancellationException` is rethrown, not mapped.
- [X] T054 Create `feature/transactions/data/src/main/kotlin/com/plcoding/spendlesssample/transactions/data/di/TransactionsDataModule.kt` with `transactionsDataModule` (`singleOf(::RoomTransactionDataSource) bind TransactionLocalDataSource::class`) and `transactionsDomainModule` (use cases and validator as `factoryOf`; the domain module itself stays Koin-free). Add use cases to it as later tasks create them.

### 2i. App shell and navigation (load `android-navigation`, `android-di-koin`)

- [X] T055 Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/navigation/TransactionsNavigation.kt`: `@Serializable data object TransactionsGraphRoute`, `@Serializable data object TransactionListRoute`, `fun NavGraphBuilder.transactionsGraph(navController: NavController, onNavigateBack: () -> Unit)` with `navigation<TransactionsGraphRoute>(startDestination = TransactionListRoute) { composable<TransactionListRoute> { /* TransactionListRoot, wired in US2 */ } }`. The create sheet is **not** a destination.
- [X] T056 Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/di/TransactionsPresentationModule.kt` with an empty `transactionsPresentationModule` (ViewModels are added by their stories).
- [X] T057 Create `app/src/main/java/com/plcoding/spendlesssample/SpendLessApp.kt` (`Application`; `startKoin { androidContext(this); modules(coreDataModule, databaseModule, transactionsDataModule, transactionsDomainModule, transactionsPresentationModule) }`), register it with `android:name=".SpendLessApp"` in `app/src/main/AndroidManifest.xml`, create `app/src/main/java/com/plcoding/spendlesssample/navigation/SpendLessNavHost.kt` (`NavHost(startDestination = TransactionsGraphRoute)`, `transactionsGraph(navController, onNavigateBack = { activity.finish() when the back stack is empty, else popBackStack })`), and update `app/src/main/java/com/plcoding/spendlesssample/MainActivity.kt` to `enableEdgeToEdge()` + `SpendLessTheme { SpendLessNavHost() }`.
- [X] T058 Run `./gradlew build` and `./gradlew connectedDebugAndroidTest` for `:core:database` and `:feature:transactions:data`; both green. Run `kotlin-mutation-testing` on `AmountFormatterTest`, `AmountInputParserTest` and `TransactionMappersTest` and add tests for every survived mutation. Commit via `git-commit`.

**Checkpoint**: Foundation ready — encrypted DB, formatting engine, design system, domain contract and app shell exist. User stories can now start.

---

## Phase 3: User Story 2 - Browse transaction history grouped by day (Priority: P1) 🎯 MVP part 1

> Ordered before US1 although both are P1: US1's acceptance ("the new expense appears at the top of the TODAY group") can only be observed on this list, while this story is testable with seeded data alone.

**Goal**: The *All Transactions* screen shows every transaction grouped by day (newest first), with TODAY / YESTERDAY / "JANUARY 9" sticky headers, expense and income rows, a back action and the add FAB.

**Independent Test**: Seed transactions dated today, yesterday and two older dates (expenses and incomes, one with a very long name, one at 999,999,999.99). Open the screen and check headers, order, row content, ellipsis, pinned header on scroll and back.

### Tests for User Story 2 ⚠️ (write first, must fail)

- [ ] T059 [P] [US2] Write `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/format/DayHeaderFormatterTest.kt`: today → `UiText.StringResource(TODAY)`, yesterday → `YESTERDAY`, older → `DynamicString("JANUARY 9")`, `"DECEMBER 28"` across a year boundary; English month names even when `Locale.setDefault(Locale.GERMAN)`.
- [ ] T060 [P] [US2] Write `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/mapper/TransactionUiMapperTest.kt`: expense → category emoji, category label, `formattedAmount` `-$45.99`; income → 💰, label "Income", `$20.00`; `note` passthrough; grouping by local date (using each row's `zoneId`) into `DayGroupUi` newest day first and newest `recordedAt` first within a day (FR-001, FR-002).
- [ ] T061 [P] [US2] Write `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListViewModelTest.kt` (JUnit 5, Turbine, `UnconfinedTestDispatcher`, fake data source, fake `CurrentDateProvider`, fake `AmountFormatPreferencesSource`): state groups reflect the data source; a newly inserted transaction appears without refresh (FR-010); emitting a new `today` relabels TODAY/YESTERDAY (midnight edge case); empty data → empty `groups`; `OnCreateClick` / `OnCreateSheetDismiss` toggle `isCreateSheetOpen` and persist it in `SavedStateHandle`.

### Implementation for User Story 2

- [ ] T062 [P] [US2] Create `feature/transactions/presentation/src/main/res/values/strings.xml` with every English string from contracts/transactions-ui.md "Strings" ("All Transactions", "Create Transaction", "Expense", "Income", "Receiver", "Sender", "Add Note", "Create", repeat labels incl. `Weekly on %1$s`, `Monthly on the %1$s`, `Yearly on %1$s`, the seven category names, "TODAY", "YESTERDAY", "Stop repeating", "Stop repeating %1$s?", "Stop", "Cancel", content descriptions "Back", "Add transaction", "Close", "Show note", "Hide note").
- [ ] T063 [P] [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/format/DayHeaderFormatter.kt` (`Locale.ENGLISH`, uppercased month + day). Make T059 pass.
- [ ] T064 [P] [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/mapper/CategoryUiMapper.kt`: `ExpenseCategory` → emoji (👔, 🎓, 💻, 🍕, ❤️, 🏠, ⚙️) + `UiText.StringResource` name; income emoji 💰 (FR-026). Make T064b pass.
- [ ] T064b [P] [US2] Write `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/mapper/CategoryUiMapperTest.kt` (parameterized): each of the seven categories → its emoji (👔 🎓 💻 🍕 ❤️ 🏠 ⚙️) and string resource; income → 💰.
- [ ] T065 [P] [US2] Create the UI models `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/model/TransactionUi.kt` (`@Immutable`: `id`, `type`, `iconEmoji`, `title`, `label: UiText`, `formattedAmount`, `note: String?`, `seriesId: Long?`, `isInRunningSeries: Boolean`) and `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/model/DayGroupUi.kt` (`dateKey: Long`, `header: UiText`, `items: ImmutableList<TransactionUi>`).
- [ ] T066 [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/mapper/TransactionUiMapper.kt` (maps + groups using `AmountFormatter`, `CategoryUiMapper`, `DayHeaderFormatter`). Make T060 pass (depends on T063–T065).
- [ ] T067 [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListState.kt` (`groups: ImmutableList<DayGroupUi>`, `expandedIds: ImmutableSet<Long>`, `isCreateSheetOpen: Boolean`, `contextMenuFor: Long?`, `stopConfirmationFor: StopConfirmationUi?`, with `StopConfirmationUi(seriesId, name)`) and `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListAction.kt` (sealed: `OnCreateClick`, `OnCreateSheetDismiss` now; US3/US6 add the rest). Load `android-presentation-mvi`.
- [ ] T068 [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListViewModel.kt`: `combine(observeTransactions(), currentDateProvider.today, preferencesSource.preferences)` → mapped groups, `stateIn(viewModelScope, WhileSubscribed(5_000), …)`; `isCreateSheetOpen` backed by `SavedStateHandle`; no `Event` type. Register with `viewModelOf(::TransactionListViewModel)` in `TransactionsPresentationModule.kt` and `ObserveTransactions` in `transactionsDomainModule`. Load `kotlin-flows`. Make T061 pass.
- [ ] T069 [P] [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/DayHeader.kt` (fetch `2417:12045` first): full width, `background` colour, feature-private style `MaterialTheme.typography.bodyXSmall.copy(fontWeight = FontWeight.Medium)` (Medium 12/16) in `onSurface@70%`, uppercase; KDoc `Figma: …node-id=2417-12045` and the F4 note. Previews: TODAY, YESTERDAY, older date ("JANUARY 9").
- [ ] T070 [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/TransactionRow.kt` — collapsed state only, no badge yet (fetch `423:4852`, `2414:5064`, `2414:5093`, `2417:12050` first; values from research R9): `Row` gap 8 vertically centred; 44dp tile radius 12 — expense `primaryFixed` with emoji in `titleLarge`, income 💰 on `secondaryFixed@40%` over `surfaceContainerLowest` (F1); name `labelMedium` `onSurface` one line ellipsis with `weight(1f)`; label `bodyXSmall` `onSurface@70%`; amount `titleLarge`, `widthIn(min = 100.dp)`, end-aligned, never truncated (F3) — expense `onSurface`, income `spendLessColors.success`. Signature from contracts/transactions-ui.md (`item`, `isExpanded`, `onNoteBadgeClick`, `onLongPress: (() -> Unit)?`, `modifier`). Previews: expense collapsed, income collapsed, one expense row per category (all seven emoji tiles), long name, `-$999,999,999.99`.
- [ ] T071 [US2] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScreen.kt` with `TransactionListRoot(onNavigateBack, viewModel = koinViewModel())` and `TransactionListScreen(state, onAction, onNavigateBack, createSheetContent: @Composable () -> Unit)` (fetch `2417:12042`, `2417:12068`, `2417:12043`, `2417:12066`, `2417:12067`, `2417:12088` first; load `android-compose-architecture`): `Scaffold(containerColor = background)`; `SpendLessTopAppBar("All Transactions")` with `SpendLessTopAppBarIconButton(ArrowBack, "Back")` and **no** download action (FR-009, F8); `LazyColumn` h-padding 16, `spacedBy(12.dp)`, `stickyHeader` per group (FR-008), items `key = id` and `contentType`, `padding(bottom = 8.dp)` on each group's last row; `SpendLessFloatingActionButton(Add, "Add transaction")` 24dp from end and 16dp above the navigation-bar inset dispatching `OnCreateClick`; empty list shows no groups. Collect with `collectAsStateWithLifecycle`. Previews: populated (today/yesterday/older, expenses and incomes), empty list, long name, max amount, pinned-header scroll state (a `rememberLazyListState(firstVisibleItemIndex = …)` that starts scrolled, `2417:12068`).
- [ ] T072 [US2] Wire `TransactionListRoot` into the `composable<TransactionListRoute>` block in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/navigation/TransactionsNavigation.kt` (pass `onNavigateBack`).
- [ ] T073 [US2] Write instrumented `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScreenTest.kt` (load `android-ui-testing`; `createAndroidComposeRule<ComponentActivity>()`, real ViewModel, fake data source + fake clock/date provider, mind `StandardTestDispatcher` + `advanceUntilIdle`): headers "TODAY", "YESTERDAY", "JANUARY 9" in order; expense row shows `-$45.99`, income row shows "Income" and `$20.00`; the header of the top group remains displayed after scrolling; back invokes `onNavigateBack`; no download icon exists.
- [ ] T074 [US2] Verify UI: `android-preview-figma-verify` for `TransactionListScreen` previews vs `2417:12042` and `TransactionRow` collapsed previews vs `2414:5142`; then `./gradlew installDebug` and `android-ui-verify` + `android-ui-screenshot-verify` on the live screen vs `2417:12042` and `2417:12068` (export icon expected absent). Resolve all deltas. Run `kotlin-mutation-testing` on T059–T061. `./gradlew build` green → `git-commit`.

**Checkpoint**: The list screen works with seeded data and matches Figma.

---

## Phase 4: User Story 1 - Record a one-time expense (Priority: P1) 🎯 MVP part 2

**Goal**: The FAB opens the Create Transaction sheet (Expense / Other / Does not repeat, amount focused, keyboard up). The user enters a receiver and an amount (optionally a category and a note) and taps Create; the expense appears at the top of TODAY.

**Independent Test**: From an empty history, open the sheet, enter "Netflix" and 15.99, choose *Entertainment*, create. One "Netflix / Entertainment / −$15.99" row appears under TODAY. Close (✕) discards input.

### Tests for User Story 1 ⚠️ (write first, must fail)

- [ ] T075 [P] [US1] Write `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/TransactionValidatorTest.kt` (fixed `Clock`): valid expense → `Success` with trimmed name, `recordedAt = now`, `zoneId` from clock, `seriesId = null`; blank / whitespace-only name → `NAME_BLANK`; 51 chars after trim → `NAME_TOO_LONG`; amount 0 → `AMOUNT_NOT_POSITIVE`; 99_999_999_999 accepted, 100_000_000_000 → `AMOUNT_TOO_LARGE`; note of 501 chars → `NOTE_TOO_LONG`; whitespace-only note → `note = null`; expense with `null` category → `CATEGORY_MISSING_FOR_EXPENSE`.
- [ ] T076 [P] [US1] Write `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/CreateTransactionTest.kt` (fake data source): a `DOES_NOT_REPEAT` transaction is inserted via `insert` and returns its id; a `DISK_FULL` from the data source is returned unchanged. (The recurring path is added in US5.)
- [ ] T077 [P] [US1] Write `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionViewModelTest.kt`: defaults `EXPENSE` / `""` / `OTHER` / `DOES_NOT_REPEAT` / `isCreateEnabled = false`; `isCreateEnabled` only when name not blank **and** amount > 0 (FR-015); name input capped at 50 chars (51st char ignored); note capped at 500; amount keystrokes filtered by `AmountInputParser` (letters, `-`, third decimal, 10th whole digit rejected); `amountParts` placeholder `-$` / `00.00`; `OnCategoryPick` sets category and closes the select; `OnCreateClick` with valid input persists one transaction and emits `CreateTransactionEvent.Created`, then the form is reset; storage failure keeps the input and sets `error`; `OnCloseClick` resets the form without saving; state fields survive a new ViewModel built from the same `SavedStateHandle` (process death); `OnAddNoteClick` sets `isNoteEditing`, `OnNoteFocusLost` with empty note clears it.

### Implementation for User Story 1

- [ ] T078 [P] [US1] Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/model/TransactionDraft.kt` (`type`, `name`, `amountMinor`, `note`, `category`, `repeat`) and `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/model/CreateTransactionError.kt` (`enum class TransactionValidationError : Error { NAME_BLANK, NAME_TOO_LONG, AMOUNT_NOT_POSITIVE, AMOUNT_TOO_LARGE, NOTE_TOO_LONG, CATEGORY_MISSING_FOR_EXPENSE, CATEGORY_NOT_ALLOWED_FOR_INCOME }`).
- [ ] T079 [US1] Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/TransactionValidator.kt` (`(clock: Clock)`, `validate(draft): Result<Transaction, TransactionValidationError>`, stamps `recordedAt`/`zoneId` = now; name "Trimmed. Must not be blank after trimming. 1–50 chars"; amount "1 ≤ value ≤ 99_999_999_999"; note "`null` when empty after trimming. ≤ 500 chars"; category "Required (non-null) when `type == EXPENSE`"). Make T075 pass.
- [ ] T080 [US1] Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/CreateTransaction.kt` (`suspend operator fun invoke(transaction): Result<Long, DataError.Local>`; one-time → `insert`). Register `TransactionValidator` and `CreateTransaction` in `transactionsDomainModule` in `feature/transactions/data/src/main/kotlin/com/plcoding/spendlesssample/transactions/data/di/TransactionsDataModule.kt`. Make T076 pass.
- [ ] T081 [P] [US1] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionState.kt` (fields and defaults verbatim from data-model §4: `type = EXPENSE`, `name = ""` "Input capped at 50", `amountInput = ""` "Raw digits plus at most one preferred separator, at most 2 decimals and 9 whole digits", `amountParts: AmountPartsUi(prefix, suffix, isPlaceholder, signTone)`, `note = ""` "Capped at 500", `isNoteEditing = false`, `category = OTHER`, `repeat = DOES_NOT_REPEAT`, `repeatOptions: ImmutableList<RepeatOptionUi>` (empty until US5), `openSelect: OpenSelect?` (`CATEGORY`, `REPEAT`), `isCreateEnabled = false`, `error: UiText? = null`), `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionAction.kt` (all 13 actions from data-model §4) and `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionEvent.kt` (`Created`).
- [ ] T082 [US1] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionViewModel.kt`: inputs capped (name 50, note 500), amount filtered by `AmountInputParser.accept` with the current preferences, `amountParts` from `AmountFormatter.parts`, `isCreateEnabled` derived; create flow `validator.validate(draft)` → `createTransaction(tx)` → `Channel` event `Created`; `DataError.Local` → `error = it.toUiText()` and input kept (FR-022a); `type`, `name`, `amountInput`, `note`, `category`, `repeat` saved in `SavedStateHandle`; form reset on close / dismiss / after `Created`, never on open. Register `viewModelOf(::CreateTransactionViewModel)`. Make T077 pass.
- [ ] T083 [P] [US1] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/component/CounterpartyInput.kt` (fetch `2415:6636`, `2415:7321`): `BasicTextField` `titleMedium` `onSurface` centred, `px 8dp`, placeholder same style at `onSurface@60%`, `cursorBrush = SolidColor(primary)`, single line. Previews: placeholder "Receiver", placeholder "Sender", typing, 50-character name.
- [ ] T084 [P] [US1] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/component/AmountInput.kt` (fetch `2415:6638`, `2415:11064`): centred `Row` gap 4, `py 10dp`, `px 8dp`, `displayMedium`; prefix/suffix in `error` (expense) or `spendLessColors.success` (income); digits `onSurface`; placeholder `00.00` (preferred separator) at `onSurface@38%`; `BasicTextField` holding only the raw number with a `VisualTransformation` that inserts thousands separators via `AmountFormatter.groupInput` with correct `OffsetMapping`; `KeyboardOptions(keyboardType = KeyboardType.Decimal)` so the phone's own system keyboard opens in decimal-number mode (FR-021a); **no custom numeric keypad**, and `AmountInputParser` still filters anything the keyboard lets through (FR-013); accepts a `FocusRequester`. Previews: empty expense (`-$ 00.00`), empty income (`$ 00.00`), filled expense, filled income, `999,999,999.99` with grouping, `,` as decimal separator. (Parentheses previews are added in T133.)
- [ ] T085 [P] [US1] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/component/NoteInput.kt` (fetch `2415:6641`): idle row gap 4 centred with `SpendLessIcons.Add` 20dp + "Add Note" `bodyMedium` `onSurface@60%`; editing state = centred multi-line `BasicTextField` in the same tokens (placeholder "Add Note"@60%, caret `primary`, `px 8dp`), grows over as many lines as needed. KDoc: "Editing state has no Figma frame; user decision 2026-11-13 (research R8)". Previews: idle, editing empty, editing multi-line.
- [ ] T086 [US1] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionScreen.kt` with `CreateTransactionRoot(onClose, onCreated, viewModel = koinViewModel())` (uses `ObserveAsEvents` for `Created`) and `CreateTransactionScreen(state, onAction, onCloseClick)` (fetch `2415:6629`, `2415:6631`, `2415:6632`, `2417:12817`, `2415:10578`, `2415:9786`, `2415:6659`, `406:271` first; layout per research R7): `SpendLessTopAppBar("Create Transaction")` with `Close` action ("Close"); Column `px 16dp`: `SpendLessSegmentedButton` (Expense `TrendingDown` / Income `TrendingUp`, Expense selected) → 34dp → inputs block 300dp centred gap 12 (`CounterpartyInput`, `AmountInput`, `NoteInput`) → 32dp → filters block **fixed height 104dp, gap 8, content bottom-aligned** containing the category `SpendLessSelect` (Icon style, tile `primaryFixed`, seven `SelectOption`s with emoji, current ticked) and a reserved 48dp repeat slot (filled in US5) → 12dp → full-width `SpendLessButton(Filled, "Create", enabled = isCreateEnabled)`; `error` rendered under Create in `bodySmall` `error` (FR-022a); `LaunchedEffect(Unit) { amountFocusRequester.requestFocus(); keyboardController?.show() }` (FR-021); opening a Select clears focus. KDoc `Figma:` lines for every state node in contracts/transactions-ui.md. Previews: empty `-$10` (`2415:6533`), typing name (`2415:7218`), category list open (`2415:9674`), filled with Create enabled (`2443:9436`), note editing (no frame, R8), storage error shown under Create. Any private sub-composable extracted here (for example the filters block) gets its own per-state previews.
- [ ] T087 [US1] Host the sheet in `TransactionListRoot` in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScreen.kt`: when `isCreateSheetOpen`, show M3 `ModalBottomSheet` (`skipPartiallyExpanded = true`, `dragHandle = null`, `containerColor = surfaceContainerLow`, top corners 28dp, scrim `onBackground@32%`, `contentWindowInsets = { WindowInsets(0) }`, no `imePadding()`, height = sheet window minus status-bar inset minus 8dp) containing `CreateTransactionRoot` whose `CreateTransactionViewModel` is scoped to the list's nav entry; ✕, scrim tap, swipe and `Created` all dispatch `OnCreateSheetDismiss` and reset the form (research R7).
- [ ] T088 [US1] Write instrumented E2E `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionFlowTest.kt` with a Robot (`…/create/CreateTransactionRobot.kt`) per `android-ui-testing`: tap +, sheet opens with Expense / Other / Does not repeat and amount focused, and the soft keyboard is visible (`waitUntil` on `ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true`, FR-021); Create disabled until both name and amount > 0; enter "Netflix", 15.99, choose Entertainment, Create → sheet closes and the row appears within 1 s (`composeRule.waitUntil(timeoutMillis = 1_000) { onAllNodesWithText("Netflix").fetchSemanticsNodes().isNotEmpty() }`, SC-004) with "Netflix", "Entertainment", "-$15.99" under TODAY; add a note → the new row has the note badge's content description (enabled after US3; mark as asserted in T097); ✕ closes without creating a row.
- [ ] T089 [US1] Verify UI: `android-preview-figma-verify` on each `CreateTransactionScreen` preview vs its node; on device `android-ui-verify` + `android-ui-screenshot-verify` vs `2415:6533` and `2415:9674` (Create disabled on the empty form is an expected deviation, F6; note-typing state has no frame; the keyboard area is the system keyboard, FR-021a, so its look is not compared against the Figma Keyboard instance). Resolve deltas. `kotlin-mutation-testing` on T075–T077. `./gradlew build` green → `git-commit`.

**Checkpoint**: MVP complete — expenses can be recorded and browsed.

---

## Phase 5: User Story 3 - Read a transaction's note in the list (Priority: P2)

**Goal**: Tapping a row's note badge expands it in place into a raised card showing the full note; tapping again collapses it. Rows expand independently and start collapsed on every visit.

**Independent Test**: Seed an expense and an income with multi-line notes and one row without a note. Tap each badge → expands with full note; tap again → collapses; the row without a note has no badge.

### Tests for User Story 3 ⚠️

- [ ] T090 [P] [US3] Extend `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListViewModelTest.kt`: `OnNoteBadgeClick(id)` adds then removes `id` in `expandedIds`; two rows can be expanded at once; a newly inserted transaction is not expanded while existing expansions are kept; a fresh ViewModel from the same `SavedStateHandle` starts with empty `expandedIds` (FR-031).
- [ ] T091 [P] [US3] Extend `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScreenTest.kt`: clicking "Show note" reveals the full note text and the label changes to "Hide note"; clicking again hides it; a row without a note has no "Show note" node; the expansion survives scrolling the row out of view and back.

### Implementation for User Story 3

- [ ] T092 [US3] Add `OnNoteBadgeClick(id)` to `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListAction.kt` and handle it in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListViewModel.kt` (`expandedIds` toggled, **not** saved to `SavedStateHandle`). Make T090 pass.
- [ ] T093 [US3] Add the note badge to `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/TransactionRow.kt` (fetch `423:4883`, `2414:5142` first): shown only when `note != null`; 20dp, radius 6, `surfaceContainerLowest`, shadow `raised`, at offset (26, 26) inside the 44dp tile; `SpendLessIcons.Notes` 14dp tinted — expense collapsed `primaryFixedDim` / expanded `primaryContainer`, income collapsed `secondaryFixedDim` / expanded `secondary`; a transparent 48dp clickable box centred on the badge (no layout change), `Role.Button`, `contentDescription` "Show note" / "Hide note" (FR-036). Only the badge toggles; tapping elsewhere on the row does nothing.
- [ ] T094 [US3] Add the expanded state to `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/TransactionRow.kt` (fetch `2414:5065`, `2414:5106`, `2414:5066`, `2414:5076`): card `surfaceContainerLowest`, radius 16, shadow `raised`, padding start/top/end 4 and bottom 10, gap 6, inner row `pr 4dp` (F9), drawn 4dp outside the row on start, end and top so icon/name/label don't move; note `bodySmall` `onSurface`, `pl 52dp`, `pr 12dp`, unlimited lines, never ellipsized; amount stays `titleLarge` token (F2). Previews: expense with note collapsed, expense expanded, income with note collapsed, income expanded, 500-char note expanded. If the badge is extracted into its own composable, it gets previews for expense subdued, expense active, income subdued, income active.
- [ ] T095 [US3] Pass `isExpanded = item.id in state.expandedIds` and `onNoteBadgeClick = { onAction(OnNoteBadgeClick(item.id)) }` in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScreen.kt`; add the "one expanded expense and one expanded income" preview. Make T091 pass.
- [ ] T096 [US3] Verify: `android-preview-figma-verify` for the new `TransactionRow` previews vs `2414:5142` (expect the 4dp amount shift, F9). Mutation-test T090. `./gradlew build` green → `git-commit`.
- [ ] T097 [US3] Complete the pending note-badge assertion in `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionFlowTest.kt` (US1-5: a created expense with a note shows "Show note").

**Checkpoint**: Notes are readable in the list.

---

## Phase 6: User Story 4 - Record income (Priority: P2)

**Goal**: Switching the sheet to *Income* changes the placeholder to *Sender*, shows the amount as positive/incoming, hides the category (Repeat and Create stay put), and records an income shown as "Income" in the list.

**Independent Test**: Open the sheet, switch to Income, enter "Rick" and 20.00, create → a green "Rick / Income / $20.00" row appears under TODAY. Switching back to Expense restores the category and keeps all other input.

### Tests for User Story 4 ⚠️

- [ ] T098 [P] [US4] Extend `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/TransactionValidatorTest.kt`: a valid income draft with `category = null` succeeds and the resulting `Transaction.category` is `null`; an income draft with a category → `CATEGORY_NOT_ALLOWED_FOR_INCOME`.
- [ ] T099 [P] [US4] Extend `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionViewModelTest.kt`: `OnTypeSelect(INCOME)` → `amountParts.prefix == "$"`, `signTone == INCOME`; name, amount, note, repeat kept across Expense ↔ Income (FR-024); the chosen expense category (e.g. `HOME`) is kept and restored when switching back; creating an income builds the draft with `category = null`.
- [ ] T100 [P] [US4] Extend `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionFlowTest.kt`: switch to Income → "Sender" placeholder shown, category select absent, Create's position unchanged; create "Rick" 20 → row "Rick", "Income", "$20.00" under TODAY.

### Implementation for User Story 4

- [ ] T101 [US4] Implement income in `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/TransactionValidator.kt` ("must be `null` when `type == INCOME`"). Make T098 pass.
- [ ] T102 [US4] Handle `OnTypeSelect` in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionViewModel.kt` (recompute `amountParts`/`signTone`; keep `category` in state while income; build the draft with `category = null` for income). Make T099 pass.
- [ ] T103 [US4] Update `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionScreen.kt` (fetch `2415:8542`, `2415:8638` first): segmented button dispatches `OnTypeSelect`; `CounterpartyInput` placeholder "Sender" for income; `AmountInput` sign in `success`; category Select hidden for income while the fixed-height, bottom-aligned filters block keeps the repeat slot at y = 306 and Create at y = 366 (research R7). Add the "Empty – Income" and "Filled – Income" previews. Make T100 pass.
- [ ] T104 [US4] Verify: `android-preview-figma-verify` for the income preview vs `2415:8638`; on device `android-ui-verify` + `android-ui-screenshot-verify` vs `2415:8542`. Mutation-test T098–T099. `./gradlew build` green → `git-commit`.

**Checkpoint**: Income and expenses can both be recorded.

---

## Phase 7: User Story 5 - Set up a recurring expense or income (Priority: P2)

**Goal**: The repeat field offers *Does not repeat*, *Daily*, *Weekly on [weekday]*, *Monthly on the [ordinal]*, *Yearly on [Mon] [ordinal]*; recurring transactions add one-time linked occurrences on each due date — caught up on app start and live at midnight — exactly once.

**Independent Test**: Create "Gym" 45.99 *Monthly on the [today]*, advance the device date past the next due date and reopen: exactly one matching Gym row on the due day, no future rows. With the app open across midnight a due occurrence appears under TODAY without refresh.

### Tests for User Story 5 ⚠️

- [ ] T105 [P] [US5] Write `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/RecurrenceScheduleTest.kt`: monthly on Jan 31 → Feb 28 (Feb 29 in a leap year) → Mar 31 → Apr 30 (computed from the anchor, not the previous date); yearly on Feb 29 → Feb 28 in non-leap years, Feb 29 in leap years; weekly = anchor + n weeks; daily = anchor + n days; `fromExclusive` excluded, `toInclusive` included; empty when `toInclusive <= fromExclusive`; `DOES_NOT_REPEAT` → empty.
- [ ] T106 [P] [US5] Write `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/MaterializeDueOccurrencesTest.kt` (fake data source): one occurrence per missed due date with the original's type, name, amount, category, note, `repeat = DOES_NOT_REPEAT`, `seriesId = original id`, `occurrenceDate = due date`, `recordedAt = min(due date + original local time in the original's zone, materialisation instant)` (FR-002: an occurrence added at midnight for a 23:00 original is stamped 00:00, a caught-up past one keeps 23:00); running twice for the same `today` adds nothing (exactly once); never creates a future-dated occurrence; upper bound is `min(today, stoppedOn)`; `lastMaterialized` advanced to the bound; storage error is returned.
- [ ] T107 [P] [US5] Extend `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/CreateTransactionTest.kt`: a transaction with `repeat != DOES_NOT_REPEAT` is persisted through `insertRecurring(original, frequency)` and the stored original has `seriesId == id`.
- [ ] T108 [P] [US5] Write `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/format/RepeatLabelFormatterTest.kt`: for Monday Jan 13 2025 → "Does not repeat", "Daily", "Weekly on Monday", "Monthly on the 13th", "Yearly on Jan 13th"; ordinals 1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st, 22nd, 23rd, 31st; short forms "Does not repeat", "Daily", "Weekly", "Monthly", "Yearly"; English under `Locale.GERMAN` default.
- [ ] T109 [P] [US5] Extend `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionViewModelTest.kt`: `repeatOptions` built from today's date (fake date provider); `OnRepeatPick(WEEKLY)` sets `repeat`, closes the select and the field shows "Weekly"; the draft carries the chosen `repeat`.

### Implementation for User Story 5

- [ ] T110 [US5] Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/RecurrenceSchedule.kt` (`object RecurrenceSchedule { fun dueDatesBetween(anchor, frequency, fromExclusive, toInclusive): List<LocalDate> }`, anchor + n periods clamped with `min(anchorDay, lengthOfMonth)`, research R10). Make T105 pass.
- [ ] T111 [US5] Add the recurring path to `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/CreateTransaction.kt` (`repeat != DOES_NOT_REPEAT` → `insertRecurring`). Make T107 pass.
- [ ] T112 [US5] Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/MaterializeDueOccurrences.kt` (`(dataSource, schedule, clock)`, `suspend operator fun invoke(today): EmptyResult<DataError.Local>`; stamp each occurrence `recordedAt = min(due date + anchorTime in the series zone, clock.instant())` (FR-002); for each running series compute due dates in `(lastMaterializedDate, min(today, stoppedOn ?: today)]`, build occurrences, call `materialize` atomically). Register in `transactionsDomainModule`. Make T106 pass.
- [ ] T113 [P] [US5] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/format/RepeatLabelFormatter.kt` (long labels from a `LocalDate`, short labels, English ordinals, `Locale.ENGLISH`) and `RepeatOptionUi(frequency, label)` in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionState.kt`. Make T108 pass.
- [ ] T114 [US5] Build `repeatOptions` from `CurrentDateProvider.today` and handle `OnRepeatSelectClick` / `OnRepeatPick` in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionViewModel.kt`. Make T109 pass.
- [ ] T115 [US5] Add the repeat `SpendLessSelect` to the filters block in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionScreen.kt` (fetch `2415:11244`, `2415:11280`, `2415:7969` first): emoji 🔄, tile `tertiaryContainer`, `SelectListItemStyle.TextIndented`, field shows the short label, current option ticked. Add previews "Filled, repeat list open" (`2415:7873`) and repeat field showing each short label ("Daily", "Weekly", "Monthly", "Yearly").
- [ ] T116 [US5] Create `app/src/main/java/com/plcoding/spendlesssample/RecurringOccurrencesTrigger.kt` (load `kotlin-coroutines`, `kotlin-flows`): app-scoped `CoroutineScope(SupervisorJob() + Dispatchers.Default)`; while `ProcessLifecycleOwner` is at least `STARTED`, collect `CurrentDateProvider.today` and call `MaterializeDueOccurrences(today)` for each emission (covers catch-up on `ON_START` and live at midnight, FR-025a / FR-025c); no logging of results. Start it from `app/src/main/java/com/plcoding/spendlesssample/SpendLessApp.kt` after `startKoin`.
- [ ] T117 [US5] Write instrumented `app/src/androidTest/java/com/plcoding/spendlesssample/RecurringOccurrencesTriggerTest.kt` (in-memory Room data source, fake `CurrentDateProvider` driven by a `MutableStateFlow`, `TestLifecycleOwner`): a daily series created 3 days ago gets exactly 3 occurrences on start; emitting the next date while started adds exactly one more; re-emitting the same date adds none.
- [ ] T118 [US5] Verify: `android-preview-figma-verify` for the repeat-open preview vs `2415:7969`; on device `android-ui-verify` + `android-ui-screenshot-verify` vs `2415:7873`; manual quickstart rows US5 and US5-live. Mutation-test T105–T109. `./gradlew build` + `connectedDebugAndroidTest` green → `git-commit`.

**Checkpoint**: Recurring series are created and materialised exactly once.

---

## Phase 8: User Story 6 - Stop a recurring expense or income (Priority: P2)

**Goal**: Long-pressing any row of a running series opens a one-item "Stop repeating" menu; confirming the dialog "Stop repeating {name}?" stops the series so no later due date produces an occurrence. Existing rows stay unchanged.

**Independent Test**: Create a daily "Gym", advance two days, long-press a Gym row → Stop repeating → Stop; advance again: no new Gym rows, existing ones remain. Long-press on a one-time row does nothing.

### Tests for User Story 6 ⚠️

- [ ] T119 [P] [US6] Write `feature/transactions/domain/src/test/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/StopRecurringSeriesTest.kt`: sets `stoppedOn = today`; a second stop leaves the first `stoppedOn` (never cleared, FR-039); no transaction is changed or removed; afterwards `MaterializeDueOccurrences` for a later date adds nothing for that series (including catch-up).
- [ ] T120 [P] [US6] Extend `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListViewModelTest.kt`: `isInRunningSeries` true for the original and occurrences of a running series, false for one-time rows and stopped series; `OnRowLongPress(id)` sets `contextMenuFor` only when `isInRunningSeries`; `OnContextMenuDismiss` clears it; `OnStopRepeatingClick` closes the menu and sets `stopConfirmationFor(seriesId, name)`; `OnStopConfirm` calls `StopRecurringSeries(seriesId, today)` and clears the dialog; `OnStopCancel` changes nothing.
- [ ] T121 [P] [US6] Extend `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScreenTest.kt`: long-press on a running-series row shows "Stop repeating"; tapping outside closes it; choosing it shows "Stop repeating Gym?" with "Stop" and "Cancel"; Cancel / dismiss keeps the series running; Stop stops it; long-press on a one-time row shows no menu.

### Implementation for User Story 6

- [ ] T122 [US6] Create `feature/transactions/domain/src/main/kotlin/com/plcoding/spendlesssample/transactions/domain/usecase/StopRecurringSeries.kt` (`suspend operator fun invoke(seriesId, today): EmptyResult<DataError.Local>` → `stopSeries`). Register in `transactionsDomainModule`. Make T119 pass.
- [ ] T123 [US6] Add `OnRowLongPress(id)`, `OnContextMenuDismiss`, `OnStopRepeatingClick`, `OnStopConfirm`, `OnStopCancel` to `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListAction.kt`; in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListViewModel.kt` combine `observeRunningSeriesIds()` into the mapping to set `isInRunningSeries`, and handle the actions. Make T120 pass.
- [ ] T124 [P] [US6] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/StopRepeatingMenu.kt` (fetch `2415:10118` for tokens): `Popup` anchored start-aligned 4dp below the row, `surfaceContainerLowest`, radius 16, shadow `select`; one 48dp row "Stop repeating" `labelMedium` `onSurface`, start padding 16; dismiss on outside tap (FR-040). KDoc: "No Figma node; composed from DS Select-list `2415:10118` per user decision 2026-11-13 (research R11)". Previews: menu item enabled, menu item pressed.
- [ ] T125 [P] [US6] Create `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/StopRepeatingDialog.kt`: M3 `AlertDialog`, `containerColor = surfaceContainerLowest`, radius 28; title "Stop repeating %1$s?" `titleLarge` `onSurface`; confirm `SpendLessButton(Text, "Stop")`, dismiss `SpendLessButton(Text, "Cancel")`; outside dismiss = Cancel. KDoc: "No Figma node; composed from DS Button `406:110` per user decision 2026-11-13 (research R11)". Previews: short name ("Gym"), 50-character name.
- [ ] T126 [US6] Add long-press to `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/TransactionRow.kt`: when `onLongPress != null`, `pointerInput { detectTapGestures(onLongPress = { haptic(HapticFeedbackType.LongPress); onLongPress() }) }` plus semantics `customActions` "Stop repeating"; rows look identical whether or not they repeat (FR-037).
- [ ] T127 [US6] Wire the menu and dialog in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScreen.kt`: pass `onLongPress` only for `isInRunningSeries` rows; show `StopRepeatingMenu` anchored to the row whose id equals `contextMenuFor`; show `StopRepeatingDialog` when `stopConfirmationFor != null`. Make T121 pass.
- [ ] T128 [US6] Verify: manual quickstart row US6 on device; mutation-test T119–T120. `./gradlew build` + `connectedDebugAndroidTest` green → `git-commit`.

**Checkpoint**: Recurring series can be stopped.

---

## Phase 9: User Story 7 - Amounts follow the user's formatting preferences (Priority: P3)

**Goal**: Every amount in the list and the sheet follows the four preferences from the one shared `AmountFormatPreferencesSource`; swapping that single binding changes every amount with no other code change (FR-035, SC-010).

**Independent Test**: Run with defaults, then with a fake source per combination (parentheses; `,` decimal + `.` thousands; space thousands) and check the sheet and the rows.

### Tests for User Story 7 ⚠️

- [ ] T129 [P] [US7] Extend `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListViewModelTest.kt` (parameterized over a fake `AmountFormatPreferencesSource`): defaults → `-$1,234.50`; `,`/`.` → `-$1.234,50`; space thousands → income `$12 000.00`; parentheses → `($45.99)`; changing the emitted preferences re-formats existing rows.
- [ ] T130 [P] [US7] Extend `feature/transactions/presentation/src/test/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionViewModelTest.kt`: parentheses → empty expense `amountParts` prefix `($`, suffix `)`; `,` decimal → placeholder `00,00`, `,` accepted and `.` rejected while typing.
- [ ] T131 [US7] Write instrumented `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/AmountFormatPreferencesSwapTest.kt`: load Koin modules with the `AmountFormatPreferencesSource` binding replaced by a fake for each combination; assert the sheet reads `($ 00,00 )` for parentheses + `,` and rows read `($1.234,50)` and `$12 000.00`. No production source edits.

### Implementation for User Story 7

- [ ] T132 [US7] Audit that every amount path (`TransactionUiMapper`, `CreateTransactionViewModel`, `AmountInput` `VisualTransformation`, placeholder) reads preferences only from the injected `AmountFormatPreferencesSource` — no literal `'$'`, `'.'`, `','`, `'-'` or `"("` outside `AmountFormatPreferences.Default` in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/` and `core/presentation/src/main/kotlin/com/plcoding/spendlesssample/core/presentation/format/`; fix any found. Make T129–T131 pass.
- [ ] T133 [P] [US7] Add parentheses-format previews: "Empty ($10)" in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/create/CreateTransactionScreen.kt` (KDoc/preview node `2415:10959`, sheet `2415:11055`) and a `($45.99)` row preview in `feature/transactions/presentation/src/main/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/component/TransactionRow.kt`.
- [ ] T134 [US7] Verify: `android-preview-figma-verify` for the parentheses sheet preview vs `2415:11055`. Mutation-test T129–T130. `./gradlew build` + `connectedDebugAndroidTest` green → `git-commit`.

**Checkpoint**: All user stories are independently functional.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Security, performance, docs and the final gates across all stories.

- [ ] T135 [P] Update the Figma fileKey in `AGENTS.md` §6 from `ftCIcpiLxrMhckuzDmYQWL` to the constitution's `xSFwRCFwe13SK2kAq85SLj` (and node `401-935`) — plan "Housekeeping"; constitution follow-up TODO.
- [ ] T136 [P] Security audit: confirm no `Log.`/`println` in `core/database/src/main/`, `feature/transactions/*/src/main/` and `app/src/main/` (grep), no `Room.databaseBuilder` without `openHelperFactory` outside `src/androidTest`, and no `INTERNET` permission in `app/src/main/AndroidManifest.xml`; fix any hit.
- [ ] T137 Performance check (SC-005): add a debug-only seeding preview/test in `feature/transactions/presentation/src/androidTest/kotlin/com/plcoding/spendlesssample/transactions/presentation/list/TransactionListScrollTest.kt` with 1,000 transactions over 12 months, scroll to the end with `performScrollToIndex`, and confirm keys/`contentType` are set and the UI models are `@Immutable`; fix any unstable parameter reported by the Compose compiler metrics. Then on a release build fling the seeded list end to end and confirm `adb shell dumpsys gfxinfo com.plcoding.spendlesssample` reports ≤ 5% janky frames (SC-005); record the result in `specs/001-transactions/quickstart.md`.
- [ ] T138 Run quickstart.md §5 security spot-checks on the device (`run-as … head -c 16 databases/spendless.db | xxd` is not `SQLite format 3`; `adb logcat -d | grep -iE "netflix|15\.99|gym"` has no matches) and §3 manual scenarios US1–US7; record any failure as a new task.
- [ ] T139 Preview audit (Constitution VIII): list every `@Composable` function under `core/design-system/src/main/`, `feature/transactions/presentation/src/main/` and `app/src/main/` (`grep -rn "@Composable" -A2`), and for each non-exempt one confirm at least one `@Preview` exists and every distinct state it can render has its own named preview, including private sub-composables. Render them with `android-preview-figma-verify` where a Figma frame exists. Add any missing previews in the composable's file.
- [ ] T140 Final gate: `./gradlew build` and `./gradlew connectedDebugAndroidTest` both green across all modules (AGENTS §5); then commit the remaining work via `git-commit`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup; **blocks all user stories**.
- **US2 (Phase 3)**: depends on Foundational.
- **US1 (Phase 4)**: depends on Foundational; its acceptance is observed on the US2 list and T087 hosts the sheet in the US2 screen.
- **US3 (Phase 5)**: depends on US2 (extends `TransactionRow` and the list ViewModel).
- **US4 (Phase 6)**: depends on US1 (extends the sheet). Independent of US3.
- **US5 (Phase 7)**: depends on US1 (repeat field in the sheet). Domain tasks T105–T112 only need Foundational and can start earlier.
- **US6 (Phase 8)**: depends on US2 (list) and US5 (running series must exist).
- **US7 (Phase 9)**: depends on US2 and US1 (both amount surfaces exist).
- **Previews**: every composable task carries its own per-state previews; T139 is a final audit, not where previews get written.
- **Polish (Phase 10)**: depends on every story you intend to ship.

### Story completion graph

```text
Setup → Foundational ─┬─► US2 ─┬─► US3
                      │        ├─────────────► US6 ◄─┐
                      │        └─► US7 ◄─┐           │
                      └─► US1 ───────────┴─► US4     │
                              └─► US5 ───────────────┘
```

### Within each story

- Tests first; confirm they fail.
- Domain models → use cases → ViewModel → components → screen → wiring → UI test → Figma verification → mutation test → build → commit.

### Parallel Opportunities

- Phase 1: T003, T004, T005 in parallel; T009 any time.
- Phase 2: 2a (T011–T014), 2b (T015–T016), 2c entities/tests (T019, T020, T025–T027), 2d (T028–T030), 2e (T035) and all seven DS components (T038–T044) are independent files and can run in parallel once their sub-section's prerequisites exist. T046/T048/T049 and T050 can run alongside the core work.
- After Foundational: US2 and US1 domain/ViewModel work (T075–T082) can proceed in parallel; US5 domain (T105–T112) can also start early.
- Within each story, all `[P]` test tasks run together.

---

## Parallel Example: User Story 2

```bash
# Tests together:
Task: "DayHeaderFormatterTest in feature/transactions/presentation/src/test/.../format/DayHeaderFormatterTest.kt"
Task: "TransactionUiMapperTest in feature/transactions/presentation/src/test/.../mapper/TransactionUiMapperTest.kt"
Task: "TransactionListViewModelTest in feature/transactions/presentation/src/test/.../list/TransactionListViewModelTest.kt"

# Then independent building blocks together:
Task: "strings.xml in feature/transactions/presentation/src/main/res/values/strings.xml"
Task: "DayHeaderFormatter in .../presentation/format/DayHeaderFormatter.kt"
Task: "CategoryUiMapper in .../presentation/mapper/CategoryUiMapper.kt"
Task: "TransactionUi + DayGroupUi in .../presentation/list/model/"
Task: "DayHeader in .../presentation/list/component/DayHeader.kt"
```

## Parallel Example: User Story 1

```bash
Task: "TransactionValidatorTest in feature/transactions/domain/src/test/.../TransactionValidatorTest.kt"
Task: "CreateTransactionTest in feature/transactions/domain/src/test/.../usecase/CreateTransactionTest.kt"
Task: "CreateTransactionViewModelTest in feature/transactions/presentation/src/test/.../create/CreateTransactionViewModelTest.kt"

Task: "CounterpartyInput in .../presentation/create/component/CounterpartyInput.kt"
Task: "AmountInput in .../presentation/create/component/AmountInput.kt"
Task: "NoteInput in .../presentation/create/component/NoteInput.kt"
```

## Parallel Example: Foundational design system

```bash
Task: "SpendLessButton in core/design-system/.../component/SpendLessButton.kt"
Task: "SpendLessIconButton in core/design-system/.../component/SpendLessIconButton.kt"
Task: "SpendLessFloatingActionButton in core/design-system/.../component/SpendLessFloatingActionButton.kt"
Task: "SpendLessSegmentedButton in core/design-system/.../component/SpendLessSegmentedButton.kt"
Task: "SpendLessTextField in core/design-system/.../component/SpendLessTextField.kt"
Task: "SpendLessSelect in core/design-system/.../component/SpendLessSelect.kt"
Task: "SpendLessTopAppBar in core/design-system/.../component/SpendLessTopAppBar.kt"
```

---

## Implementation Strategy

### MVP First (US2 + US1, both P1)

1. Phase 1 Setup → Phase 2 Foundational (build green, DB encrypted, DS verified against Figma).
2. Phase 3 US2: list with seeded data → **validate** against `2417:12042` / `2417:12068`.
3. Phase 4 US1: create expense → **validate** the quickstart US1 scenario end to end.
4. **STOP**: this is the shippable MVP (record and browse expenses).

### Incremental Delivery

1. MVP (US2 + US1).
2. US3 notes → demo.
3. US4 income → demo.
4. US5 recurring → demo (date-advance scenario).
5. US6 stop repeating → demo.
6. US7 formatting swap → demo.
7. Polish and final gate.

Each increment ends with a green `./gradlew build`, its Figma verification and a `git-commit`.

---

## Notes

- `[P]` = different files, no dependency on an incomplete task.
- `[USn]` maps the task to its spec user story for traceability.
- Expected Figma deviations (do not "fix"): export icon absent (F8), Create disabled on the empty form (F6), amount column min-width (F3), note-typing state (R8), stop menu/dialog (R11), 4dp expanded-amount shift (F9), income tile 40% (F1).
- If any Figma MCP call fails, stop UI work and report the failing call and node (Constitution I, fail closed).
