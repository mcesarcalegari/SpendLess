# Contract: Transactions UI (screens + feature components)

**Module**: `:feature:transactions:presentation`. Screens follow `android-presentation-mvi`: a Root and a Screen in one file. Feature components are `internal` and are built **only** from `:core:design-system` components and tokens. Figma base: `https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=`.

Each composable's KDoc must carry the `Figma:` line(s) listed here (Constitution VII).

---

## Navigation

```kotlin
@Serializable data object TransactionsGraphRoute
@Serializable data object TransactionListRoute

fun NavGraphBuilder.transactionsGraph(navController: NavController, onNavigateBack: () -> Unit)
```

`navigation<TransactionsGraphRoute>(startDestination = TransactionListRoute) { composable<TransactionListRoute> { … } }`. The create sheet is **not** a destination: it is hosted by the list screen (research R7).

`:app` hosts the graph. No dashboard exists yet, so `TransactionListRoute` is the start destination. Back on an empty back stack finishes the activity (spec Assumption: entry point).

---

## Screen 1: `TransactionListRoot` / `TransactionListScreen`

Figma: `2417:12042` (default), `2417:12068` (scrolled, pinned header).

```kotlin
@Composable fun TransactionListRoot(onNavigateBack: () -> Unit, viewModel: TransactionListViewModel = koinViewModel())
@Composable fun TransactionListScreen(state: TransactionListState, onAction: (TransactionListAction) -> Unit, onNavigateBack: () -> Unit, createSheetContent: @Composable () -> Unit)
```

| Element | Implementation | Tokens / spec |
|---|---|---|
| Background | `Scaffold(containerColor = background)` | `Schemes/Background` |
| Top bar | DS `SpendLessTopAppBar("All Transactions")` + nav `ArrowBack` ("Back") | `2417:12066`. Download action **omitted** (FR-009, F8) |
| List | `LazyColumn`, h-padding 16dp, `verticalArrangement = spacedBy(12.dp)`, `key = id`, `contentType` | `2417:12043` |
| Day header | `stickyHeader`, full-width `background`, text feature style "Medium 12/16 @70% onSurface" | `2417:12045`, F4 |
| Group spacing | last item of each group gets `padding(bottom = 8.dp)` | `2417:12047` `pb-8` |
| Row | `TransactionRow` (below) | `423:4852` |
| FAB | DS `SpendLessFloatingActionButton(Add, "Add transaction")`, end 24dp, 16dp above the nav-bar inset | `2417:12067` |
| Create sheet | `CreateTransactionSheet` when `isCreateSheetOpen` | `2415:6629` |
| Menu / dialog | `StopRepeatingMenu`, `StopRepeatingDialog` | no node (R11) |

**Previews**: populated (today, yesterday and older), pinned-header scroll state, one expanded expense and one expanded income, long name, max amount, empty list.

### `TransactionRow` (internal)

Figma: collapsed `2414:5064` (expense) / `2414:5093` (income); expanded `2414:5065` / `2414:5106`; master `423:4852`.

```kotlin
@Composable internal fun TransactionRow(
    item: TransactionUi,
    isExpanded: Boolean,
    onNoteBadgeClick: () -> Unit,
    onLongPress: (() -> Unit)?,   // null when not in a running series (FR-037)
    modifier: Modifier = Modifier,
)
```

All values are in research R9. Summary:

- 44dp tile, radius 12.
- Badge 20dp at (26, 26), radius 6, shadow `raised`, `Notes` 14dp tinted per the R9 table.
- Name `labelMedium` with ellipsis; label `bodyXSmall`@70%.
- Amount `titleLarge` with `widthIn(min = 100.dp)`; `success` for income.
- Expanded: card `surfaceContainerLowest`, radius 16, shadow `raised`, padding 4/4/4/10, overhanging 4dp each side. Note `bodySmall`, start padding 52dp.
- Long-press: `pointerInput { detectTapGestures(onLongPress) }` + `HapticFeedbackType.LongPress`, plus a semantics `customActions` "Stop repeating".

**Previews**: expense collapsed with no note, with note, expanded; income collapsed with note, expanded; long name; `-$999,999,999.99`; parentheses format.

### `StopRepeatingMenu` / `StopRepeatingDialog` (internal)

