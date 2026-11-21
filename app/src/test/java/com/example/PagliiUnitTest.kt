package com.example

import com.example.data.model.AiToolType
import com.example.data.model.AssistantLanguage
import com.example.data.model.ChatCategory
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.HomeworkSubject
import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagliiUnitTest {

    @Test
    fun `assistant languages parse safely with Hinglish fallback`() {
        assertEquals(AssistantLanguage.HINDI, AssistantLanguage.fromLabel("Hindi"))
        assertEquals(AssistantLanguage.URDU, AssistantLanguage.fromLabel("urdu"))
        assertEquals(AssistantLanguage.ENGLISH, AssistantLanguage.fromLabel("ENGLISH"))
        assertEquals(AssistantLanguage.HINGLISH, AssistantLanguage.fromLabel(null))
        assertEquals(AssistantLanguage.HINGLISH, AssistantLanguage.fromLabel("unknown"))
    }

    @Test
    fun `all 10 homework modes and 8 AI tools are configured`() {
        assertEquals(10, HomeworkSubject.entries.size)
        assertTrue(HomeworkSubject.SCAN_HOMEWORK.triggersScan)
        assertFalse(HomeworkSubject.SOLVE_MATHS.triggersScan)

        assertEquals(8, AiToolType.entries.size)
        assertTrue(AiToolType.entries.all { it.title.isNotBlank() && it.systemPrompt.isNotBlank() })
    }

    @Test
    fun `firestore data models expose safe no-arg defaults`() {
        val profile = UserProfile()
        assertEquals("", profile.id)
        assertEquals("Hinglish", profile.preferredLanguage)

        val session = ChatSession()
        assertEquals("chat", session.category)
        assertEquals(0L, session.messageCount)

        val message = ChatMessage()
        assertEquals("user", message.role)
        assertFalse(message.hasImage)

        assertEquals(5, ChatCategory.entries.size)
    }
}
