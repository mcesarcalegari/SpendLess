# Research: Transactions (Phase 0)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-11-13

Every decision below follows the same format: Decision, Rationale, Alternatives considered. Every visual value quoted here came from a structured Figma MCP response (`get_design_context`, `get_metadata`, `get_variable_defs`) or from an SVG asset exported through that response. None came from a screenshot (Constitution I).

---

## R0. Figma fetch log (Constitution I)

File `xSFwRCFwe13SK2kAq85SLj`, MCP session `bd748865-ecd7-41e7-abf0-1cab90681c5d`, fetched 2026-11-13.

| Node | What | Call(s) | Result |
|---|---|---|---|
| `0:1` | 🧩 Components page | `get_metadata` | OK: page tree, component-set ids |
| `406:110` | Button (Filled/Text × state × icon) | `get_design_context` | OK |
| `406:3390` | Icon button (Filled/Standard/Error/On Primary × state) | `get_design_context` | OK |
| `410:2338` | FAB (Enabled/Focused/Pressed) | `get_design_context` | OK |
| `406:5441`, `406:5462` | Segmented button + building blocks | `get_design_context` | OK |
| `407:1793` | Text field (Enabled/Focused/Error/Disabled × input/placeholder) | `get_design_context` | OK |
| `2415:10171` | Select (Icon/Text × Enabled/Focused) | `get_design_context` | OK |
| `2415:10118` | Select-list | `get_design_context` | OK |
| `2415:10072` | list-item | `get_design_context` | OK |
| `2415:4433` | Transactions screen section | `get_metadata` | OK: frame ids |
| `2417:12042` | All Transactions | `get_design_context` | OK |
| `2417:12068` / `2417:12069` | All Transactions – Scroll | `get_design_context`, `get_metadata` | OK |
| `2414:5142` | Expense + Income row states | `get_design_context`, `get_variable_defs` | OK |
| `2415:6629` | Create – Empty – `-$10` (bottom sheet) | `get_design_context`, `get_variable_defs` | OK |
| `2415:11055` | Create – Empty – `($10)` | `get_design_context` | OK |
| `2415:7314` | Create – Typing Name | `get_design_context` | OK |
| `2415:9770` | Create – Category Select Active | `get_design_context` | OK |
| `2415:7969` | Create – Filled, repeat list open | `get_design_context` | OK |
| `2443:9532` | Create – Filled | `get_design_context`, `get_metadata` | OK |
| `2415:8638` | Create – Filled – Income | `get_design_context` | OK |
| `406:2552`, `406:2443` | M3-kit Top app bar / Icon button masters | `get_design_context` | **Rejected**: "invalid node selection" (they sit on no visible page). See R6. |
| — | `search_design_system` "dialog", "menu", style "shadow" | `search_design_system` | Empty: no dialog, menu or shadow style exists in the file. See R11. |

Assets exported through `get_design_context` were downloaded only to inspect their fill colours. They are not shipped. Icons are regenerated from the DS symbol nodes (R5).

---

## R1. Module layout for the first multi-module split

**Decision**: Split the single `:app` module into the canonical layout from the `android-module-structure` skill:

```
:build-logic, :app,
:core:domain, :core:data, :core:database, :core:presentation, :core:design-system,
:feature:transactions:domain, :feature:transactions:data, :feature:transactions:presentation
```

The current theme (`app/.../ui/theme/*`) and fonts move to `:core:design-system`.

**Rationale**: AGENTS.md §2 makes this layout mandatory from day one. The user asked for a design-system module. A single SQLCipher Room database is shared infrastructure, and the skill names `:core:database` as its home.

**Alternatives considered**:
- Keep `:app` single-module until a second feature exists. Rejected: violates AGENTS.md §2.
- Put DB classes in `:feature:transactions:data`. Rejected: the encrypted DB and its key handling are app-wide security infrastructure (Constitution, Additional Constraints).

The namespace stays `com.plcoding.spendlesssample.*` (for example `…core.designsystem`). Renaming the application id is out of scope.

