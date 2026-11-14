# Contract: `:core:design-system` components

This document defines the public API and Figma token tables for each shared component. It covers the components the user named: **buttons, FAB, segmented button, text field, icon button, dropdown (Select)**. It also covers the top app bar, theme additions, shadows and icons, which this feature needs.

**Rules for every component below**:

- `public` visibility, with a `modifier: Modifier = Modifier` parameter.
- Strings come from the caller.
- Colours come only from `MaterialTheme.colorScheme` or `MaterialTheme.spendLessColors`. Hard-coded `Color(...)` is not allowed.
- Shapes, sizes and paddings come from the tables below.
- Elevation is always `0.dp`. Shadows come from `SpendLessShadows` (see §0).
- The state layer is the Figma flat overlay. Each component sets it through `LocalRippleConfiguration` with the colour and alpha listed in its table (research R3).
- KDoc carries `Figma:` lines for the component set and for each variant (Constitution VII).
- Previews: one per distinct state (Enabled, Pressed, Focused, Disabled, Selected, Error, Placeholder, with icon left or right, long text). Figma ships a light scheme only, so there are **no dark previews**.
- "`@x%`" means `color.copy(alpha = x/100)`.

Figma base URL: `https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=`

---

## 0. Theme additions

| Item | Change | Source |
|---|---|---|
| Colour scheme | Wire `primaryFixed #EADDFF`, `primaryFixedDim #D2BCFF`, `onPrimaryFixed #24005A`, `onPrimaryFixedVariant #5900C7`, `secondaryFixed #E5EA58`, `secondaryFixedDim #C9CE3E`, `onSecondaryFixed #1C1D00`, `onSecondaryFixedVariant #484A00` into `lightColorScheme(...)` (material3 ≥ 1.4, research R2) | `1:972`, variables `Schemes/*` |
| `spendLessColors.success` | Already present (`#29AC08`) | `Schemes/Success` |
| Typography | Already matches Figma `material-theme/*` (title/large 20/26 SB, title/medium 16/24 SB, label/medium 16/24 M, label/small 14/20 M, body/medium 16/24 R, body/small 14/20 R, body/x-small 12/16 R, display/medium 36/44 SB) | `2416:30065` |
| `SpendLessShadows` | `select` (0,6, blur 10, spread 0, `#180040`@8%) · `raised` (0,8,12,0,@10%) · `fab` (0,8,24,4,@16%), applied through `Modifier.dropShadow` | research R4 |
| `SpendLessIcons` | `ArrowBack`, `Close`, `Add`, `Check`, `ArrowDropDown`, `Notes`, `TrendingDown`, `TrendingUp`. Painters from regenerated 24×24-viewport vectors | research R5 |
| `SpendLessTheme` | Rename `SpendLessSampleTheme` → `SpendLessTheme`, moved from `:app` | — |

---

## 1. `SpendLessButton`

Figma: `406:110` (set). Variants: Filled Enabled `406:283`, Pressed `406:274`, Focused `406:277`, Disabled `406:271`, icon Left `406:267`, Right `406:467`; Text Enabled `406:213`, Pressed `406:204`, Focused `406:207`, Disabled `406:201`, icon Left `406:197`, Right `406:427`.

```kotlin
enum class SpendLessButtonStyle { Filled, Text }

@Composable
fun SpendLessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SpendLessButtonStyle = SpendLessButtonStyle.Filled,
    enabled: Boolean = true,
    leadingIcon: Painter? = null,
    trailingIcon: Painter? = null,
)
```

Base: M3 `Button` (Filled) / `TextButton` (Text).

| Token | Filled | Text |
|---|---|---|
| Height | 48dp (`heightIn(min = 48.dp)`) | 48dp |
| Shape | `RoundedCornerShape(16.dp)` | 16dp |
| Container | `primary` | transparent |
| Content | `onPrimary` | `primary` |
| Disabled container | `onSurface`@12% | transparent |
| Disabled content | `onSurface`@38% | `onSurface`@38% |
| State layer (pressed/focused) | `onPrimary`@12% | `primary`@12% |
| Padding, no icon | h 24dp, v 10dp | h 12dp, v 10dp |
| Padding, icon left | start 16, end 24 | start 12, end 16 |
| Padding, icon right | start 24, end 16 | start 16, end 12 |
| Icon | 18dp, gap 8dp, tinted as the label | same |
| Label | `titleMedium` (SemiBold 16/24) | same |

