package com.example.data.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val preferredLanguage: String = "Hinglish",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "chat",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val hasImage: Boolean = false,
    val toolTag: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class LocalChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val hasImage: Boolean = false,
    val attachedBitmap: Bitmap? = null,
    val toolTag: String? = null,
    val isError: Boolean = false
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AppThemeMode(val label: String) {
    DARK("Dark Neon"),
    LIGHT("Light Lavender"),
    SYSTEM("System Default")
}

enum class AssistantLanguage(
    val label: String,
    val nativeHint: String,
    val localeTag: String
) {
    HINGLISH("Hinglish", "Hindi + English mix", "en-IN"),
    ENGLISH("English", "Global English", "en-US"),
    HINDI("Hindi", "हिंदी (Devanagari)", "hi-IN"),
    URDU("Urdu", "اردو (Nastaliq/Script)", "ur-PK");

    companion object {
        fun fromLabel(raw: String?): AssistantLanguage =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: HINGLISH
    }
}

enum class ChatCategory(val key: String, val label: String) {
    ALL("all", "All"),
    CHAT("chat", "Chats"),
    HOMEWORK("homework", "Homework"),
    VISION("vision", "Photo AI"),
    TOOLS("tools", "AI Tools")
}

enum class HomeworkSubject(
    val label: String,
    val subtitle: String,
    val promptInstruction: String,
    val triggersScan: Boolean = false
) {
    SCAN_HOMEWORK(
        label = "Scan Homework",
        subtitle = "Snap or pick a worksheet photo",
        promptInstruction = "Scan this homework photo, read every question clearly, and provide clean step-by-step solutions.",
        triggersScan = true
    ),
    SOLVE_MATHS(
        label = "Solve Maths",
        subtitle = "Algebra, geometry, arithmetic",
        promptInstruction = "Solve this mathematics problem step by step. Show formulas, intermediate calculations, and highlight the final answer."
    ),
    SST_QA(
        label = "SST Question Answers",
        subtitle = "History, civics, geography",
        promptInstruction = "Answer this Social Studies (History / Geography / Civics) question clearly with key dates, facts, and bullet points."
    ),
    SCIENCE_QA(
        label = "Science Question Answers",
        subtitle = "Physics, chemistry, biology",
        promptInstruction = "Explain and answer this Science question with clear concepts, definitions, formulas/equations, and real-life examples."
    ),
    ENGLISH_HOMEWORK(
        label = "English Homework",
        subtitle = "Grammar, essays, comprehension",
        promptInstruction = "Help with this English homework task (grammar, comprehension, essay, or literature) with crystal-clear explanations."
    ),
    URDU_HOMEWORK(
        label = "Urdu Homework",
        subtitle = "Nazm, ghazal, qawaid, mazmoon",
        promptInstruction = "Help with this Urdu homework in polite, accurate Urdu (and explain difficult words clearly so any student can understand)."
    ),
    EXPLAIN_PAGE(
        label = "Explain This Page",
        subtitle = "Break down textbook concepts",
        promptInstruction = "Explain the textbook page or passage in simple, friendly language with a quick summary and key takeaways."
    ),
    GENERATE_QA(
        label = "Generate Questions & Answers",
        subtitle = "Practice quiz & exam prep",
        promptInstruction = "Generate important practice Questions & Answers (MCQs, short Q&A, and long Q&A) based on this topic or page."
    ),
    SHORT_ANSWERS(
        label = "Short Answers",
        subtitle = "Crisp 2-3 line exam answers",
        promptInstruction = "Give a concise, exam-ready short answer (2 to 4 crisp sentences or bullet points)."
    ),
    DETAILED_ANSWERS(
        label = "Detailed Answers",
        subtitle = "Full step-by-step breakdown",
        promptInstruction = "Provide a complete, detailed step-by-step explanation with headings, examples, and a quick recap."
    )
}

enum class AiToolType(
    val title: String,
    val description: String,
    val inputHint: String,
    val systemPrompt: String
) {
    WRITING(
        title = "AI Writing Assistant",
        description = "Essays, emails, captions, speeches & stories",
        inputHint = "e.g., Write a heartfelt birthday message or formal leave email...",
        systemPrompt = "You are an expert creative & professional writer. Craft polished, engaging text tailored to the user's tone and language."
    ),
    CODE(
        title = "Code Generator",
        description = "Kotlin, Python, JS, C++, SQL & bug fixing",
        inputHint = "e.g., Write a Kotlin coroutine retry helper or debug my Python loop...",
        systemPrompt = "You are a senior software engineer. Provide clean, production-ready, well-commented code and explain how it works."
    ),
    WEBSITE(
        title = "Website Creator Helper",
        description = "HTML/CSS, Tailwind, React layouts & copy",
        inputHint = "e.g., Build a modern neon portfolio landing page for a developer...",
        systemPrompt = "You are a modern web architect & UI designer. Generate semantic responsive HTML/CSS/JS or React components plus helpful UX copy."
    ),
    APP_DEV(
        title = "App Dev Assistant",
        description = "Android, Jetpack Compose, architecture & Play tips",
        inputHint = "e.g., Design a Jetpack Compose bottom sheet with Material 3...",
        systemPrompt = "You are a senior Android & mobile engineer specializing in Kotlin, Jetpack Compose, and Material 3. Give idiomatic guidance and code."
    ),
    YOUTUBE(
        title = "YouTube Title & Description",
        description = "Catchy titles, SEO descriptions, hashtags & hooks",
        inputHint = "e.g., Video about top 5 AI study hacks for students in 2026...",
        systemPrompt = "You are a viral YouTube strategist. Generate 5 high-CTR titles, an engaging SEO description with timestamps template, and relevant tags."
    ),
    GAMING(
        title = "Gaming Content Assistant",
        description = "Stream titles, montage ideas, BGMI/Valorant tips",
        inputHint = "e.g., Give me funny squad callouts and stream title for BGMI clutch...",
        systemPrompt = "You are an energetic gaming creator & strategist. Give hype stream titles, commentary hooks, thumbnail ideas, or gameplay strategy."
    ),
    TRANSLATOR(
        title = "Translation Tool",
        description = "Translate English, Hindi, Urdu, Hinglish & more",
        inputHint = "e.g., Translate 'You are doing amazing!' into natural Urdu & Hindi...",
        systemPrompt = "You are an accurate multilingual translator fluent in English, Hindi, Urdu, Hinglish, and world languages. Show script + easy pronunciation."
    ),
    IDEAS(
        title = "Creative Ideas Generator",
        description = "Reels, startups, college projects & gifts",
        inputHint = "e.g., 10 unique final-year AI app project ideas for college...",
        systemPrompt = "You are a creative brainstorm partner. Generate original, practical, inspiring ideas grouped clearly with next steps."
    )
}
