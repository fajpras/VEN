package com.ven.app.ui.screens.ai

import com.ven.app.data.preference.AiChatHistoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AiChatViewModelTest {

    private class FakeHistoryStore(initial: List<ChatConversation>) : AiChatHistoryStore {
        var history: List<ChatConversation> = initial
        val deletedIds = mutableListOf<String>()

        override fun loadHistory(userId: String): List<ChatConversation> = history

        override fun saveHistory(userId: String, history: List<ChatConversation>) {
            this.history = history
        }

        override fun deleteConversation(userId: String, chatId: String) {
            deletedIds += chatId
            history = history.filterNot { it.id == chatId }
        }
    }

    private fun conversation(id: String, title: String): ChatConversation {
        return ChatConversation(
            id = id,
            title = title,
            messages = listOf(
                ChatMessage(
                    id = "$id-user",
                    sender = ChatSender.User,
                    text = title,
                ),
            ),
        )
    }

    @Test
    fun onDeleteConversation_nonActiveChat_removesOnlyThatChat() {
        val first = conversation("chat-1", "Rekomendasi karya")
        val second = conversation("chat-2", "Pameran 3D")
        val store = FakeHistoryStore(listOf(first, second))
        val viewModel = AiChatViewModel(historyPrefs = store)

        viewModel.onSelectConversation(second)
        viewModel.onDeleteConversation(first.id)

        val state = viewModel.uiState.value
        assertEquals(listOf("chat-2"), state.chatHistory.map { it.id })
        assertEquals("chat-2", state.currentChatId)
        assertEquals(second.messages, state.messages)
        assertEquals(listOf("chat-1"), store.deletedIds)
    }

    @Test
    fun onDeleteConversation_activeChat_resetsToEmptyState() {
        val first = conversation("chat-1", "Rekomendasi karya")
        val second = conversation("chat-2", "Pameran 3D")
        val store = FakeHistoryStore(listOf(first, second))
        val viewModel = AiChatViewModel(historyPrefs = store)

        viewModel.onSelectConversation(first)
        viewModel.onInputChange("draft pertanyaan")
        viewModel.onDeleteConversation(first.id)

        val state = viewModel.uiState.value
        assertEquals(listOf("chat-2"), state.chatHistory.map { it.id })
        assertNull(state.currentChatId)
        assertEquals(emptyList<ChatMessage>(), state.messages)
        assertEquals("", state.inputText)
        assertFalse(state.isAiTyping)
        assertEquals(listOf("chat-1"), store.deletedIds)
    }

    @Test
    fun onDeleteConversation_unknownChat_keepsActiveConversation() {
        val first = conversation("chat-1", "Rekomendasi karya")
        val second = conversation("chat-2", "Pameran 3D")
        val store = FakeHistoryStore(listOf(first, second))
        val viewModel = AiChatViewModel(historyPrefs = store)

        viewModel.onSelectConversation(first)
        viewModel.onDeleteConversation("chat-missing")

        val state = viewModel.uiState.value
        assertEquals(listOf("chat-1", "chat-2"), state.chatHistory.map { it.id })
        assertEquals("chat-1", state.currentChatId)
        assertEquals(first.messages, state.messages)
    }
}