---

## 2. `SpendLessIconButton`

Figma: `406:3390` (set). Filled Enabled `406:3447`, Pressed `406:3435`, Focused `406:3439`, Disabled `406:3431`; Standard Enabled `406:3467`, Pressed `406:3455`, Focused `406:3459`, Disabled `406:3451`; Error Enabled `406:3427`, Pressed `2416:30521`, Focused `2416:30505`; On Primary Enabled `2416:30246`, Pressed `2416:30234`, Focused `2416:30527`.

```kotlin
enum class SpendLessIconButtonStyle { Filled, Standard, Error, OnPrimary }

@Composable
fun SpendLessIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SpendLessIconButtonStyle = SpendLessIconButtonStyle.Standard,
    enabled: Boolean = true,
)
```

Base: M3 `FilledIconButton` (Filled, Error, OnPrimary) / `IconButton` (Standard).

The outer box is 48dp with 2dp padding, so the container is **44dp** with radius **16dp**. The icon is 24dp and centred.

| Style | Container | Icon | State layer (pressed/focused) | Disabled |
|---|---|---|---|---|
| Filled | `primary` | `onPrimary` | `onPrimary`@12% | container `onSurface`@12%, icon `onSurface`@38% |
| Standard | none | `onSurfaceVariant` | `onSurfaceVariant`@12% | whole button 38% opacity |
| Error | `error`@8% | `error` | `error`@12% | — (no Figma variant, so `enabled` is ignored visually) |
| OnPrimary | `onPrimary`@12% | `onPrimary` | `onPrimary`@12% | — |

---

## 3. `SpendLessFloatingActionButton`

Figma: `410:2338`. Enabled `410:2375`, Focused `410:2369`, Pressed `410:2365`.

```kotlin
@Composable
fun SpendLessFloatingActionButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Base: M3 `FloatingActionButton` with `elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)`.

| Token | Value |
|---|---|
| Size | 64dp |
| Shape | `RoundedCornerShape(20.dp)` |
| Container | `secondaryContainer` |
| Content | `onSecondaryContainer` |
| Icon | 28dp (padding 16dp) |
| Shadow | `SpendLessShadows.fab` |
| State layer | `onSecondaryContainer`@12% |

---

## 4. `SpendLessSegmentedButton`

Figma: `406:5441` (group), building blocks `406:5462`. Label Selected Enabled `406:5583`, Unselected `406:5564`, Pressed `406:5499` / `406:5480`, Focused `406:5527` / `406:5508`.

```kotlin
@Immutable
data class SegmentedOption<T>(val value: T, val label: String, val icon: Painter? = null)

@Composable
fun <T> SpendLessSegmentedButton(
    options: ImmutableList<SegmentedOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
)
```

Base: `Row(Modifier.selectableGroup())` containing an M3 `Surface(selected = …, onClick = …)` per segment. The M3 `SegmentedButton` was rejected (research R3).

| Token | Value |
|---|---|
| Track | height 48dp, radius 16dp, colour `primaryContainer`@8%, horizontal padding 4dp |
| Segment | `weight(1f)`, vertical padding 4dp, inner shape radius **12dp** |
| Selected segment | container `surfaceContainerLowest`; label and icon `primary` |
| Unselected segment | container transparent; label and icon `onPrimaryFixed`@70% |
| Content | h-padding 12dp, v-padding 10dp, icon 18dp, gap **6dp** |
| Label | `titleMedium` |
| State layer | `primaryContainer`@8% |
| Semantics | `Role.Tab`, `selected` |

---

## 5. `SpendLessTextField`

Figma: `407:1793`. Input text: Enabled `407:2445`, Focused `407:2433`, Error `407:2410`, Disabled `407:2398`. Placeholder: Enabled `407:2338`, Focused `407:2326`, Error `407:2304`, Disabled `407:2292`.

```kotlin
@Composable
fun SpendLessTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    showDropDownIcon: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
)
```

Base: `BasicTextField` + `decorationBox`. `OutlinedTextField` was rejected (research R3). This feature does not use the text field; it is built at the user's request for reuse.

| Token | Value |
|---|---|
| Layout | Column: label → field → supporting, gap **7dp** |
| Label | `labelSmall` (Medium 14/20), `onSurface` |
| Field | height 48dp, radius 16dp, background `surfaceContainerLowest`, border 1dp |
| Border | Enabled `outline` · Focused `primary` · Error `error` · Disabled `outline` with the whole field at 38% |
| Content padding | start 16dp, end 12dp, vertical 4dp; trailing gap 4dp |
| Input | `bodyMedium` (Regular 16/24), `onSurface`; cursor `primary` |
| Placeholder | `bodyMedium`, `onSurfaceVariant` (70% while focused); `onSurface` when disabled |
| Trailing icon | `ArrowDropDown`, 20dp (`showDropDownIcon`) |
| Supporting | `bodySmall` (Regular 14/20): `onSurfaceVariant` · Error `error` · Disabled `onSurface`@38% |
| Disabled | label, field and supporting at 38% opacity |

---

## 6. `SpendLessSelect` (dropdown)

Figma: Select `2415:10171`. Icon Enabled `2415:10170`, Icon Focused `2415:10169`, Text Enabled `2415:10277`, Text Focused `2415:10331`. Select-list `2415:10118`; list-item `2415:10072`.

```kotlin
@Immutable
data class SelectOption<T>(
    val value: T,
    val label: String,                 // list text (e.g. "Weekly on Monday")
    val leadingEmoji: String? = null,  // "Icon" type tile
    val leadingSymbol: String? = null, // "Text" type, e.g. "$"
    val supportingText: String? = null,
)