## R2. Compose BOM upgrade (needed for shadows and colour roles)

**Decision**: Bump `composeBom` from `2024.09.00` to the current stable BOM. The implementer fetches the exact version through the `android-version-catalog` skill. The new BOM must provide:

- **Compose UI ≥ 1.9**, for `Modifier.dropShadow(shape, Shadow(radius, spread, color, offset))`.
- **material3 ≥ 1.4**, for `ColorScheme.primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `secondaryFixed`, `secondaryFixedDim` and the rest of the fixed roles, and `RippleConfiguration`.

**Rationale**: Figma uses three *tinted, offset, spread* shadows (R4). `Modifier.shadow(elevation)` cannot reproduce colour `#180040`, a y-offset or a spread. Figma also uses fixed colour roles: `primaryFixed` (category tiles), `primaryFixedDim` and `secondaryFixedDim` (note badge), `onPrimaryFixed` (segment labels) and `secondaryFixed` (income tile). The existing `Color.kt` already defines these values, but `SpendLessColorScheme.kt` never wires them, because material3 1.3 (BOM 2024.09) has no such roles.

**Alternatives considered**:
- Custom `drawBehind` shadow with `BlurMaskFilter`. Kept as a fallback only if the fetched BOM somehow lacks `dropShadow`.
- Put the fixed roles in `SpendLessExtraColors`. This is the fallback if material3 < 1.4 is forced.

## R3. "Material 3 as the base" vs. "do not improvise with standard M3"

**Decision**: Build every design-system component on an M3 composable, as `android-compose-components` requires: `Button`, `TextButton`, `IconButton`/`FilledIconButton`, `FloatingActionButton`, `Surface(selected=…)`, `TopAppBar`, `ModalBottomSheet`, `AlertDialog`. Then **override every visual parameter from Figma**: shape, container and content colours (with alpha), height, content padding, typography, elevation (always 0, with Figma shadows applied through `dropShadow`) and state layers.

Where M3's own structure cannot express the Figma component, the base is a lower-level M3 or foundation primitive, and the reason is recorded:

| Component | M3 default rejected because | Base used |
|---|---|---|
| Segmented button | M3 `SegmentedButton` is an outlined pill with a check icon and divider lines; Figma is a tinted track with a white raised segment | `Row(Modifier.selectableGroup())` + M3 `Surface(selected, onClick, shape = 12dp)` |
| Text field | `OutlinedTextField` has a floating label inside the border and is 56dp tall; Figma has the label *above*, 48dp, radius 16 | `BasicTextField` + `decorationBox` |
| Select / dropdown | `DropdownMenu` and `ExposedDropdownMenu` force 8dp list padding and M3 menu shape and item metrics; Figma has a 4dp gap, the anchor's width, 48dp rows, radius 16, max 5 rows, a custom scrollbar | `Popup` with a custom `PopupPositionProvider` + `Surface` + `LazyColumn` rows using `Modifier.selectable` |

**State layers**: Figma state layers are flat overlays: Filled button `onPrimary@12%`, Text button `primary@12%`, Standard icon button `onSurfaceVariant@12%`, Error icon button `error@12%`, FAB `onSecondaryContainer@12%`, segment `primaryContainer@8%`. Each DS component provides `LocalRippleConfiguration` with that colour and `RippleAlpha(pressed = .12f, focused = .12f, hovered = .08f, dragged = .12f)` (`.08f` for the segment). The default M3 ripple tint and alpha are never used.

**Rationale**: This satisfies the skill (real 48dp targets, accessibility semantics) and the user (no stock M3 look).

## R4. Shadow tokens

**Decision**: Add a `SpendLessShadows` token object to `:core:design-system`. Figma has no effect styles for these (`search_design_system` returned empty), so the values come from the node data:

| Token | Offset (x,y) | Blur | Spread | Colour | Used by (node) |
|---|---|---|---|---|---|
| `select` | 0, 6 | 10 | 0 | `#180040` @ 8% | Select anchor and Select-list (`2415:10171`, `2415:10118`) |
| `raised` | 0, 8 | 12 | 0 | `#180040` @ 10% | Note badge (`423:4883`), expanded row card (`2414:5065`) |
| `fab` | 0, 8 | 24 | 4 | `#180040` @ 16% | FAB (`410:2338`) |

