package dev.mokksy.mokksy.request

import dev.mokksy.mokksy.JournalMode
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Records [RecordedRequest] snapshots for post-test verification,
 * differentiating between requests matched by a stub and those that were not.
 *
 * Recording behaviour is controlled by [JournalMode]:
 * - [dev.mokksy.mokksy.JournalMode.NONE]: request recording is disabled entirely.
 * - [dev.mokksy.mokksy.JournalMode.LEAN]: only unmatched requests are recorded (lower overhead).
 * - [dev.mokksy.mokksy.JournalMode.FULL]: all requests are recorded, matched and unmatched alike.
 *
 * All operations are thread-safe and lock-free.
 * @author Konstantin Pavlov
 */
internal class RequestJournal(
    private val mode: JournalMode = JournalMode.LEAN,
) {
    /** Whether this journal records matched requests (i.e. [JournalMode.FULL]). */
    internal val recordsMatched: Boolean = (mode == JournalMode.FULL)
    internal val recordsUnmatched: Boolean = (mode != JournalMode.NONE)

    private val matched: AtomicRef<PersistentList<RecordedRequest>> = atomic(persistentListOf())
    private val unmatched: AtomicRef<PersistentList<RecordedRequest>> = atomic(persistentListOf())

    /**
     * Records a request that was successfully matched by a stub.
     * No-op in [JournalMode.LEAN] and [JournalMode.NONE].
     */
    fun recordMatched(request: RecordedRequest) {
        if (mode == JournalMode.FULL) {
            matched.update { it.add(request) }
        }
    }

    /**
     * Records a request for which no stub was found.
     * No-op in [JournalMode.NONE].
     */
    fun recordUnmatched(request: RecordedRequest) {
        if (mode != JournalMode.NONE) {
            unmatched.update { it.add(request) }
        }
    }

    /**
     * Returns a consistent snapshot of all requests matched by a stub.
     * Always empty in [JournalMode.LEAN] and [JournalMode.NONE].
     */
    fun getMatched(): List<RecordedRequest> = matched.value

    /**
     * Returns a consistent snapshot of all requests that had no matching stub.
     * Always empty in [JournalMode.NONE].
     */
    fun getUnmatched(): List<RecordedRequest> = unmatched.value

    /** Clears both matched and unmatched request histories. */
    fun clear() {
        matched.value = persistentListOf()
        unmatched.value = persistentListOf()
    }
}
