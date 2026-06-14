package com.shumidub.todoapprealm.data

import android.content.Context

/**
 * Persisted, user-editable titles for the four top-level tabs (groups 0..3).
 *
 * Stored in SharedPreferences (not Realm) so the locked schema v5 stays untouched. Falls back
 * to the built-in defaults when a tab has never been renamed.
 */
object TabNames {
    private const val PREFS = "tab_names"
    val DEFAULTS = listOf("Tasks 1", "Tasks 2", "Tasks 3", "Notes")

    fun load(context: Context): List<String> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return DEFAULTS.mapIndexed { i, d -> p.getString("tab_$i", d)?.ifBlank { d } ?: d }
    }

    fun save(context: Context, group: Int, name: String) {
        if (group !in DEFAULTS.indices) return
        val n = name.trim().ifBlank { DEFAULTS[group] }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("tab_$group", n).apply()
    }
}
