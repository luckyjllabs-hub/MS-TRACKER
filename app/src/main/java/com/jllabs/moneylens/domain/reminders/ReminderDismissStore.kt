package com.jllabs.moneylens.domain.reminders

import android.content.Context

/**
 * Persists reminders the user marked Done so they stay hidden across launches.
 */
object ReminderDismissStore {
    private const val PREFS = "moneylens_reminder_dismissed"
    private const val KEY_IDS = "ids"

    fun dismissedIds(context: Context): Set<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun isDismissed(context: Context, id: String): Boolean =
        id.isNotBlank() && id in dismissedIds(context)

    fun markDone(context: Context, id: String) {
        if (id.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = dismissedIds(context).toMutableSet().apply { add(id) }
        prefs.edit().putStringSet(KEY_IDS, next).apply()
    }
}