Figma `drop-shadow` blur maps to `Shadow(radius = blur)`. `#180040` is not a scheme variable, so it becomes a DS-internal `ShadowTint` constant, used only inside these tokens.

## R5. Icons (Constitution III)

**Decision**: Regenerate every icon this feature uses from its **DS symbol node** on page `0:1` with the `figma-to-android-vector` skill. Keep the **24×24 icon frame as the viewport**: translate the glyph group, and do not crop to the glyph bounds. Export single-colour and tint at the call site. Place the icons in `:core:design-system` `res/drawable` and expose them through a `SpendLessIcons` object. Delete the old copies in `:app`.

| Icon | DS node | Used at (size → tint) |
|---|---|---|
| `icon/arrow_back` | `406:4177` | List top bar back (24 → onSurface) |
| `icon/close` | `410:2796` | Sheet close (24 → onSurface) |
| `icon/add` | `410:2550` | FAB (28 → onSecondaryContainer), Add Note (20 → onSurface @60%), Button icon slot (18) |
| `icon/check` | `2415:10110` | Select-list selected item (24 → primary) |
| `icon/arrow_drop_down` | `407:1569` | Select (24 → onSurface), Text field trailing (20) |
| `icon/Notes` | `423:4875` | Note badge (14 → see R9) |
| `icon/Trending down` | `2417:12683` | Expense segment (18 → primary selected / onPrimaryFixed@70% unselected) |
| `icon/Trending up` | `2417:12682` | Income segment (18 → same rule) |

Material 3 fallbacks: **none needed**.

**Finding (defect in existing assets)**: the drawables in `app/src/main/res/drawable` crop the viewport to the glyph and stretch it to 24dp. For example, `ic_add` has a 14×14 viewport at 24dp and `ic_arrow_drop_down` a 10×5 viewport at 24×12dp. They render larger than the Figma icon frames, so they must not be reused. Icons that this feature does not use (fingerprint, lock, logout and others) are left untouched. Regenerating them is a follow-up for the features that need them.

## R6. Top app bar (kit component off-page)

**Decision**: Build `SpendLessTopAppBar` from the **instance** data, because the master `406:2552` cannot be fetched:

- List: `2417:12066` (Small, leading back icon, `px 4dp`, `py 8dp`).
- Sheet: `2415:6632` (title only plus trailing close, `pl 16dp`, `pr 4dp`).

Spec: height 64; title `titleLarge` (Figtree SemiBold 20/26) in `onSurface`. Navigation and action buttons are 48dp boxes with a **40dp circular** state layer (`rounded-100`, `p-8`) and a 24dp icon in **onSurface `#1B1B1C`**. Base: M3 `TopAppBar` with transparent container, `windowInsets = 0`, `expandedHeight = 64.dp`, and nav and action icons from M3 `IconButton` with `CircleShape`.