@Composable
fun <T> SpendLessSelect(
    options: ImmutableList<SelectOption<T>>,
    selected: T,
    selectedText: String,              // short form shown in the field (e.g. "Weekly")
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fieldLeadingEmoji: String? = null,
    leadingTileColor: Color = MaterialTheme.colorScheme.primaryFixed,
    listItemStyle: SelectListItemStyle = SelectListItemStyle.Icon, // Icon | Text | TextIndented
)
```

Base for the anchor: M3 `Surface(onClick)`. Base for the list: `Popup` + custom `PopupPositionProvider`, an M3 `Surface`, and a `LazyColumn` of rows using `Modifier.selectable`. `DropdownMenu` and `ExposedDropdownMenu` were rejected (research R3).

| Token | Value |
|---|---|
| Anchor | height 48dp, fill width, radius 16dp, `surfaceContainerLowest`, shadow `select` |
| Anchor padding | Icon type: start 4dp; Text type: start 16dp. End 12dp, gap 8dp |
| Leading tile | 40dp, radius 12dp, `leadingTileColor` (category `primaryFixed`, repeat `tertiaryContainer`), emoji in `titleLarge` centred |
| Field text | `labelMedium` (Medium 16/24), `onSurface`, `weight(1f)` |
| Supporting (Text type) | `bodyMedium`, `onSurfaceVariant` |
| Trailing | `ArrowDropDown` 24dp `onSurface`. It does **not** flip while open (F7) |
| List position | 4dp below the anchor (`top = 52`), same width as the anchor |
| List surface | `surfaceContainerLowest`, radius 16dp, shadow `select`, no vertical padding |
| Row | height 48dp, end padding 12dp, gap 8dp. Start padding: Icon 4dp · Text 16dp · TextIndented **52dp** (repeat list, `2415:7969`) |
| Row text | `labelMedium`, `onSurface` |
| Selected marker | `Check` 24dp `primary`, end 12dp, vertically centred |
| Max visible | 5 rows (240dp), then scroll |
| Scrollbar | 4dp wide, height 100dp, `onSurface`@12%, fully rounded, 2dp from the end edge, top inset 14dp |
| Dismiss | outside tap, or picking an item calls `onExpandedChange(false)` |
| Semantics | Anchor `Role.DropdownList`; rows `selected` |

---

## 7. `SpendLessTopAppBar`

Figma (instances; master unreachable, research R6): leading-nav variant `2417:12066`; title-plus-close variant `2415:6632`.

```kotlin
@Composable
fun SpendLessTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
)

@Composable
fun SpendLessTopAppBarIconButton(icon: Painter, contentDescription: String, onClick: () -> Unit)
```

Base: M3 `TopAppBar` with `colors = transparent`, `windowInsets = WindowInsets(0)` and `expandedHeight = 64.dp`.

| Token | Value |
|---|---|
| Height | 64dp |
| Padding | with nav icon: h 4dp; title only: start 16dp, end 4dp; vertical 8dp |
| Title | `titleLarge`, `onSurface` |
| Icon button | 48dp touch, **40dp circular** state layer, icon 24dp `onSurface` |
