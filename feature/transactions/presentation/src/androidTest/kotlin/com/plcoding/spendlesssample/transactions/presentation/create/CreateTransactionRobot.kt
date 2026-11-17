package com.plcoding.spendlesssample.transactions.presentation.create

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isLessThan
import com.plcoding.spendlesssample.transactions.presentation.R

/**
 * Drives the create flow from the All Transactions screen. The sheet's name and amount fields
 * show no text of their own while empty, so they are found as the first and second editable
 * nodes, in on-screen order.
 */
@OptIn(ExperimentalTestApi::class)
class CreateTransactionRobot(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>,
) {

    private fun string(@StringRes id: Int) = rule.activity.getString(id)

    private fun editable(index: Int): SemanticsNodeInteraction =
        rule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[index]

    private val nameField get() = editable(0)
    private val amountField get() = editable(1)
    private val createButton get() = rule.onNodeWithText(string(R.string.create))

    fun openSheet() = apply {
        rule.onNodeWithContentDescription(string(R.string.cd_add_transaction)).performClick()
        rule.waitUntilExactlyOneExists(hasText(string(R.string.create_transaction)))
    }

    fun assertDefaults() = apply {
        rule.onNodeWithText(string(R.string.expense)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.receiver)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.category_other)).assertIsDisplayed()
        rule.onNodeWithText("-$").assertIsDisplayed()
        rule.onNodeWithText("00.00").assertIsDisplayed()
    }

    fun assertAmountFocused() = apply {
        rule.waitUntil(timeoutMillis = 2_000) {
            runCatching { amountField.assertIsFocused() }.isSuccess
        }
    }

    /** FR-021: the soft keyboard is up without any extra tap. The sheet is its own window. */
    fun assertKeyboardShown() = apply {
        rule.waitUntil(timeoutMillis = 3_000) { isImeShown() }
    }

    fun enterName(name: String) = apply { nameField.performTextInput(name) }

    fun enterAmount(amount: String) = apply { amountField.performTextInput(amount) }

    fun pickCategory(@StringRes category: Int) = apply {
        rule.onNodeWithText(string(R.string.category_other)).performClick()
        rule.waitUntilExactlyOneExists(hasText(string(category)))
        rule.onNodeWithText(string(category)).performClick()
        rule.waitForIdle()
    }

    fun addNote(note: String) = apply {
        rule.onNodeWithText(string(R.string.add_note)).performClick()
        rule.waitForIdle()
        // The note field is the third editable node once it replaces the "Add Note" row.
        editable(2).performTextInput(note)
    }

    fun assertCreateDisabled() = apply { createButton.assertIsNotEnabled() }

    fun assertCreateEnabled() = apply { createButton.assertIsEnabled() }

    fun tapCreate() = apply { createButton.performClick() }

    fun tapClose() = apply {
        rule.onNodeWithContentDescription(string(R.string.cd_close)).performClick()
    }

    fun assertSheetClosed() = apply {
        rule.waitUntilDoesNotExist(hasText(string(R.string.create_transaction)), timeoutMillis = 2_000)
    }

    /** SC-004: the new row is visible within one second of tapping Create. */
    fun assertRowAppearsWithin1s(name: String) = apply {
        rule.waitUntil(timeoutMillis = 1_000) {
            rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun assertRowUnderToday(name: String, label: String, amount: String) = apply {
        rule.onNodeWithText(name).assertIsDisplayed()
        rule.onNodeWithText(label).assertIsDisplayed()
        rule.onNodeWithText(amount).assertIsDisplayed()
        assertThat(top(string(R.string.today))).isLessThan(top(name))
    }

    private fun top(text: String) = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    private fun isImeShown(): Boolean =
        shell("dumpsys input_method").lineSequence().any { it.trim().startsWith("mInputShown=true") }

    companion object {
        fun shell(command: String): String {
            val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        }
    }
}