**Why not the DS Icon button (`406:3390`, Standard)**: Its container is radius 16 at 44dp and its icon is `onSurfaceVariant #44474B`. The screen instances clearly use the kit icon button (circle, `#1B1B1C`). Following the frames is the faithful choice. The DS Icon button is still implemented in full (FR from the user's request) and is available to other features.

**Placement**: `:core:design-system`. It is a kit component used by two screens and on every Figma page with a title bar. The Figma link for its KDoc is the list instance `2417:12066` (Constitution VII; the master is unreachable).

## R7. Create sheet: host, layout, and the income gap

**Decisions**:
- **Host**: An M3 `ModalBottomSheet` opened from the list screen. Its visibility is `TransactionListState.isCreateSheetOpen`, which **is persisted in the list ViewModel's `SavedStateHandle`**. Content is `CreateTransactionRoot` with its own `CreateTransactionViewModel`. That ViewModel is scoped to the list's nav entry and **resets its form when the sheet closes** (✕, scrim or swipe dismiss, or after `Created`), never on open. Stale input therefore never reappears (edge case "closing with unsaved input"), and after process death the sheet reopens with the form restored from `SavedStateHandle`.
- **IME**: In Figma the keyboard *overlays* the sheet's lower part (`2415:6533`: Create ends at about y = 546 and the Keyboard starts at y = 554). The sheet keeps its fixed height. Set `contentWindowInsets = { WindowInsets(0) }` and apply no `imePadding()` to the sheet content, so the IME does not reflow the layout. Verify against `2415:6533`.
- **Sheet tokens** (`2415:6631`):
  - `containerColor = surfaceContainerLow`, top corners 28dp.
  - `dragHandle = null`, `skipPartiallyExpanded = true`.
  - Scrim `onBackground @ 32%`.
  - The sheet is 832dp tall in an 892dp frame, so its top edge sits 8dp below the 52dp status bar. Implement this as `fillMaxHeight()` of the sheet window, minus the status-bar inset, minus 8dp.
  - Internal: column `py 4dp`, `gap 4dp`.
- **Content layout**: Translate the frame's absolute offsets into a `Column` with `px 16dp`:
  1. Segmented button (y = 0).
  2. Spacer 34dp, then the Inputs block (300dp wide, centred, `gap 12dp`; starts at y = 82).
  3. Spacer 32dp, then the Filters block (`gap 8dp`; y = 250, **fixed height 104dp, content bottom-aligned**).
  4. Spacer 12dp, then the Create button (y = 366).
- **Income frame (`2415:8638`)**: The Filters block stays at y = 250 with the repeat Select at y = 306. Only the category slot disappears, and Create stays at y = 366. The fixed-height, bottom-aligned Filters block reproduces this exactly and keeps the layout from jumping when the user toggles type.
- **Keyboard**: On open, the amount field takes focus in a `LaunchedEffect(Unit)` with `FocusRequester` (FR-021). Opening a Select clears focus, so the keyboard hides (frame `2415:9770` hides the Keyboard instance).

**Alternatives considered**:
- A navigation-compose `dialog` destination wrapping a sheet. Rejected: two windows, and inset quirks.
- A ViewModel key per opening. Rejected: it leaks ViewModels in the store.

## R8. Inputs inside the sheet (not DS text fields)

The sheet inputs are **feature-specific**: centred and borderless. They are not the DS Text field `407:1793`.

| Input | Node | Style |
|---|---|---|
| Name (Receiver/Sender) | `2415:6636`, typing `2415:7321` | `BasicTextField`, `titleMedium` (SemiBold 16/24), onSurface, centred, `px 8dp`. Placeholder in the same style at onSurface **60%**. Caret `primary` (Figma caret 20dp tall, `#5A00C8`; Compose `cursorBrush = SolidColor(primary)`, and Compose's fixed 2dp caret width is accepted). Max 50 chars. |
| Amount | `2415:6638`, `2415:11064` | Row with `gap 4dp`, centred, `py 10dp`, `px 8dp`. `displayMedium` (SemiBold 36/44). Sign prefix (`-$`, `($`, or `$`) and the closing `)` in `error` (expense) or `success` (income). Digits in onSurface. Placeholder `00.00` (with the preferred decimal separator) in onSurface **38%**. `BasicTextField` holds only the raw number string; a `VisualTransformation` inserts thousands separators with correct offset mapping. `KeyboardType.Decimal`. |
| Add Note (idle) | `2415:6641` | Row with `gap 4dp`, centred: `icon/add` 20dp + "Add Note" in `bodyMedium` (Regular 16/24), onSurface **60%**. |
| Note (typing) | — *no frame* | **User-approved deviation (2026-11-13)**: tapping "Add Note" swaps the row for a centred, multi-line `BasicTextField` in the **same tokens as the idle row** (`bodyMedium`, onSurface, placeholder "Add Note" @60%, caret primary, `px 8dp`). It grows over as many lines as needed and holds at most 500 chars. If it loses focus while empty, it returns to the idle row. |

**Decimal separator caveat**: Some IMEs do not show `,` on `KeyboardType.Decimal`. The input filter accepts **only** the preferred separator (spec edge case). Because the default is `.`, this does not block the feature. It is listed as a risk for the future preferences feature.

## R9. Transaction row and note expansion

From `2417:12042` (row master `423:4852`) and `2414:5142`:

- **Collapsed row**:
  - `Row`, `gap 8dp`, vertically centred. Groups are separated by `gap 12dp` between items, with an extra `pb 8dp` on the last row of each day.
  - Icon tile: 44dp, radius 12.
    - Expense: `primaryFixed` with the category emoji in `titleLarge`.
    - Income: 💰 on `secondaryFixed @ 40%` over `surfaceContainerLowest`. See inconsistency F1.
  - Name: `labelMedium` (Medium 16/24), onSurface, one line, ellipsis.
  - Label: `bodyXSmall` (Regular 12/16), onSurface **70%**.
  - Amount: `titleLarge`. Expense in onSurface; income in **`success`**.
- **Amount column**: Figma fixes it at `w = 100`. **Decision**: `widthIn(min = 100.dp)`, end-aligned, never truncated. The name column takes `weight(1f)` and yields first (FR-007, edge case "very large amounts"). See F3.
- **Note badge** (`423:4883`):
  - 20dp, radius 6, `surfaceContainerLowest`, shadow `raised`.
  - Positioned at offset (26, 26) inside the 44dp tile, so it overhangs by 2dp.
  - `icon/Notes` at 14dp. Tint by state, all colours exported from the Figma SVGs and mapped to tokens:

| Type | Collapsed (subdued) | Expanded (active) |
|---|---|---|
| Expense | `#D2BCFF` → `primaryFixedDim` | `#8138FF` → `primaryContainer` |
| Income | `#C9CE3E` → `secondaryFixedDim` | `#5F6200` → `secondary` |

  - The touch target is extended to 48dp **without changing layout**: a transparent 48dp clickable box centred on the badge inside the tile's `Box`. It has `Role.Button` and `contentDescription` "Show note" or "Hide note" (FR-036).
- **Expanded card** (`2414:5065`, `2414:5106`):
  - `surfaceContainerLowest`, radius 16, shadow `raised`.
  - Padding start/top/end 4, bottom 10; `gap 6dp`. Inner row `pr 4dp`.
  - The card is 388dp against a 380dp row. It is drawn 4dp outside the row on the start, end **and top** (negative offsets equal to the card padding), so the icon, name and label do not move (US3-1). The inner row's `pr 4dp` moves the amount 4dp inward relative to the collapsed row. This comes from Figma and is kept as-is (F9).
  - Note: `bodySmall` (Regular 14/20), onSurface, `pl 52dp`, `pr 12dp`, wraps freely (FR-028).
- **Expansion state**: `expandedIds: ImmutableSet<Long>` lives in `TransactionListState` (android-compose-components: no local domain state). It is **not** saved to `SavedStateHandle`, so every row starts collapsed when the screen is opened again (FR-031).
- **Day header** (`2417:12045`): Figtree **Medium** 12/16, onSurface 70%, uppercase, background `background`, full width. No text style token matches it, because `bodyXSmall` is Regular. **Decision**: a feature-private `TextStyle` derived from `MaterialTheme.typography.bodyXSmall.copy(fontWeight = Medium)`, with a KDoc link to `2417:12045` (Constitution II permits this when no token exists). Sticky through `LazyColumn.stickyHeader` (FR-008, `2417:12088`).
- **FAB placement**: 24dp from the end and 16dp above the navigation-bar inset (frame: `bottom 40` including the 24dp nav bar).

## R10. Recurrence engine (no WorkManager)

**Decision**: Materialise occurrences **in-process**, not with WorkManager:

1. **Catch-up on app foreground**: an app-scoped coroutine in `:app` reacts to `ProcessLifecycleOwner` `ON_START` and calls the `MaterializeDueOccurrences` use case (FR-025a).
2. **Live at the date change**: a `CurrentDateProvider` in `:core:data` emits today's `LocalDate`. It is a `callbackFlow` over `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED` and `ACTION_TIMEZONE_CHANGED`, merged with a timer that delays until the next local midnight from an injected `Clock`, then `distinctUntilChanged()`. Each emission while the app is started runs `MaterializeDueOccurrences` (FR-025c). The list ViewModel combines the same flow to relabel TODAY and YESTERDAY at midnight (edge case).
3. **Exactly once**: occurrences have a unique index on `(series_id, occurrence_date)`. Inserts use `OnConflictStrategy.IGNORE` inside one Room transaction per run (FR-025a "none twice", SC-008).
4. **Due-date maths** is a pure domain function, `RecurrenceSchedule.dueDatesBetween(anchor, frequency, fromExclusive, toInclusive)`. Each due date is computed **from the anchor**, never from the previous occurrence:
   - Monthly: day `min(anchorDay, lengthOfMonth)`, so Jan 31 → Feb 28 → Mar 31 → Apr 30.
   - Yearly on Feb 29: Feb 28 in non-leap years.
   - Daily and weekly: anchor plus n days or n weeks.
   - The upper bound is `min(today, stoppedOn)`.
   - The occurrence time of day is the original's local time on the due date, capped at the materialisation instant when that is earlier (FR-002, amended 2026-11-16). `MaterializeDueOccurrences` therefore takes the injected `Clock`.

**Rationale**: The spec only needs occurrences "the next time the app is opened" or "at the date change while open". Nothing must run while the app is closed. WorkManager would add a dependency and a process with no user benefit (`android-background` decision tree: no need to survive process death, so not this skill).

**Alternatives considered**:
- WorkManager periodic daily work. Rejected: unnecessary, and it is inexact at midnight.
- `AlarmManager` exact alarms. Rejected: needs a permission and runs while closed, which the spec does not require.

## R11. Stop-repeating menu and dialog (no Figma design)

`search_design_system` found no dialog or menu component. **User decision (2026-11-13): compose them from DS parts**, recorded as a deviation in the plan's Complexity Tracking:

- **Menu**:
  - A `Popup` anchored at the long-pressed row (start-aligned, 4dp below the row).
  - Same surface tokens as Select-list `2415:10118`: `surfaceContainerLowest`, radius 16, shadow `select`.
  - One 48dp row "Stop repeating" in `labelMedium` onSurface, `pl 16dp` (Select "Text" list-item padding).
  - Dismisses on outside tap (FR-040).
  - Long-press triggers haptic feedback and works only for rows with `isInRunningSeries` (FR-037).
- **Dialog**:
  - M3 `AlertDialog` with `containerColor = surfaceContainerLowest`, shape radius 28 (the Figma sheet corner token).
  - Title "Stop repeating {name}?" in `titleLarge` onSurface.
  - Buttons: DS `SpendLessButton` Text style "Cancel" and "Stop".
  - Dismiss-on-outside counts as Cancel (FR-038).

Both are `internal` to `:feature:transactions:presentation`, and each KDoc states "no Figma node — composed from DS Select-list `2415:10118` / Button `406:110` per user decision 2026-11-13". When Figma gains real components, they get promoted to the DS and relinked.

## R12. Amount format preferences (FR-032 – FR-035)

**Decision**:
- `:core:domain` holds `AmountFormatPreferences(currencySymbol: String, negativeStyle: NegativeStyle, decimalSeparator: Char, thousandsSeparator: Char)` with an `init` check that `decimalSeparator != thousandsSeparator`.
- `:core:domain` also holds the interface `AmountFormatPreferencesSource { val preferences: Flow<AmountFormatPreferences> }`.
- `:core:data` holds `DefaultAmountFormatPreferencesSource`, which emits `flowOf(AmountFormatPreferences.Default)` (`$`, `MINUS_PREFIX`, `.`, `,`). This is the **one shared place**; the future preferences feature replaces this binding in Koin and nothing else (FR-035).
- `:core:presentation` holds the pure formatter `AmountFormatter` (`format(minor, isNegative)` and `parts(...)`, which returns the prefix, number and suffix the sheet needs) and the pure parser `AmountInputParser`. Both are unit-tested against every spec example (`-$1,234.50`, `-$1.234,50`, `$12 000.00`, `($45.99)`).

**Money type**: amounts are stored and processed as `Long` minor units (cents). The maximum is `99_999_999_999` (999,999,999.99). Floating point is never used.

## R13. Security (Constitution, Additional Constraints; AGENTS §4.5)

**Decision**:
- `:core:database` opens Room with `SupportOpenHelperFactory(passphrase)` from `net.zetetic:sqlcipher-android`.
- Passphrase: 32 bytes from `SecureRandom`, generated once and encrypted with an AndroidKeyStore AES-256-GCM key (alias `spendless_db_key`, `setRandomizedEncryptionRequired(true)`). Only the IV and ciphertext are stored, in `noBackupFilesDir`.
- The passphrase array is **not** zeroed. `SupportOpenHelperFactory` and `SupportHelper` keep a reference to the array (no copy) and SQLCipher reads it on each lazy open, so zeroing it after `build()` would silently key the database with 32 zero bytes (verified against the sqlcipher-android source, 2026-11-16). It lives only inside the factory singleton and is never logged or persisted unwrapped.
- `backup_rules.xml` and `data_extraction_rules.xml` exclude the database and the key blob.
- `Room.databaseBuilder` without the factory exists nowhere in production code. In-memory Room without encryption is used **only** in tests.
- No `Log.*` anywhere in the transaction data or presentation path.

## R14. Testing stack

**Decision**:
- **JVM tests**: JUnit 5, AssertK, Turbine and `kotlinx-coroutines-test` (`android-unit-testing`). Android library modules get JUnit 5 through the `de.mannodermaus.android-junit5` plugin, wired in a `:build-logic` convention plugin.
- **Instrumented tests**:
  - In-memory Room DAO tests (`android-integration-testing`).
  - One test that proves the on-disk DB is not readable without the passphrase.
  - Compose UI tests with `createAndroidComposeRule<ComponentActivity>()` and a fake `Clock` (`android-ui-testing`).
- **Figma checks**: `android-preview-figma-verify` per preview, and `android-ui-verify` plus `android-ui-screenshot-verify` per screen.

---

## Figma inconsistencies found (recorded, resolved without inventing)

| # | Where | Conflict | Decision |
|---|---|---|---|
| F1 | Income icon tile | `secondaryFixed` @ **40%** in the list frames (`2417:12050`) and @ **30%** in the row-states frame (`2414:5093`) | Use **40%**. The list frames are the authoritative list design (spec Assumptions). |
| F2 | Expanded amount | `titleLarge` with line height 28 and tracking 0.1 in `2414:5076`; 26/0 everywhere else | Use the `titleLarge` token (26/0) and treat the expanded-state value as drift. |
| F3 | Amount column | Fixed `w = 100`, which cannot hold `-$999,999,999.99` at 20sp, while the spec says the amount never truncates | `widthIn(min = 100.dp)`, with the name taking `weight(1f)`. |
| F4 | Day header | Figtree Medium 12/16 has no text style token | Feature-private derived style (R9). |
| F5 | Top bar icons | Screens use the kit icon button (circle, onSurface); the DS page has a 16-radius, onSurfaceVariant icon button | Screens follow their instances (R6). The DS Icon button is still built. |
| F6 | Create button on empty form | Drawn enabled; the designer note says disabled until valid | Disabled via the DS Button Disabled state `406:271` (spec Assumptions). |
| F7 | Select focused state | Arrow stays `arrow_drop_down` while open (`2415:10169`) even though `icon/arrow_drop_up` exists | Follow the node: no arrow flip. |
| F8 | Export icon | Present in the list top bar | Hidden until the export feature ships (FR-009). |
| F9 | Expanded row amount | The expanded card's inner `pr 4dp` places the amount 4dp further in than in the collapsed row (`2414:5066` vs `2414:5064`) | Follow the node values. The 4dp shift is Figma-sourced, and verification should expect it. |

---

## R15. Design-system refetch before implementation (2026-11-16)

At the owner's request, every design-system node on page `0:1` was refetched with `get_design_context` (MCP session `a61810af-efd7-4751-83a3-96fa87153e89`) before any component code was written. The contract tables were then diffed against the live data, and [contracts/design-system-components.md](./contracts/design-system-components.md) was corrected where it was wrong or incomplete.

| Node | What | Result |
|---|---|---|
| `0:1` | Components page | `get_metadata` OK. Every node id in the contract exists |
| `1:732` | Colour scheme | `get_variable_defs` OK. Every `Schemes/*` value equals `Color.kt` |
| `2416:30065` | Typography | `get_variable_defs` OK. Every `material-theme/*` style equals `Type.kt` |
| `406:110` | Button (24 variants) | OK. Matches the contract on every token |
| `406:3390` | Icon button (14 variants) | OK. One gap (D1) |
| `410:2338` | FAB | OK. One note (D2) |
| `406:5441`, `406:5462` | Segmented button + building blocks | OK. One correction (D3) |
| `2417:12817` | Segmented button instance in the Create sheet | OK. Confirms the icon + label configuration |
| `407:1793` | Text field (8 variants) | OK. Gaps D4–D6 |
| `2415:10171`, `2415:10118`, `2415:10072` | Select, Select-list, list-item | OK. Gaps D7–D9 |
| `2415:11244`, `2415:11280` | Repeat Select instances (closed / open) | OK. Confirm `tertiaryContainer` tile and 52dp `TextIndented` rows |
| `2417:12066`, `2415:6632` | Top app bar instances | OK. Match R6 |
| `406:2552`, `406:2443` | Kit Top app bar / Icon button masters | **Still rejected** ("invalid node selection"). The owner-approved instance exception (R6, F5) stays in force |
| 8 icon symbols (R5) | `get_design_context` + SVG export | OK. Every SVG is a 24×24 viewBox with the glyph in frame coordinates |

**Differences found and how they are resolved** (Figma wins over the earlier contract text):

| # | Component | Live Figma | Resolution |
|---|---|---|---|
| D1 | Icon button, Standard Disabled | The icon is `onSurface` (not `onSurfaceVariant`), with the whole button at 38% | Implement as fetched |
| D2 | FAB Pressed | A frozen ripple frame: a full `onSecondaryContainer`@8% layer plus a 12% ripple wave | The Compose ripple with pressed alpha 0.12 is the animated equivalent. No static 8% layer is faked |
| D3 | Segmented segment | Track 48 with 4dp vertical padding leaves a **40dp** segment. The `py 10` content (44dp) is centred and clipped inside it | Segment = 40dp tall, content centred, horizontal padding 12. The 10dp vertical padding is not applied |
| D4 | Text field caret | `primary` when Focused, **`error`** in the Error state | Cursor brush follows the state |
| D5 | Text field trailing icon | `ArrowDropDown` 20dp in **`onSurfaceVariant`** | Implement as fetched |
| D6 | Text field Disabled | The field background stays a solid `surfaceContainerLowest`; the border and content layer are at 38%; label and supporting text at 38%; input and placeholder text are `onSurface` | Implement as fetched |
| D7 | Select press feedback | No state layer is drawn for the anchor or the rows; "Focused" only means the list is open | **Owner decision 2026-11-16: no press feedback** (`indication = null`) |
| D8 | Select-list scrollbar | `showScroll` is a property; the 5-row repeat list (`2415:7969`) shows a static 100dp thumb even though it cannot scroll | **Owner decision 2026-11-16: show the thumb only when the list can scroll**; the fixed 100dp thumb moves with the scroll position |
| D9 | Select, Text type | Anchor and rows show the symbol (`labelMedium`, `onSurface`) followed by supporting text (`bodyMedium`, `onSurfaceVariant`) | The anchor shows `selectedText` plus the selected option's `supportingText` |
