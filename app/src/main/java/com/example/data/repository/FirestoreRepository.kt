package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.UserProfile
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        preferredLanguage: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val safeName = displayName.trim().ifBlank { "Friend" }.take(80)
            val safeEmail = email?.trim()?.take(120)
            val safeLang = if (preferredLanguage in VALID_LANGUAGES) preferredLanguage else "Hinglish"

            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "preferredLanguage" to safeLang,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeEmail != null) {
                    updates["email"] = safeEmail
                }
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "preferredLanguage" to safeLang,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredLanguage(userId: String, preferredLanguage: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            val safeLang = if (preferredLanguage in VALID_LANGUAGES) preferredLanguage else "Hinglish"
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "preferredLanguage" to safeLang,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val name = auth.currentUser?.displayName?.take(80)?.ifBlank { "Friend" } ?: "Friend"
                val email = auth.currentUser?.email?.take(120)
                upsertUserProfile(uid, name, email, safeLang).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeChatSessions(userId: String): Flow<List<ChatSession>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/chats"
        emitAll(
            db.collection("users").document(uid).collection("chats")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun loadChatMessages(userId: String, chatId: String): Result<List<ChatMessage>> {
        val path = "users/$userId/chats/$chatId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(chatId.isNotBlank()) { "Chat ID cannot be blank." }
            val snapshot = db.collection("users").document(uid)
                .collection("chats").document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
            val messages = snapshot.toObjects(
                ChatMessage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            Result.success(messages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createOrAppendTurn(
        userId: String,
        existingChatId: String?,
        title: String,
        category: String,
        userText: String,
        modelText: String,
        hasImage: Boolean,
        toolTag: String?,
        currentMessageCount: Long
    ): Result<String> {
        var logPath = "users/$userId/chats"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val safeCategory = if (category in VALID_CATEGORIES) category else "chat"
            val safeTitle = title.trim().ifBlank { "New Conversation" }.take(120)
            val safeUserText = userText.trim().take(32000)
            val safeModelText = modelText.trim().take(32000)
            val safePreview = safeModelText.replace("\n", " ").trim().take(280)
            val safeToolTag = toolTag?.trim()?.take(60)?.takeIf { it.isNotEmpty() }

            val chatsCollection = db.collection("users").document(uid).collection("chats")
            val chatRef = if (existingChatId.isNullOrBlank()) {
                chatsCollection.document()
            } else {
                chatsCollection.document(existingChatId)
            }
            logPath = chatRef.path

            val userMsgRef = chatRef.collection("messages").document()
            val modelMsgRef = chatRef.collection("messages").document()

            val batch = db.batch()
            if (existingChatId.isNullOrBlank()) {
                val chatPayload = mapOf(
                    "userId" to uid,
                    "title" to safeTitle,
                    "category" to safeCategory,
                    "lastMessagePreview" to safePreview,
                    "messageCount" to 2L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(chatRef, chatPayload)
            } else {
                val updatedCount = (currentMessageCount + 2L).coerceAtMost(10000L)
                val chatUpdates = mapOf(
                    "title" to safeTitle,
                    "category" to safeCategory,
                    "lastMessagePreview" to safePreview,
                    "messageCount" to updatedCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.update(chatRef, chatUpdates)
            }

            val userMsgPayload = mapOf(
                "userId" to uid,
                "role" to "user",
                "text" to safeUserText,
                "hasImage" to hasImage,
                "toolTag" to safeToolTag,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            val modelMsgPayload = mapOf(
                "userId" to uid,
                "role" to "model",
                "text" to safeModelText,
                "hasImage" to false,
                "toolTag" to safeToolTag,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            batch.set(userMsgRef, userMsgPayload)
            batch.set(modelMsgRef, modelMsgPayload)
            batch.commit().await()

            Result.success(chatRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, logPath)
            Result.failure(e)
        }
    }

    suspend fun deleteChatSession(userId: String, chatId: String): Result<Unit> {
        val path = "users/$userId/chats/$chatId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(chatId.isNotBlank()) { "Chat ID cannot be blank." }
            val chatRef = db.collection("users").document(uid).collection("chats").document(chatId)
            val messagesSnap = chatRef.collection("messages").get().await()
            val batch = db.batch()
            messagesSnap.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.delete(chatRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private companion object {
        val VALID_LANGUAGES = setOf("English", "Hindi", "Urdu", "Hinglish")
        val VALID_CATEGORIES = setOf("chat", "homework", "vision", "tools")
    }
}
