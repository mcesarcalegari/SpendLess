package com.plcoding.spendlesssample

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.plcoding.spendlesssample.core.domain.time.CurrentDateProvider
import com.plcoding.spendlesssample.transactions.domain.usecase.MaterializeDueOccurrences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adds due recurring occurrences while the app is in the foreground (research R10). While the
 * lifecycle is at least STARTED it collects today's date and runs [MaterializeDueOccurrences] for
 * each emission: the first emission after every start catches up missed due dates (FR-025a), and
 * a date change while open adds that day's occurrences at once (FR-025c). The use case is
 * idempotent, so a repeated date adds nothing. Results are not logged (AGENTS §4.5); a failed run
 * is retried on the next start or date change.
 *
 * Nothing runs while the app is in the background; no WorkManager is needed.
 */
class RecurringOccurrencesTrigger(
    private val currentDateProvider: CurrentDateProvider,
    private val materializeDueOccurrences: MaterializeDueOccurrences,
    private val scope: CoroutineScope,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    /** Starts following [lifecycle] (the process lifecycle in the app) until [scope] ends. */
    fun start(lifecycle: Lifecycle): Job = scope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // collect, not collectLatest: a run in progress is never cancelled by the next date.
            currentDateProvider.today.collect { today ->
                // repeatOnLifecycle runs its block on the main thread; the work does not.
                withContext(workDispatcher) { materializeDueOccurrences(today) }
            }
        }
    }
}
