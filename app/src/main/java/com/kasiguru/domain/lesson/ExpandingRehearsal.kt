package com.kasiguru.domain.lesson

/**
 * Where a missed exercise goes back into the queue.
 *
 * Lessons used to append every miss to the very end of the run, which put the retry at whichever
 * distance the lesson happened to be long. Both extremes are bad: answered while the correction is
 * still on screen it is copying rather than remembering, and answered twenty items later it is
 * usually just forgotten again with nothing gained.
 *
 * The gap therefore starts short and widens on each further miss — the same expanding-interval idea
 * SM-2 applies across days, applied inside one sitting, which is where the evidence for spaced
 * retrieval is strongest.
 */
object ExpandingRehearsal {

    /** Items to let pass before a missed exercise returns, by how many times it has been missed. */
    val GAPS = listOf(2, 5, 10)

    /**
     * The gap after [missCount] misses.
     *
     * A word missed four or five times keeps the widest gap rather than being pushed further out
     * each time: past a point, deferring it further only means the lesson ends with it unlearned.
     */
    fun gapFor(missCount: Int): Int = GAPS[(missCount - 1).coerceIn(0, GAPS.lastIndex)]

    /**
     * Index to reinsert at, given where the learner is now and how long the queue is.
     *
     * Clamped to the end of the queue, so a miss near the finish still comes back — the lesson can
     * only end once every item has been answered correctly, and an index past the end would drop it.
     */
    fun insertIndex(currentPosition: Int, missCount: Int, queueSize: Int): Int =
        (currentPosition + gapFor(missCount)).coerceIn(0, queueSize)
}
