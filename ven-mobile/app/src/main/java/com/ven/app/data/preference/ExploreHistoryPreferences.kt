package com.ven.app.data.preference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

interface ExploreHistoryStore {
    fun load(): List<String>
    fun save(queries: List<String>)
}

class ExploreHistoryPreferences private constructor(context: Context) : ExploreHistoryStore {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<String>>() {}.type

    override fun load(): List<String> {
        val json = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            gson.fromJson<List<String>>(json, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun save(queries: List<String>) {
        try {
            preferences.edit().putString(KEY_HISTORY, gson.toJson(queries, listType)).apply()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val PREF_NAME = "ven_explore_pref"
        private const val KEY_HISTORY = "key_explore_history"

        @Volatile
        private var INSTANCE: ExploreHistoryPreferences? = null

        fun getInstance(context: Context): ExploreHistoryPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExploreHistoryPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
