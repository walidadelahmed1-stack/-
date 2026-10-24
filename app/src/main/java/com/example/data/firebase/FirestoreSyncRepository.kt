package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.PlayerProgress
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "FirestoreSyncRepo"
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Observes real-time progress updates for the authenticated user
     */
    fun observeProgress(userId: String): Flow<PlayerProgress?> = callbackFlow {
        val path = "users/$userId/progress/data"
        val docRef = db.collection("users").document(userId).collection("progress").document("data")

        val listener: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, path)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val data = snapshot.data ?: emptyMap()
                    val starsJson = data["starsJson"] as? String ?: "{}"
                    val scoresJson = data["scoresJson"] as? String ?: "{}"
                    val solvedList = (data["solvedPuzzles"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val storiesList = (data["storiesSeen"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()

                    val starsMap = mutableMapOf<String, Int>()
                    val starsObj = JSONObject(starsJson)
                    for (k in starsObj.keys()) {
                        starsMap[k] = starsObj.getInt(k)
                    }

                    val scoresMap = mutableMapOf<String, Int>()
                    val scoresObj = JSONObject(scoresJson)
                    for (k in scoresObj.keys()) {
                        scoresMap[k] = scoresObj.getInt(k)
                    }

                    val puzzleScoresJson = data["puzzleScoresJson"] as? String ?: "{}"
                    val puzzleScoresMap = mutableMapOf<String, com.example.data.model.PuzzleScoreRecord>()
                    try {
                        val pObj = JSONObject(puzzleScoresJson)
                        for (k in pObj.keys()) {
                            val item = pObj.getJSONObject(k)
                            puzzleScoresMap[k] = com.example.data.model.PuzzleScoreRecord(
                                levelId = k,
                                highScore = item.optInt("highScore", 0),
                                bestTimeSeconds = item.optLong("bestTimeSeconds", 0L),
                                fewestMoves = item.optInt("fewestMoves", 0),
                                stars = item.optInt("stars", 3),
                                timestamp = item.optLong("timestamp", System.currentTimeMillis())
                            )
                        }
                    } catch (_: Exception) {}

                    val progress = PlayerProgress(
                        stars = starsMap,
                        scores = scoresMap,
                        solvedPuzzles = solvedList.toSet(),
                        storiesSeen = storiesList.toSet(),
                        puzzleScores = puzzleScoresMap
                    )
                    trySend(progress)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed parsing remote progress snapshot", e)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Uploads local PlayerProgress to Firestore under the user's private collection
     */
    suspend fun saveProgress(progress: PlayerProgress) {
        val uid = auth.currentUser?.uid ?: return
        val path = "users/$uid/progress/data"
        val docRef = db.collection("users").document(uid).collection("progress").document("data")

        val starsObj = JSONObject()
        progress.stars.forEach { (k, v) -> starsObj.put(k, v) }

        val scoresObj = JSONObject()
        progress.scores.forEach { (k, v) -> scoresObj.put(k, v) }

        val puzzleScoresObj = JSONObject()
        progress.puzzleScores.forEach { (k, rec) ->
            val item = JSONObject().apply {
                put("highScore", rec.highScore)
                put("bestTimeSeconds", rec.bestTimeSeconds)
                put("fewestMoves", rec.fewestMoves)
                put("stars", rec.stars)
                put("timestamp", rec.timestamp)
            }
            puzzleScoresObj.put(k, item)
        }

        val payload = mapOf(
            "userId" to uid,
            "starsJson" to starsObj.toString(),
            "scoresJson" to scoresObj.toString(),
            "puzzleScoresJson" to puzzleScoresObj.toString(),
            "solvedPuzzles" to progress.solvedPuzzles.toList(),
            "storiesSeen" to progress.storiesSeen.toList(),
            "highestChapter" to (progress.stars.keys.mapNotNull { it.split("-").firstOrNull()?.toIntOrNull() }.maxOrNull() ?: 1),
            "totalStars" to progress.totalStars,
            "totalScore" to progress.scores.values.sum(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            Log.d(TAG, "Cloud progress synced successfully for user: $uid")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Log.w(TAG, "Failed saving progress to Firestore: ${e.message}")
        }
    }

    /**
     * One-time fetch of user's remote cloud progress
     */
    suspend fun fetchProgress(): PlayerProgress? {
        val uid = auth.currentUser?.uid ?: return null
        val path = "users/$uid/progress/data"
        val docRef = db.collection("users").document(uid).collection("progress").document("data")

        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return null

            val data = snapshot.data ?: return null
            val starsJson = data["starsJson"] as? String ?: "{}"
            val scoresJson = data["scoresJson"] as? String ?: "{}"
            val puzzleScoresJson = data["puzzleScoresJson"] as? String ?: "{}"
            val solvedList = (data["solvedPuzzles"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val storiesList = (data["storiesSeen"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()

            val starsMap = mutableMapOf<String, Int>()
            val starsObj = JSONObject(starsJson)
            for (k in starsObj.keys()) {
                starsMap[k] = starsObj.getInt(k)
            }

            val scoresMap = mutableMapOf<String, Int>()
            val scoresObj = JSONObject(scoresJson)
            for (k in scoresObj.keys()) {
                scoresMap[k] = scoresObj.getInt(k)
            }

            val puzzleScoresMap = mutableMapOf<String, com.example.data.model.PuzzleScoreRecord>()
            try {
                val pObj = JSONObject(puzzleScoresJson)
                for (k in pObj.keys()) {
                    val item = pObj.getJSONObject(k)
                    puzzleScoresMap[k] = com.example.data.model.PuzzleScoreRecord(
                        levelId = k,
                        highScore = item.optInt("highScore", 0),
                        bestTimeSeconds = item.optLong("bestTimeSeconds", 0L),
                        fewestMoves = item.optInt("fewestMoves", 0),
                        stars = item.optInt("stars", 3),
                        timestamp = item.optLong("timestamp", System.currentTimeMillis())
                    )
                }
            } catch (_: Exception) {}

            PlayerProgress(
                stars = starsMap,
                scores = scoresMap,
                solvedPuzzles = solvedList.toSet(),
                storiesSeen = storiesList.toSet(),
                puzzleScores = puzzleScoresMap
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }
}
