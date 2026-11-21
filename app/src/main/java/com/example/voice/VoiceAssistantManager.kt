package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AssistantLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class VoiceAssistantManager(context: Context) {
    private val appContext = context.applicationContext

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status: $status")
            }
        }
    }

    fun speak(text: String, language: AssistantLanguage) {
        val cleanText = cleanForSpeech(text)
        if (cleanText.isBlank()) return
        if (!ttsReady) {
            _voiceError.value = "Voice engine is warming up, please tap Speak again in a moment."
            return
        }
        val locale = Locale.forLanguageTag(language.localeTag)
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.forLanguageTag("en-IN"))
        }
        _isSpeaking.value = true
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startListening(
        language: AssistantLanguage,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    ) {
        _voiceError.value = null
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _voiceError.value = "Speech recognition service is not available on this device."
            return
        }
        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Please speak clearly and try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic and try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Voice recognition requires network access."
                        else -> "Voice input stopped (code $error). Please try again."
                    }
                    _voiceError.value = msg
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotBlank()) {
                        onFinalResult(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        onPartialResult(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            _isListening.value = true
            recognizer.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _voiceError.value = e.localizedMessage ?: "Unable to start microphone."
        }
    }

    fun stopListening() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun clearVoiceError() {
        _voiceError.value = null
    }

    fun shutdown() {
        stopSpeaking()
        tts?.shutdown()
        tts = null
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    private fun cleanForSpeech(raw: String): String {
        return raw
            .replace(Regex("```[\\s\\S]*?```"), " code snippet ")
            .replace(Regex("[*_`#>]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(3500)
    }

    private companion object {
        const val TAG = "VoiceAssistant"
    }
}
