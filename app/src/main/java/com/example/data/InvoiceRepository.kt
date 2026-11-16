package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserInfo(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

sealed interface CreditDeductionResult {
    data object Deducted : CreditDeductionResult
    data object UnlimitedSubscriber : CreditDeductionResult
    data object InsufficientCredits : CreditDeductionResult
}

class InvoiceRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val invoiceDao = database.invoiceDao()
    private val userStatsDao = database.userStatsDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Current User Info State
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    private val firestoreDb: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            val databaseId = appContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(databaseId)
        } catch (e: Exception) {
            Log.e("InvoiceRepository", "Firestore initialization failed: ${e.message}")
            null
        }
    }

    // Firebase state flag
    private val isFirebaseReady: Boolean by lazy {
        try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            FirebaseAuth.getInstance()
            firestoreDb != null
        } catch (e: Exception) {
            Log.e("InvoiceRepository", "Firebase SDK is not available or unconfigured: ${e.message}")
            false
        }
    }

    init {
        // Automatically check if Firebase Auth has an active session on startup, or set a dummy user
        if (isFirebaseReady) {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    _currentUser.value = UserInfo(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString()
                    )
                }
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error checking active Firebase session", e)
            }
        }
    }

    fun isFirebaseMode(): Boolean = isFirebaseReady

    // --- Authentication ---

    suspend fun signInWithGoogle(idToken: String): Result<UserInfo> = withContext(Dispatchers.IO) {
        if (!isFirebaseReady) {
            return@withContext Result.failure(IllegalStateException("Firebase is unconfigured in this build environment."))
        }

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Auth returned null user")
            val userInfo = UserInfo(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            _currentUser.value = userInfo

            // Sync stats
            initializeUserStats(userInfo.uid)

            Result.success(userInfo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("InvoiceRepository", "Firebase sign-in failed", e)
            Result.failure(e)
        }
    }

    fun signInDemoUser() {
        if (!BuildConfig.DEBUG) {
            Log.w("InvoiceRepository", "Demo sign-in blocked in release builds.")
            return
        }
        val demoUser = UserInfo(
            uid = "demo_szco_2026",
            email = "demo@bleskovafaktura.local",
            displayName = "Demo Živnostník",
            photoUrl = null
        )
        _currentUser.value = demoUser
        repositoryScope.launch {
            initializeUserStats(demoUser.uid)
        }
    }

    fun signOut() {
        if (isFirebaseReady) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error signing out from Firebase", e)
            }
        }
        _currentUser.value = null
    }

    // --- Invoices Data Access ---

    fun getInvoices(): Flow<List<Invoice>> {
        val user = _currentUser.value ?: return flow { emit(emptyList()) }
        
        return if (isFirebaseReady) {
            // In high-fidelity production, we stream from Firestore and cache locally.
            // For robust local-first, we query our Room DB, which we sync in the background.
            // This is offline-first!
            invoiceDao.getInvoicesForUser(user.uid).map { entities ->
                entities.map { it.toInvoice() }
            }
        } else {
            // Pure local mode
            invoiceDao.getInvoicesForUser(user.uid).map { entities ->
                entities.map { it.toInvoice() }
            }
        }
    }

    suspend fun insertInvoice(invoice: Invoice) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val updatedInvoice = invoice.copy(userId = user.uid)

        // Save to Local SQLite cache (Room) immediately
        invoiceDao.insertInvoice(updatedInvoice.toEntity())

        // Save to Firebase Firestore if ready
        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                db.collection("users")
                    .document(user.uid)
                    .collection("invoices")
                    .document(updatedInvoice.id)
                    .set(updatedInvoice)
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error saving invoice to Firestore, cached locally", e)
            }
        }

        // Update statistics
        recalculateStats(user.uid)
    }

    suspend fun deleteInvoice(id: String) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext

        // Delete locally from Room
        invoiceDao.deleteInvoice(id)

        // Delete from Firebase Firestore if ready
        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                db.collection("users")
                    .document(user.uid)
                    .collection("invoices")
                    .document(id)
                    .delete()
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error deleting invoice from Firestore, removed locally", e)
            }
        }

        // Update statistics
        recalculateStats(user.uid)
    }

    // --- User Statistics, Credits, and Subscription ---

    fun getUserStatsFlow(): Flow<UserStats> {
        val user = _currentUser.value ?: return flow { emit(UserStats()) }
        return userStatsDao.getUserStatsFlow(user.uid).map { entity ->
            entity?.toUserStats() ?: UserStats(userId = user.uid)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun initializeUserStats(userId: String) = withContext(Dispatchers.IO) {
        var localEntity = userStatsDao.getUserStats(userId)
        
        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                val firestoreDoc = db.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                if (firestoreDoc.exists()) {
                    val firestoreCredits = firestoreDoc.getLong("credits")?.toInt() ?: 10
                    val firestoreSubscribed = firestoreDoc.getBoolean("isSubscribed") ?: false
                    
                    val mergedStats = UserStatsEntity(
                        userId = userId,
                        credits = firestoreCredits,
                        isSubscribed = firestoreSubscribed,
                        totalInvoiced = localEntity?.totalInvoiced ?: 0.0,
                        invoiceCount = localEntity?.invoiceCount ?: 0
                    )
                    userStatsDao.insertUserStats(mergedStats)
                    return@withContext
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error fetching user stats from Firestore", e)
            }
        }

        if (localEntity == null) {
            localEntity = UserStatsEntity(
                userId = userId,
                credits = 10, // Starting credits
                isSubscribed = false,
                totalInvoiced = 0.0,
                invoiceCount = 0
            )
            userStatsDao.insertUserStats(localEntity)
        }
    }

    suspend fun addCredits(amount: Int) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val currentStats = userStatsDao.getUserStats(user.uid)?.toUserStats() ?: UserStats(userId = user.uid)
        val updatedStats = currentStats.copy(credits = currentStats.credits + amount)
        
        userStatsDao.insertUserStats(updatedStats.toEntity())

        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                db.collection("users")
                    .document(user.uid)
                    .update("credits", updatedStats.credits)
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error updating credits in Firestore", e)
            }
        }
    }

    suspend fun deductCredit(): CreditDeductionResult = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext CreditDeductionResult.InsufficientCredits
        val currentStats = userStatsDao.getUserStats(user.uid)?.toUserStats() ?: UserStats(userId = user.uid)
        
        if (currentStats.isSubscribed) {
            return@withContext CreditDeductionResult.UnlimitedSubscriber
        }
        
        if (currentStats.credits <= 0) {
            return@withContext CreditDeductionResult.InsufficientCredits
        }

        val updatedStats = currentStats.copy(credits = currentStats.credits - 1)
        userStatsDao.insertUserStats(updatedStats.toEntity())

        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                db.collection("users")
                    .document(user.uid)
                    .update("credits", updatedStats.credits)
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error deducting credit in Firestore", e)
            }
        }
        return@withContext CreditDeductionResult.Deducted
    }

    suspend fun subscribePremium() = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val currentStats = userStatsDao.getUserStats(user.uid)?.toUserStats() ?: UserStats(userId = user.uid)
        val updatedStats = currentStats.copy(isSubscribed = true)
        
        userStatsDao.insertUserStats(updatedStats.toEntity())

        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                db.collection("users")
                    .document(user.uid)
                    .update("isSubscribed", true)
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error updating subscription in Firestore", e)
            }
        }
    }

    suspend fun cancelSubscription() = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        val currentStats = userStatsDao.getUserStats(user.uid)?.toUserStats() ?: UserStats(userId = user.uid)
        val updatedStats = currentStats.copy(isSubscribed = false)
        
        userStatsDao.insertUserStats(updatedStats.toEntity())

        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                db.collection("users")
                    .document(user.uid)
                    .update("isSubscribed", false)
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error canceling subscription in Firestore", e)
            }
        }
    }

    private suspend fun recalculateStats(userId: String) {
        // Query all invoices from room for this user
        // Wait, because Room handles flow we can do a blocking query or direct list
        // Let's read from room Dao. Since we didn't add a direct query for List, we'll run a quick calculation in a transaction or query
        val list = database.openHelper.writableDatabase.let { db ->
            val cursor = db.query("SELECT totalAmount FROM invoices WHERE userId = '$userId'")
            val amounts = mutableListOf<Double>()
            if (cursor.moveToFirst()) {
                do {
                    amounts.add(cursor.getDouble(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
            amounts
        }

        val total = list.sum()
        val count = list.size

        val currentStats = userStatsDao.getUserStats(userId)?.toUserStats() ?: UserStats(userId = userId)
        val updatedStats = currentStats.copy(
            totalInvoiced = total,
            invoiceCount = count
        )
        userStatsDao.insertUserStats(updatedStats.toEntity())

        val db = firestoreDb
        if (isFirebaseReady && db != null) {
            try {
                db.collection("users")
                    .document(userId)
                    .update(
                        "totalInvoiced", total,
                        "invoiceCount", count
                    )
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("InvoiceRepository", "Error updating aggregate stats in Firestore", e)
            }
        }
    }
}
