package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiExtractionResult
import com.example.data.CreditDeductionResult
import com.example.data.ExtractedInvoiceJson
import com.example.data.InvoiceAiService
import com.example.data.Invoice
import com.example.data.InvoiceRepository
import com.example.data.UserInfo
import com.example.data.UserStats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Scanning(val progressMessage: String) : ScanUiState
    data class Success(val extractedData: ExtractedInvoiceJson, val bitmap: Bitmap) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InvoiceRepository(application)

    // Current logged in user
    val currentUser: StateFlow<UserInfo?> = repository.currentUser

    // List of invoices, reactive to current user changes
    val invoices: StateFlow<List<Invoice>> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getInvoices()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current user statistics (credits, subscription status, etc.)
    val userStats: StateFlow<UserStats> = currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getUserStatsFlow()
            } else {
                flowOf(UserStats())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStats()
        )

    // Scanning UI State
    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    // Flag to auto-launch camera on scan screen
    var shouldAutoLaunchCamera = false

    // Mode checker (True = Real Firebase, False = Local Offline Mock Mode)
    val isFirebaseMode: Boolean = repository.isFirebaseMode()

    // --- Authentication Actions ---

    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            onResult(result.isSuccess)
        }
    }

    fun signInDemoUser() {
        repository.signInDemoUser()
    }

    fun signOut() {
        repository.signOut()
        _scanState.value = ScanUiState.Idle
    }

    // --- Invoices Management Actions ---

    fun saveInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repository.insertInvoice(invoice)
        }
    }

    fun deleteInvoice(id: String) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
        }
    }

    // --- Scanner / AI Extraction Actions ---

    private val funnySlovakScannerMessages = listOf(
        "Blesk štartuje AI analyzátor...",
        "Čítame drobné písmo na doklade...",
        "Počítame DPH a celkovú sumu...",
        "Identifikujeme dodávateľa a IČO...",
        "Bleskovo kategorizujeme váš výdavok...",
        "Ukladáme detaily do bezpečného cloudu..."
    )

    fun startNewScan() {
        _scanState.value = ScanUiState.Idle
    }

    fun scanInvoiceImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning("Bleskovo kontrolujem kredity...")

            val deductionResult = repository.deductCredit()
            if (deductionResult is CreditDeductionResult.InsufficientCredits) {
                _scanState.value = ScanUiState.Error("Nemáte dostatok kreditov na skenovanie. Zakúpte si kredity alebo Blesk Premium v sekcii Predplatné.")
                return@launch
            }
            val shouldRefundOnFailure = deductionResult is CreditDeductionResult.Deducted

            val progressJob = launch {
                for (msg in funnySlovakScannerMessages) {
                    _scanState.value = ScanUiState.Scanning(msg)
                    kotlinx.coroutines.delay(1200)
                }
            }

            try {
                when (val result = InvoiceAiService.extractInvoiceData(bitmap)) {
                    is AiExtractionResult.Success -> {
                        _scanState.value = ScanUiState.Success(result.data, bitmap)
                    }
                    is AiExtractionResult.InvalidDocument -> {
                        if (shouldRefundOnFailure) repository.addCredits(1)
                        val refundNote = if (shouldRefundOnFailure) " Kredit bol vrátený." else ""
                        _scanState.value = ScanUiState.Error("${result.message}$refundNote")
                    }
                    is AiExtractionResult.Timeout -> {
                        if (shouldRefundOnFailure) repository.addCredits(1)
                        val refundNote = if (shouldRefundOnFailure) " Kredit bol vrátený." else ""
                        _scanState.value = ScanUiState.Error("AI analýza vypršala po 60 sekundách. Skúste to prosím znova.$refundNote")
                    }
                    is AiExtractionResult.ServiceError -> {
                        if (shouldRefundOnFailure) repository.addCredits(1)
                        val refundNote = if (shouldRefundOnFailure) " Kredit bol vrátený." else ""
                        _scanState.value = ScanUiState.Error("${result.message}$refundNote")
                    }
                }
            } catch (e: CancellationException) {
                if (shouldRefundOnFailure) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        repository.addCredits(1)
                    }
                }
                throw e
            } catch (e: Exception) {
                if (shouldRefundOnFailure) repository.addCredits(1)
                val refundNote = if (shouldRefundOnFailure) " Kredit bol vrátený." else ""
                _scanState.value = ScanUiState.Error("Neočekávaná chyba pri spracovaní dokladu: ${e.localizedMessage ?: "neznáma chyba"}.$refundNote")
            } finally {
                progressJob.cancel()
            }
        }
    }

    // --- Monetization Actions (Simulated purchases) ---

    fun buyCreditPack(amount: Int, priceEur: Double, onFinished: () -> Unit) {
        viewModelScope.launch {
            // In Play Billing, this would be triggered after a successful Google Play Purchase.
            // We simulate it here by instantly updating the database.
            repository.addCredits(amount)
            onFinished()
        }
    }

    fun subscribePremium(onFinished: () -> Unit) {
        viewModelScope.launch {
            repository.subscribePremium()
            onFinished()
        }
    }

    fun cancelPremium() {
        viewModelScope.launch {
            repository.cancelSubscription()
        }
    }
}
