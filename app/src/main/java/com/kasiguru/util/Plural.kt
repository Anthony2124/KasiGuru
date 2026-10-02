package com.kasiguru.util

/**
 * A count with its noun: "1 day", "3 days", "0 stars".
 *
 * The number and the word are built together so they can never disagree. Every place that wrote
 * `"$n days"` by hand read "1 days" the first time a learner had exactly one.
 *
 * @param many the plural, for nouns that do not just take an s ("story" → "stories").
 */
fun pluralize(count: Int, one: String, many: String = one + "s"): String =
    "$count ${if (count == 1) one else many}"
