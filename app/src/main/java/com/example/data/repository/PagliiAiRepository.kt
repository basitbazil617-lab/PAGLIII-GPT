package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.model.AiToolType
import com.example.data.model.AssistantLanguage
import com.example.data.model.HomeworkSubject
import com.example.data.model.LocalChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class PagliiAiRepository(private val appContext: Context) {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.75f
            }
        )
    }

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.4f
            }
        )
    }

    private fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildPersonaInstruction(language: AssistantLanguage): String {
        val languageDirective = when (language) {
            AssistantLanguage.HINGLISH ->
                "Respond primarily in warm, natural Hinglish (Hindi written in Roman script mixed smoothly with English), unless the user explicitly writes in pure English, Devanagari Hindi, or Urdu—always mirror the user's preferred script seamlessly."
            AssistantLanguage.ENGLISH ->
                "Respond in clear, engaging English, while still understanding Hindi, Urdu, and Hinglish inputs effortlessly."
            AssistantLanguage.HINDI ->
                "Respond primarily in clear, natural Hindi (Devanagari script), while understanding Hinglish, Urdu, and English effortlessly."
            AssistantLanguage.URDU ->
                "Respond primarily in polite, natural Urdu (Urdu script), while understanding Roman Urdu, Hinglish, Hindi, and English effortlessly."
        }
        return """
            You are PAGLII GPT, a friendly, witty, warm, and super helpful AI assistant created by SAHIB (tagline: "YOUR FRIENDLY AI ASSISTANT ❤️").
            Personality: Cheerful, encouraging, lightly humorous like a smart best friend, never rude, always accurate and crystal-clear.
            Language rule: $languageDirective
            Formatting: Use clean headings, bullet points, and clear step-by-step explanations where helpful. Keep code inside fenced code blocks.
        """.trimIndent()
    }

    fun streamChatReply(
        history: List<LocalChatMessage>,
        userPrompt: String,
        attachedImage: Bitmap?,
        language: AssistantLanguage
    ): Flow<String> = flow {
        if (!isOnline()) {
            throw IllegalStateException("No internet connection. Please connect to Wi-Fi or mobile data and try again.")
        }

        val persona = buildPersonaInstruction(language)
        val chatHistory = buildList {
            add(content("user") { text("System persona instruction:\n$persona") })
            add(content("model") { text("Got it! I'm PAGLII GPT, your friendly AI assistant created by SAHIB ❤️ Ready to help!") })
            history.takeLast(14).forEach { msg ->
                if (!msg.isError && msg.text.isNotBlank()) {
                    add(content(msg.role) { text(msg.text) })
                }
            }
        }

        val chat = chatModel.startChat(history = chatHistory)
        val outgoingContent = content("user") {
            if (attachedImage != null) {
                image(attachedImage)
            }
            text(
                userPrompt.ifBlank {
                    if (attachedImage != null) "Please analyze this image and help me understand it."
                    else "Hello!"
                }
            )
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(outgoingContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Paglii GPT returned an empty response. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat generation timed out", e)
            throw IllegalStateException("Response timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat generation failed", e)
            throw e
        }
    }

    fun streamVisionAnalysis(
        image: Bitmap,
        prompt: String,
        previousTurns: List<LocalChatMessage>,
        language: AssistantLanguage
    ): Flow<String> = flow {
        if (!isOnline()) {
            throw IllegalStateException("No internet connection. Photo AI needs internet to analyze images.")
        }

        val persona = buildPersonaInstruction(language)
        val historyContext = if (previousTurns.isEmpty()) {
            ""
        } else {
            val recent = previousTurns.takeLast(6).joinToString("\n") {
                val speaker = if (it.role == "user") "User" else "PAGLII GPT"
                "$speaker: ${it.text}"
            }
            "\n\nPrevious conversation about this same image:\n$recent\n"
        }

        val fullPrompt = """
            $persona
            You are analyzing an uploaded photo with expert vision capabilities (printed text, handwritten notes, textbook pages, worksheets, diagrams, charts, and objects).
            $historyContext
            User's current request about the image:
            $prompt
        """.trimIndent()

        val requestContent = content {
            image(image)
            text(fullPrompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                visionModel.generateContentStream(requestContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Could not extract an answer for this photo. Please try a clearer image or prompt.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vision generation timed out", e)
            throw IllegalStateException("Photo analysis timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vision generation failed", e)
            throw e
        }
    }

    fun streamHomeworkSolution(
        subject: HomeworkSubject,
        questionText: String,
        image: Bitmap?,
        preferShortAnswer: Boolean,
        language: AssistantLanguage
    ): Flow<String> = flow {
        if (!isOnline()) {
            throw IllegalStateException("No internet connection. Homework Solver requires an active connection.")
        }

        val persona = buildPersonaInstruction(language)
        val lengthGuidance = if (preferShortAnswer) {
            "Format: Keep the final answer concise and exam-ready (short answer style), while still keeping the logic easy to follow."
        } else {
            "Format: Give an easy-to-understand, step-by-step detailed explanation with clear formulas, reasoning, and a quick final summary."
        }

        val taskPrompt = """
            $persona
            Mode: ${subject.label}
            Instruction: ${subject.promptInstruction}
            $lengthGuidance
            
            Student's Question / Notes:
            ${questionText.ifBlank { "Please solve and explain everything shown in the uploaded homework photo." }}
        """.trimIndent()

        val requestContent = content {
            if (image != null) image(image)
            text(taskPrompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                visionModel.generateContentStream(requestContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Homework Solver returned an empty response. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Homework generation timed out", e)
            throw IllegalStateException("Homework solution timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Homework generation failed", e)
            throw e
        }
    }

    fun streamToolGeneration(
        tool: AiToolType,
        topic: String,
        extraContext: String,
        language: AssistantLanguage
    ): Flow<String> = flow {
        if (!isOnline()) {
            throw IllegalStateException("No internet connection. Please check your network and try again.")
        }

        val persona = buildPersonaInstruction(language)
        val prompt = """
            $persona
            Active Tool: ${tool.title}
            Specialist Role: ${tool.systemPrompt}
            
            User Request:
            $topic
            ${if (extraContext.isNotBlank()) "\nAdditional Preferences / Context:\n$extraContext" else ""}
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Tool returned an empty response. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Tool generation timed out", e)
            throw IllegalStateException("Generation timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tool generation failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "PagliiAiRepo"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val AI_TIMEOUT_MS = 60_000L
    }
}
