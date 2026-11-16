# Feature Specification: Transactions

**Feature Branch**: `001-transactions`

**Created**: 2026-11-13

**Status**: Draft

**Input**: User description: "We are building a transactions feature for a finance tracking app, which is used to help the user track their expenses and income, both as one-time expenses and as recurring expenses or recurring income. Overall they can see how their income and expenses develop and evolve over time. The transaction feature involves: a transaction list screen that lists all transactions in grouped sections for each day; a create transaction sheet, which can be used in order to create a new transaction that can, again, either be income or expense. It has a category and has a specific repeat type, which could be one time, weekly, or monthly. The exact repeat types you can read from the Figma mockups. Use these Figma mockups as the single source of truth."

**Amendment (2026-11-13)**: "Update the existing specification with more clarity on how a note is being displayed for a specific item. Specifically there is an expanded view for each transaction in the list that can be toggled when clicking on the little note icon (in case there is a note for this transaction). [...] Regarding the formatting preferences it's not just about the expense formatting that could be displayed with a parenthesis or a minus, but also the preferences regarding the decimal separator and the thousand separator, which all come from a different feature. Implementing this preference is out of scope for this feature but we should already implement it in a way that these preferences in general exist but are hard-coded to some kind of valid value so adjustments in the future are easy."

## Design Source *(Figma — single source of truth)*

All frames live in the section **"Transactions screen"** of the SpendLess – Mauro Team file:
https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-4433

| Frame | What it shows | Link |
|---|---|---|
| All Transactions | Full history, grouped by day, with the add button | [2417-12042](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12042) |
| All Transactions – Scroll | Scrolled list; the current day header stays pinned under the title bar | [2417-12068](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2417-12068) |
| Create Transaction – Empty – Formatting `-$10` | Empty form, expense amount shown as minus-prefixed | [2415-6533](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-6533) |
| Create Transaction – Empty – Formatting `($10)` | Empty form, expense amount shown in parentheses | [2415-10959](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-10959) |
| Create Transaction – Typing Name | Receiver name being typed | [2415-7218](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7218) |
| Create Transaction – Category Select Active | Category list open, current choice ticked | [2415-9674](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-9674) |
| Create Transaction – Filled (repeat list open) | Expense filled in, repeat options list open | [2415-7873](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-7873) |
| Create Transaction – Filled | Expense filled in, ready to create | [2443-9436](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2443-9436) |
| Create Transaction – Filled – Income | Income filled in; no category picker | [2415-8542](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-8542) |
| Expense + Income (transaction row states) | Collapsed and note-expanded rows for an expense and an income. This frame sits outside the "Transactions screen" section. | [2414-5142](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2414-5142) |

Designer notes attached to the Create Transaction frames:

- "'Amount' is required field. Validation - only numbers, >0.00. 'Create' btn is disabled until 'Amount' is filled" ([2415-5138](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-5138))
- "'Amount' formatting according to user's preferences" ([2415-5139](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-5139))
- "Keyboard active by default" ([2415-5140](https://www.figma.com/design/xSFwRCFwe13SK2kAq85SLj/SpendLess---Mauro-Team?node-id=2415-5140))

### What the mockups add to the original description

1. **Five repeat options, not three.** The options are *Does not repeat*, *Daily*, *Weekly on [weekday]*, *Monthly on the [day]*, and *Yearly on [month day]*. The weekday, day and date in each label come from the transaction's own date (for example "Weekly on Monday", "Monthly on the 14th", "Yearly on Feb 14th"). Once chosen, the field shows the short form ("Weekly").
2. **Only expenses have a category.** When *Income* is selected, the category picker disappears. In the list, income rows always show "Income" as their label, with their own icon style.
3. **Every transaction has a counterparty name.** For an expense it is labelled *Receiver* (who was paid). For an income it is labelled *Sender* (who paid the user). This name is the row title in the list.
4. **Optional note, readable in the list.** The form has an *Add Note* action. In the list, rows with a note show a small note badge on their category icon. Tapping the badge expands the row in place: the row becomes a raised card, and the full note text appears below the name, wrapping over as many lines as needed. Tapping the badge again collapses it (frame `2414:5142`).
5. **Default values.** The form opens as *Expense*, with category *Other* and repeat *Does not repeat*.
6. **Amount rules.** The amount is required and must be a number greater than 0.00. *Create* stays disabled until it is valid. The product owner has also made the name required (see Clarifications).
7. **Amount display follows user preferences.** Every amount follows the user's amount-format preferences: currency symbol, negative-number style (`-$10` or `($10)`), decimal separator and thousands separator. Those preferences belong to a separate feature. Until it exists, they hold fixed defaults (see FR-032 to FR-035). In the create sheet, the sign is highlighted to mark the type: an outgoing sign for expenses, an incoming sign for incomes. In the list, expense amounts use the normal text colour and are told apart only by their negative format. Income amounts are positive and highlighted as incoming.
8. **Quick entry.** When the sheet opens, the keyboard is already up, so the user can start typing straight away.
9. **No date field.** The form has no date picker. A new transaction is recorded for the current day.
10. **Day headers.** Groups are titled "TODAY", "YESTERDAY", and then the month and day for older dates (for example "JANUARY 9", "DECEMBER 28"). The current group's header stays pinned while the user scrolls.
11. **Screen actions.** The list screen has a back action, a title ("All Transactions"), an export/download action in the top bar (hidden until the export feature exists, see FR-009), and a floating add (+) button that opens the create sheet.

## Clarifications

### Session 2026-11-13

- Q: How should future occurrences of a recurring transaction appear? → A: A new transaction is added automatically on each due date, from then on. Missed due dates are caught up the next time the app is opened.
- Q: Is the expense category list complete? → A: Yes. There are exactly seven: Clothing & Accessories, Education, Entertainment, Food & Groceries, Health & Wellness, Home, Other.
- Q: Can the Receiver/Sender name be left empty? → A: No. It is required, and *Create* stays disabled until both the name and the amount are filled in.
- Q: When a recurring transaction adds a new occurrence on its due date, is that occurrence a plain one-time copy, or does it become a recurring transaction too? → A: A plain one-time copy linked to the original. The original transaction alone holds the schedule, and the copies do not repeat.
- Q: What is the largest amount a user can enter for one transaction? → A: 999,999,999.99 (nine whole digits plus two decimals).
- Q: Should the export/download icon in the title bar appear on the *All Transactions* screen before the export feature exists? → A: No. It stays hidden until the export feature ships. This is a deliberate, recorded deviation from Figma.
- Q: If the app stays open past midnight and a recurring transaction falls due, should the new occurrence appear right away, or only the next time the app is opened? → A: Right away, at the date change, while the app is open.
- Q: What are the maximum lengths for the Receiver/Sender name and for the note? → A: The name can be up to 50 characters and the note up to 500 characters.
- Q: When a monthly repeat is anchored to the 31st, and a short month pushes one occurrence to an earlier day (for example Feb 28), does the next occurrence go back to the 31st, or stay on the earlier day? → A: It goes back to the original day (Jan 31 → Feb 28 → Mar 31 → Apr 30). A yearly repeat set on Feb 29 falls on Feb 28 in non-leap years and Feb 29 in leap years.
- Q: When a recurring occurrence is added automatically, where does it go in that day's list relative to transactions the user entered by hand? → A: It takes the original transaction's time of day on its due date, or the moment it is added if that is earlier, and is sorted by that time, both when added live and when caught up. (Amended 2026-11-16: the cap stops an occurrence added at midnight from ranking above transactions the user enters later that day.)
- Q: Is the app's text English only for this feature, or must it follow the phone's language? → A: English only. Day headers, month and weekday names, ordinals, repeat labels and category names are always in English, whatever the phone's language.
- Q: Must this feature be usable with the phone's screen reader, including spoken labels for the note badge and for whether a row is an expense or an income? → A: Only basic labels for the icon-only controls (back, add, close, note badge). Fuller screen-reader support is out of scope.
- Q: Should a recurring expense or income be able to end in this feature, for example on an end date or after a set number of times? → A: Yes, by the user stopping it from the list. Long-pressing any row that belongs to a recurring series (the original or any occurrence) opens a small menu with one item, "Stop repeating". Choosing it means no further occurrences are added after that day. There is no end date or end count in the create sheet.
- Q: Should the app ask "Are you sure?" before stopping a recurring series, since a stopped series can't be restarted? → A: Yes. A confirmation dialog names the transaction and asks the user to confirm or cancel.
- Q: Should rows that belong to a running recurring series look different in the list, so the user knows which rows can be long-pressed to stop? → A: No. Rows look exactly as in Figma, and the long-press menu is the only sign that a row repeats.

### Session 2026-11-13 (amendment)

- Q: How is a transaction's note shown after it is created? → A: In the list, the row expands in place when the user taps its note badge, showing the full note (frame `2414:5142`). Tapping the badge again collapses it.
- Q: Which amount-format preferences does this feature follow? → A: All of them: the currency symbol, the negative-number style, the decimal separator and the thousands separator. Editing them is out of scope. This feature reads them from one shared place, which holds fixed defaults until the preferences feature exists.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Record a one-time expense (Priority: P1)

The user has just spent money and wants to record it quickly. They open the create sheet from the transaction list. They type who they paid and the amount, optionally pick a category and add a note, and tap *Create*. The new expense then appears in today's group.

**Why this priority**: Recording spending is the main reason the app exists. Without it there is nothing to list or analyse.

**Independent Test**: From an empty history, open the sheet, enter "Netflix" and 15.99, choose *Entertainment*, and create it. A single "Netflix / Entertainment / −$15.99" row appears under TODAY.

**Acceptance Scenarios**:

1. **Given** the transaction list is shown, **When** the user taps the add (+) button, **Then** the Create Transaction sheet opens with *Expense* selected, category *Other*, repeat *Does not repeat*, an empty amount (placeholder 00.00), the *Receiver* placeholder, and the keyboard already shown.
2. **Given** the sheet is open with the name or the amount missing, **When** the user looks at *Create*, **Then** it is disabled. It becomes enabled once both a name and an amount greater than 0.00 are entered.
3. **Given** the user has entered a name and an amount greater than 0.00, **When** they tap *Create*, **Then** the sheet closes and the new expense appears at the top of the TODAY group, shown as a negative, outgoing amount.
4. **Given** the user opens the category field, **When** the list of categories appears, **Then** the current category is ticked, and choosing another category closes the list and shows the new choice in the field.
5. **Given** the user taps *Add Note* and enters text, **When** the expense is created, **Then** its row in the list shows the note badge on the category icon.
6. **Given** the sheet is open, **When** the user taps the close (✕) control, **Then** the sheet closes and no transaction is created.

---

### User Story 2 - Browse transaction history grouped by day (Priority: P1)

The user opens *All Transactions* to see where their money went and came from, newest first, grouped by day.

**Why this priority**: Seeing past income and spending over time is the second half of the app's core value, and it confirms that recording worked.

**Independent Test**: Seed transactions dated today, yesterday and two older dates. Open the screen and check the group headers, their order, and the content of each row.

**Acceptance Scenarios**:

1. **Given** transactions exist on several dates, **When** the list is shown, **Then** they are grouped by calendar day, newest day first. The headers read "TODAY", "YESTERDAY", and then the month and day (for example "JANUARY 9").
2. **Given** an expense row, **When** it is displayed, **Then** it shows the category icon, the receiver name, the category name, and a negative amount in the normal text colour, using the user's chosen negative-number format.
3. **Given** an income row, **When** it is displayed, **Then** it shows the income icon style, the sender name, the label "Income", and a positive amount highlighted as incoming.
4. **Given** a name too long for the row, **When** it is displayed, **Then** it is shortened with an ellipsis and the amount stays fully visible.
5. **Given** the list is longer than the screen, **When** the user scrolls, **Then** the header of the group currently at the top stays pinned under the title bar, and the add button stays available.
6. **Given** the list screen, **When** the user taps back, **Then** they return to the screen they came from.

---

### User Story 3 - Read a transaction's note in the list (Priority: P2)

The user wants to remember why a transaction happened, for example who they had coffee with. Without leaving the list, they tap the note badge on that row to read the note, then tap it again to hide it.

**Why this priority**: A note is only useful if it can be read later. Expanding the row in place keeps the list compact and puts the context exactly where the user is looking.

**Independent Test**: Seed one expense and one income, each with a multi-line note, and one transaction without a note. Tap each badge and check that the row expands to show the full note. Tap again and check that it collapses. Check that the row without a note has no badge and cannot be expanded.

**Acceptance Scenarios**:

1. **Given** a collapsed row whose transaction has a note, **When** the user taps the note badge, **Then** the row expands in place into a raised card. The full note appears below the name and category, aligned with the name, and wraps over as many lines as it needs. The note badge switches from its subdued look to a stronger, active look. The icon, name, label and amount stay in place.
2. **Given** an expanded row, **When** the user taps the note badge again, **Then** the row collapses back to its normal one-line height, the note text is hidden, and the badge returns to its subdued look.
3. **Given** a row whose transaction has no note, **When** it is displayed, **Then** it shows no badge and cannot be expanded.
4. **Given** one row is expanded, **When** the user taps the badge on another row, **Then** that row expands too. Each row expands and collapses on its own.
5. **Given** an expanded expense row and an expanded income row, **When** they are compared, **Then** both look the same as each other, apart from the icon style and the amount styling already defined for each type.

---

### User Story 4 - Record income (Priority: P2)

The user receives money (for example a friend's share of a gift) and records it as income.

**Why this priority**: Income is needed to see the full financial picture. It reuses the same flow as expenses, so it adds little extra effort.

**Independent Test**: Open the sheet, switch to *Income*, enter sender "Rick" and 20.00, and create it. A positive, incoming "Rick / Income / $20.00" row appears under TODAY.

**Acceptance Scenarios**:

1. **Given** the sheet is open on *Expense*, **When** the user selects *Income*, **Then** the name placeholder changes to *Sender*, the amount is shown as positive and incoming, and the category field is hidden.
2. **Given** *Income* is selected and a valid amount is entered, **When** the user taps *Create*, **Then** an income transaction is recorded with no user-chosen category and shows "Income" as its label in the list.
3. **Given** the user switches from *Income* back to *Expense*, **When** the form updates, **Then** the category field reappears and the name, amount, note and repeat values the user already entered are kept.

---

### User Story 5 - Set up a recurring expense or income (Priority: P2)

The user records something that happens regularly, such as a monthly gym membership or a weekly allowance, so it is tracked over time without being entered again each time.

**Why this priority**: The user's description names recurring items as a core goal. The mockups list it as a field on every transaction.

**Independent Test**: Create a "Gym" expense of 45.99 with repeat *Monthly on the [today's day]*, then move the device date forward past the next due date. A matching transaction is present on that date.

**Acceptance Scenarios**:

1. **Given** the sheet is open, **When** the user opens the repeat field, **Then** the options *Does not repeat*, *Daily*, *Weekly on [today's weekday]*, *Monthly on the [today's day, ordinal]*, and *Yearly on [today's month and day]* are listed, and the current choice is ticked.
2. **Given** the user chooses *Weekly on Monday*, **When** the list closes, **Then** the field shows "Weekly".
3. **Given** a transaction was created with a repeat other than *Does not repeat*, **When** a later due date arrives, **Then** a new transaction with the same type, name, amount, category and note is added automatically to that day's group. It is not created in advance, so no future-dated rows appear. The added transaction is a one-time copy and does not add occurrences of its own.
4. **Given** a recurring transaction whose due dates passed while the app was closed, **When** the user next opens the app, **Then** one transaction is added for each missed due date, each in its own day's group.
5. **Given** a transaction created with *Does not repeat*, **When** any later date arrives, **Then** no further transactions are added for it.

---

### User Story 6 - Stop a recurring expense or income (Priority: P2)

The user cancels a subscription or stops receiving a regular payment. From the list, they stop the series so no more occurrences are added, and their past records stay as they are.

**Why this priority**: Without a way to stop it, a recurring transaction would add entries forever and the history would become wrong. The repeat options alone give no end date or end count.

**Independent Test**: Create a "Gym" expense with repeat *Daily*. Move the device date forward two days, long-press one of the Gym rows and choose *Stop repeating*. Move the date forward again. No new Gym rows appear, and the existing ones are still listed.

**Acceptance Scenarios**:

1. **Given** a row that belongs to a recurring series that is still running (the original or any occurrence), **When** the user long-presses it, **Then** a small menu opens next to the row with one item, *Stop repeating*.
2. **Given** that menu is open, **When** the user chooses *Stop repeating*, **Then** the menu closes and a confirmation dialog appears that names the transaction (for example "Stop repeating Gym?"), with *Stop* and *Cancel* actions.
3. **Given** the confirmation dialog is shown, **When** the user confirms with *Stop*, **Then** the series is stopped and no occurrences are added for any due date after that day. This includes due dates that would otherwise be caught up later.
4. **Given** the confirmation dialog is shown, **When** the user chooses *Cancel* or dismisses the dialog, **Then** the series keeps running and nothing changes.
5. **Given** a series has been stopped, **When** the list is shown, **Then** the original and every occurrence added before the stop are still listed, unchanged.
6. **Given** that menu is open, **When** the user taps outside it, **Then** the menu closes and nothing changes.
7. **Given** a row that is not part of a running recurring series (a one-time transaction, or a series that is already stopped), **When** the user long-presses it, **Then** nothing happens.

---

### User Story 7 - Amounts follow the user's formatting preferences (Priority: P3)

Amounts everywhere in this feature follow the user's amount-format preferences (currency symbol, negative-number style, decimal separator, thousands separator), so they look the way the user expects. Choosing these preferences belongs to a separate feature. This feature only follows them. Until that feature exists, the defaults apply: `$`, minus prefix, `.` as the decimal separator, `,` as the thousands separator.

**Why this priority**: It affects how readable the app is, but not whether data is correct. The core flows work with a default format.

**Independent Test**: Run the feature with its default preferences, then with each preference value changed in the one shared place where the preferences are kept, and check the create sheet and the list rows each time.

**Acceptance Scenarios**:

1. **Given** the user's preference is minus-prefix, **When** the empty expense form is shown, **Then** the amount appears as `-$ 00.00`.
2. **Given** the user's preference is parentheses, **When** the empty expense form is shown, **Then** the amount appears as `($ 00.00 )`. List rows also show expenses in parentheses (for example `($45.99)`): this follows from the designer note, because the frame itself still draws list rows with a minus prefix.
3. **Given** the default preferences, **When** an expense of 1234.5 is shown in the list, **Then** it reads `-$1,234.50`.
4. **Given** the decimal separator is `,` and the thousands separator is `.`, **When** the same expense is shown, **Then** it reads `-$1.234,50`. While typing an amount in the create sheet, the user enters decimals with `,`.
5. **Given** the thousands separator is a space, **When** an income of 12000 is shown, **Then** it reads `$12 000.00`.
6. **Given** the preferences feature does not exist yet, **When** the app is used, **Then** every amount uses the defaults, and the user is offered no way to change them.

---

### Edge Cases

- **Amount of zero, or empty**: *Create* stays disabled.
- **Non-numeric input** (letters, symbols, minus signs): it is not accepted into the amount. The sign is set by the Expense/Income choice, not typed by the user.
- **More than two decimal places**: input is limited to two decimal places.
- **Typing the "wrong" decimal separator**: only the decimal separator from the user's preference is accepted into the amount. The thousands separator is never typed; it is added automatically as the amount grows.
- **Decimal and thousands separators set to the same character**: this combination is not valid. The defaults can never produce it, and the future preferences feature must not allow it.
- **Very long note**: a note can be up to 500 characters long. The expanded row grows to show the whole note. The note is never cut off with an ellipsis.
- **Expanded row and scrolling**: a row stays expanded while it scrolls out of view and back in, until the user leaves the screen. When the user comes back, every row starts collapsed.
- **Expanded row when a new transaction is added**: rows that are already expanded stay expanded. The new row appears collapsed.
- **Very large amounts**: the largest amount allowed is 999,999,999.99. A tenth whole digit is not accepted into the field. Up to that limit, the amount stays fully readable in the form and in list rows, and the row title gives way first.
- **Empty receiver/sender name**: the name is required. *Create* stays disabled while the name is empty or contains only spaces. Leading and trailing spaces are removed before saving.
- **Category picker for a category not on screen**: the category list scrolls to show all seven categories.
- **Opening the repeat list on the 29th–31st of a month**: the monthly label still uses that day ("Monthly on the 31st"). In shorter months the occurrence falls on the last day of that month. The following month goes back to the original day: Jan 31 → Feb 28 → Mar 31 → Apr 30.
- **Yearly repeat created on Feb 29**: in non-leap years it falls on Feb 28, and in leap years it goes back to Feb 29.
- **Midnight rollover while the list is open**: the "TODAY" and "YESTERDAY" headers update to match the new date, and any recurring occurrence due on the new date appears under TODAY right away.
- **No transactions at all**: the list shows no groups. A dedicated empty-state design is not part of this frame set (see Assumptions).
- **Switching Expense ↔ Income after picking a category**: the chosen expense category is kept and comes back if the user switches back to Expense.
- **Closing the sheet with unsaved input**: the input is discarded without asking, and nothing is recorded.
- **Saving fails** (for example, the phone is out of storage): the sheet stays open, everything the user typed is kept, and a short message under *Create* says the transaction was not saved. When storage is full it reads "Not enough storage to save this transaction."; otherwise "Couldn't save the transaction. Please try again." No Figma frame shows this state; the product owner approved the text on 2026-11-16.

## Requirements *(mandatory)*

### Functional Requirements

**Transaction list**

- **FR-001**: The system MUST show all of the user's transactions on one *All Transactions* screen, grouped by calendar day, with the newest day first.
- **FR-002**: Within a day, the system MUST show the most recently recorded transaction first. An automatically added recurring occurrence counts as recorded at the original transaction's time of day on its due date, or at the moment it is added if that is earlier, whether it was added live or caught up later.
- **FR-003**: Day headers MUST read "TODAY" for the current date, "YESTERDAY" for the previous date, and the month name and day number (for example "JANUARY 9") for any older date.
- **FR-004**: Each row MUST show the category icon, the counterparty name (receiver or sender), a secondary label (the category name for expenses, "Income" for incomes), and the amount.
- **FR-005**: In the list, expense amounts MUST be shown as negative in the normal text colour. Income amounts MUST be shown as positive and highlighted as incoming. The two must be easy to tell apart at a glance.
- **FR-006**: A row MUST show a note badge on its icon when the transaction has a note, and MUST NOT show it otherwise. Tapping the badge expands or collapses the note as described in FR-028 to FR-031.
- **FR-007**: Long counterparty names MUST be shortened with an ellipsis so the amount is never cut off.
- **FR-008**: While scrolling, the header of the day group at the top of the visible list MUST stay pinned under the title bar.
- **FR-009**: The screen MUST offer a back action, the title "All Transactions", and a floating add action that opens the create sheet. The export/download icon drawn in the title bar belongs to a separate export feature. This feature MUST NOT show it: the icon stays hidden until the export feature ships (see Assumptions).
- **FR-010**: A newly created transaction MUST appear in the list as soon as the create sheet closes, without the user refreshing.

**Create transaction**

- **FR-011**: The create sheet MUST let the user choose between *Expense* and *Income*. *Expense* is selected by default.
- **FR-012**: The sheet MUST provide a required counterparty name field, with the placeholder *Receiver* for expenses and *Sender* for incomes. A name made up only of spaces counts as empty. The name holds at most 50 characters, and further typing is not accepted.
- **FR-013**: The sheet MUST provide an amount field. It is required, accepts digits and one decimal separator (the one set in the user's preference) with at most two decimal places, and must be greater than 0.00 and at most 999,999,999.99 (nine whole digits).
- **FR-014**: The amount MUST be shown with the currency symbol and sign that match the selected type (negative for expense, positive for income), formatted according to the user's amount-format preferences (FR-032 to FR-035). This includes the placeholder (`00.00` with the preferred decimal separator).
- **FR-015**: The *Create* action MUST stay disabled until both the name is filled in and the amount is valid, and MUST become enabled as soon as both are.
- **FR-016**: The sheet MUST provide an optional *Add Note* action for attaching free-text notes to the transaction. A note holds at most 500 characters, and further typing is not accepted.
- **FR-017**: For expenses, the sheet MUST provide a category field, defaulting to *Other*. Choosing it opens a scrollable list where each category has an icon and a name, and the current choice is ticked.
- **FR-018**: For incomes, the sheet MUST hide the category field. The transaction is recorded as income without a user-chosen category.
- **FR-019**: The sheet MUST provide a repeat field, defaulting to *Does not repeat*, with exactly these options: *Does not repeat*, *Daily*, *Weekly on [weekday]*, *Monthly on the [ordinal day]*, *Yearly on [abbreviated month] [ordinal day]*. The weekday, day and date come from the transaction's date and are always written in English (for example "Monthly on the 22nd").
- **FR-020**: After a repeat option is chosen, the field MUST show the short frequency name (*Does not repeat*, *Daily*, *Weekly*, *Monthly*, *Yearly*).
- **FR-021**: When the sheet opens, the keyboard MUST already be shown and ready for input, with no extra tap.
- **FR-022**: Tapping *Create* with valid input MUST save the transaction dated today, close the sheet, and return the user to the list.
- **FR-023**: Tapping the close (✕) control MUST close the sheet without saving anything.
- **FR-022a**: If saving fails, the sheet MUST stay open with all input kept and show a message under *Create* saying the transaction was not saved (see Edge Cases). Nothing is recorded.
- **FR-024**: Switching between *Expense* and *Income* MUST keep the name, amount, note and repeat values the user has already entered.
- **FR-025**: For a transaction with a repeat other than *Does not repeat*, the system MUST automatically add a new transaction on each later due date. The new transaction copies the type, name, amount, category and note. Occurrences are never created ahead of their due date.
- **FR-025a**: Due dates that passed while the app was closed MUST be caught up the next time the app is opened. Exactly one transaction is added per missed due date, and none is added twice.
- **FR-025b**: The original recurring transaction MUST be the only record that holds the repeat schedule. Each automatically added occurrence is a one-time transaction (its repeat is *Does not repeat*) that stays linked to the original, and it never adds further occurrences itself.
- **FR-025c**: If the app is open when the date changes and a recurring transaction falls due on the new date, its occurrence MUST be added at that moment and appear in the list without the user reopening the app or refreshing.

**Stopping a recurring series**

- **FR-037**: Long-pressing any row that belongs to a running recurring series, whether the original or an occurrence, MUST open a small menu next to that row with exactly one item, *Stop repeating*. Long-pressing any other row MUST do nothing. Rows in a recurring series MUST look the same as one-time rows (no repeat marker), as in Figma.
- **FR-038**: Choosing *Stop repeating* MUST first show a confirmation dialog that names the transaction and offers *Stop* and *Cancel*. Only confirming with *Stop* MUST stop the whole series at once, whichever row was long-pressed. *Cancel*, or dismissing the dialog, changes nothing. Once stopped, no occurrence is added for any due date after the day it was stopped, either live (FR-025c) or in a catch-up (FR-025a).
- **FR-039**: Stopping a series MUST NOT change or remove the original transaction or any occurrence already added. A stopped series cannot be restarted in this feature.
- **FR-040**: The menu MUST close without changes when the user taps outside it.

**Notes in the list**

- **FR-028**: Tapping the note badge on a collapsed row MUST expand that row in place to show the transaction's full note below the name and category, aligned with the name. The note wraps over as many lines as needed and is never cut off.
- **FR-029**: An expanded row MUST look visibly different from collapsed rows: it is raised as a card around the row and its note, and its note badge switches from the subdued look to a stronger, active look. Both looks are shown in frame `2414:5142`, for expenses and for incomes.
- **FR-030**: Tapping the note badge on an expanded row MUST collapse it back to its normal height.
- **FR-031**: Each row MUST expand and collapse on its own, so several rows can be expanded at once. Expanded rows stay expanded while the user scrolls on the screen. Every row starts collapsed when the user opens the screen.

**Amount formatting**

- **FR-032**: Every amount shown or typed in this feature (list rows, the amount field and its placeholder) MUST follow these four amount-format preferences: currency symbol, negative-number style (minus prefix `-$10` or parentheses `($10)`), decimal separator, and thousands separator.
- **FR-033**: This feature MUST read all four preferences from one shared place and MUST NOT let the user edit them. Editing them belongs to a separate preferences feature.
- **FR-034**: Until the preferences feature exists, the shared place MUST hold these defaults: currency symbol `$` placed before the number, negative style minus prefix, decimal separator `.`, thousands separator `,`. Example: `-$1,234.50`.
- **FR-035**: Changing any single preference value in that shared place MUST change how every amount in this feature is shown and typed, with no other change to this feature. This is what will let the future preferences feature plug in.

**Accessibility**

- **FR-036**: Each control that is only an icon (back, add (+), close (✕) and the note badge) MUST have a short spoken label for the phone's screen reader, for example "Back", "Add transaction", "Close", "Show note". The note badge's label says what tapping it will do ("Show note" or "Hide note"). Fuller screen-reader support, such as spoken expense/income type or expanded/collapsed state, is out of scope for this feature.

**Expense categories**

- **FR-026**: The expense categories MUST be exactly these seven, listed in this alphabetical order: *Clothing & Accessories* 👔, *Education* 🎓, *Entertainment* 💻, *Food & Groceries* 🍕, *Health & Wellness* ❤️, *Home* 🏠, *Other* ⚙️. Users cannot add, rename or remove categories.

**Data protection** (from the project constitution)

- **FR-027**: All transaction data (names, amounts, notes, categories, repeat settings) MUST stay on the device, be protected at rest, and never appear in diagnostic output.

### Key Entities

- **Transaction**: a single record of money moving in or out. Attributes: type (expense or income), counterparty name (receiver for expense, sender for income), amount (positive value; the sign comes from the type), optional note, category (expenses only), repeat frequency, and the date and time it was recorded. An automatically added occurrence also records which original recurring transaction it came from. Its own repeat frequency is always *Does not repeat*.
- **Category**: a fixed label for an expense, with an emoji icon and a display name (for example 🍕 Food & Groceries). Incomes do not use categories and are shown as "Income".
- **Repeat frequency**: one of Does not repeat, Daily, Weekly, Monthly, Yearly. It is anchored to the transaction's date (which weekday, which day of the month, or which day of the year). Each due date is worked out from that original anchor, not from the previous occurrence. A recurring series is either running or stopped. When stopped, it records the day it was stopped, and no due date after that day produces an occurrence. When the anchor day doesn't exist in a month, the occurrence falls on that month's last day.
- **Amount format preferences**: four values that set how every amount is shown and typed: currency symbol, negative-number style (`-$10` or `($10)`), decimal separator, and thousands separator. They are kept in one shared place. This feature reads them but does not edit them. Until the preferences feature exists, they hold the defaults `$`, minus prefix, `.`, `,`.
- **Note display state**: per row and per visit to the screen, whether the note is expanded or collapsed. It is not saved; every row starts collapsed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can record a one-time expense with a name and amount (keeping the default category and repeat) in under 15 seconds from tapping the add button.
- **SC-002**: 95% of first-time users create a valid transaction on their first try, without hitting a disabled *Create* button they cannot explain.
- **SC-003**: In a moderated test, users tell expenses from incomes correctly for 100% of rows on the list screen, at a glance.
- **SC-004**: A newly created transaction is visible in the list within 1 second of tapping *Create*.
- **SC-005**: The transaction list scrolls smoothly: at least 95% of frames are drawn on time while flinging through at least 1,000 transactions spread over 12 months, on a mid-range phone.
- **SC-006**: Every amount shown in the list and the create sheet matches the user's amount-format preference in 100% of checked cases.
- **SC-007**: No transaction data is lost across app restarts. Every created transaction is still listed after the app is closed and reopened.
- **SC-008**: For recurring transactions, 100% of due occurrences, including those missed while the app was closed, appear in the list exactly once, on their correct day, the next time the user opens the list.
- **SC-009**: A user can read any transaction's note with one tap from the list, and hide it again with one more tap, without leaving the screen.
- **SC-010**: Changing any one of the four default preference values changes 100% of the amounts shown and typed in this feature.

## Assumptions

- **Entry point**: users reach *All Transactions* from another screen (such as a dashboard) that is not part of this feature. The back action returns there.
- **Export is a separate feature**: the download/export icon is drawn in the title bar in Figma, but what it exports and in which format are left to the separate export spec. Until that feature ships, the icon is hidden, so the app has no do-nothing control. This is the one deliberate deviation from the *All Transactions* frames, and UI checks against Figma should expect it (clarification on 2026-11-13).
- **Amount-format preferences come from elsewhere**: a separate preferences feature owns the currency symbol, the negative-number style, the decimal separator and the thousands separator. Building that feature is out of scope here. This feature follows the four values from one shared place, which holds fixed defaults (`$`, minus prefix, `.`, `,`) until that feature exists. The defaults match the primary mockup. Currency conversion is out of scope: the symbol is only shown, never used to convert amounts.
- **Transaction date is always "now"**: the mockups have no date picker, so back-dating or future-dating is out of scope.
- **Editing and deleting transactions are out of scope**: no frame in this set shows them. Stopping a recurring series (User Story 6) is the one exception, added by the product owner. It stops future occurrences only, and deletes nothing.
- **Stop menu and confirmation are not in Figma**: the long-press menu, its *Stop repeating* item and the confirmation dialog come from the product owner (clarification on 2026-11-13), not from a mockup. The text was chosen to match the existing *Does not repeat* option. The design system has no menu or dialog component. On 2026-11-13 the product owner decided that, until Figma provides them, the menu takes the look of the existing selection list (the same surface as the category and repeat lists) and the dialog is built from the existing surfaces, text styles and text buttons. Both are swapped for real designs once Figma provides them.
- **Entering a note has no mockup**: no frame shows the note being typed. On 2026-11-13 the product owner decided that tapping *Add Note* turns that line into a centred note field in the same look as the *Add Note* line itself, growing over as many lines as needed. If the field is left empty, it returns to the *Add Note* line.
- **Viewing a note**: notes are read by expanding the row in the list (User Story 3). Editing or deleting a note after the transaction is created is out of scope.
- **What toggles the note**: the user named the note badge as the control, so only tapping the badge expands or collapses a row. Tapping anywhere else on the row does nothing.
- **Hidden date in the row-state frame**: frame `2414:5142` contains a hidden date text (`12.01.2025`) next to the amount. Because it is hidden in Figma, it is not shown. The day group header already gives the date.
- **Keyboard focus**: the mockups show the numeric keyboard on open, so the amount field is assumed to have focus when the sheet opens. Tapping the name or the note moves focus there.
- **Empty list state**: no empty-state design is in this frame set, so an empty history shows an empty list under the title bar with the add button available. A dedicated empty-state design can be added later from Figma.
- **Dev note vs. mockup**: the empty-form mockups draw *Create* in its normal colour, but the designer note says it is disabled until an amount is entered. The note is treated as the rule, and the disabled state uses the design system's standard disabled look. The product owner also made the name required (clarification on 2026-11-13), which goes beyond the note.
- **The list behind the create sheet is outdated**: the list drawn behind the create sheet frames is an older version ("8 JAN" / "31 DEC" headers, notes as row subtitles). The dedicated *All Transactions* frames (`2417:12042`, `2417:12068`) are the authoritative list design.
- **English only**: the mockups are English only, so all text in this feature is English whatever the phone's language. This covers day headers, month and weekday names, English ordinals ("14th"), repeat labels and category names. Translations are out of scope until Figma provides them (clarification on 2026-11-13).
- **Single user, single device, no backend**: SpendLess is offline-first, and all data lives on the user's device.
- **Portrait phone only**: per the project constitution, only the portrait phone layout is specified.