- Menu: `Popup` below the row; Select-list surface tokens (`2415:10118`); one 48dp row "Stop repeating", `labelMedium`, start padding 16dp.
- Dialog: M3 `AlertDialog`, `surfaceContainerLowest`, radius 28. Title "Stop repeating %1$s?" in `titleLarge`. Confirm DS Text button "Stop"; dismiss DS Text button "Cancel".
- KDoc: "No Figma node; composed from DS per user decision 2026-11-13 (research R11)."

---

## Screen 2: `CreateTransactionRoot` / `CreateTransactionScreen`

Figma (per state, for previews and KDoc):

| State | Node |
|---|---|
| Empty, `-$10` | `2415:6533` (sheet `2415:6629`) |
| Empty, `($10)` | `2415:10959` (sheet `2415:11055`) |
| Typing name | `2415:7218` (sheet `2415:7314`) |
| Category list open | `2415:9674` (sheet `2415:9770`) |
| Filled, repeat list open | `2415:7873` (sheet `2415:7969`) |
| Filled | `2443:9436` (sheet `2443:9532`) |
| Filled, income | `2415:8542` (sheet `2415:8638`) |

```kotlin
@Composable fun CreateTransactionRoot(onClose: () -> Unit, onCreated: () -> Unit, viewModel: CreateTransactionViewModel = koinViewModel())
@Composable fun CreateTransactionScreen(state: CreateTransactionState, onAction: (CreateTransactionAction) -> Unit, onCloseClick: () -> Unit)
```

| Element | Implementation | Node / tokens |
|---|---|---|
| Sheet | M3 `ModalBottomSheet`; `surfaceContainerLow`; top radius 28; `dragHandle = null`; scrim `onBackground`@32%; `skipPartiallyExpanded` | `2415:6631`, R7 |
| Header | DS `SpendLessTopAppBar("Create Transaction")` title-only + action `Close` ("Close") | `2415:6632` |
| Type toggle | DS `SpendLessSegmentedButton`: Expense (`TrendingDown`) / Income (`TrendingUp`) | `2417:12817` |
| Name input | `CounterpartyInput` (internal), placeholder "Receiver"/"Sender" | `2415:6636`, `2415:7321`, R8 |
| Amount input | `AmountInput` (internal), focused on open | `2415:6638`, `2415:11064`, R8 |
| Note | `NoteInput` (internal): idle "+ Add Note" row; typing per approved deviation | `2415:6641`, R8 |
| Category | DS `SpendLessSelect`, tile `primaryFixed`, style Icon; hidden for income while its 48dp slot is kept (bottom-aligned filters block) | `2415:10578`, `2415:9786`, R7 |
| Repeat | DS `SpendLessSelect`, emoji 🔄, tile `tertiaryContainer`, list style TextIndented; field shows the short label | `2415:11244`, `2415:11280` |
| Create | DS `SpendLessButton(Filled, "Create", enabled = isCreateEnabled)` full width | `2415:6659`, disabled `406:271` |
| Layout | Column: segmented → 34dp → inputs (300dp, centred, gap 12) → 32dp → filters (104dp fixed, gap 8, bottom-aligned) → 12dp → Create | R7 |

**Strings** (English only; `values/strings.xml` of the feature):

- "All Transactions", "Create Transaction", "Expense", "Income", "Receiver", "Sender", "Add Note", "Create".
- Repeat options: "Does not repeat", "Daily", "Weekly", "Monthly", "Yearly", "Weekly on %1$s", "Monthly on the %1$s", "Yearly on %1$s".
- The seven category names, "TODAY", "YESTERDAY", "Stop repeating", "Stop repeating %1$s?", "Stop", "Cancel".
- Content descriptions: "Back", "Add transaction", "Close", "Show note", "Hide note".

Weekday, month and ordinal text is built with `Locale.ENGLISH` regardless of the device locale.

---

## UI verification targets (AGENTS §5.2)

| Composable / screen | `android-preview-figma-verify` node | `android-ui-verify` + `android-ui-screenshot-verify` node |
|---|---|---|
| `TransactionListScreen` | `2417:12042` | `2417:12042`, `2417:12068` (export icon expected to be absent) |
| `TransactionRow` previews | `2414:5142` | — |
| `CreateTransactionScreen` previews | each node in the state table | `2415:6533`, `2415:9674`, `2415:7873`, `2415:8542` |
| Each DS component preview | its variant node in design-system-components.md | — |
