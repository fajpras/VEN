package com.ven.app.data.preference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ven.app.ui.screens.ai.ChatConversation

interface AiChatHistoryStore {
    fun loadHistory(userId: String = "guest"): List<ChatConversation>
    fun saveHistory(userId: String = "guest", history: List<ChatConversation>)
    fun deleteConversation(userId: String = "guest", chatId: String)
}

class AiChatHistoryPreferences private constructor(context: Context) : AiChatHistoryStore {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<ChatConversation>>() {}.type

    override fun loadHistory(userId: String): List<ChatConversation> {
        val key = "${KEY_CHAT_HISTORY}_$userId"
        val json = preferences.getString(key, null) ?: return emptyList()
        return try {
            gson.fromJson<List<ChatConversation>>(json, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun saveHistory(userId: String, history: List<ChatConversation>) {
        try {
            val key = "${KEY_CHAT_HISTORY}_$userId"
            val json = gson.toJson(history, listType)
            preferences.edit().putString(key, json).apply()
        } catch (_: Exception) {
        }
    }

    override fun deleteConversation(userId: String, chatId: String) {
        val key = "${KEY_CHAT_HISTORY}_$userId"
        val remaining = loadHistory(userId).filterNot { it.id == chatId }
        try {
            preferences.edit().apply {
                if (remaining.isEmpty()) {
                    remove(key)
                } else {
                    putString(key, gson.toJson(remaining, listType))
                }
            }.commit()
        } catch (_: Exception) {
        }
    }

    fun clearHistory(userId: String = "guest") {
        val key = "${KEY_CHAT_HISTORY}_$userId"
        preferences.edit().remove(key).apply()
    }

    companion object {
        private const val PREF_NAME = "ven_ai_chat_pref"
        private const val KEY_CHAT_HISTORY = "key_ai_chat_history"

        @Volatile
        private var INSTANCE: AiChatHistoryPreferences? = null

        fun getInstance(context: Context): AiChatHistoryPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiChatHistoryPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
