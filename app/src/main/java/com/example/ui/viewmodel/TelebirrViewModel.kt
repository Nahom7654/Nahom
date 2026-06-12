package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Random

class TelebirrViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val transactionsState: StateFlow<List<TransactionEntity>>

    // User's Telebirr account information
    private val _balance = MutableStateFlow(25000.00) // Starting account balance in ETB
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _phoneNumber = MutableStateFlow("+251 912 34 56 78")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _userName = MutableStateFlow("Nahom Mengistu")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Form states
    private val _merchantCode = MutableStateFlow("")
    val merchantCode = _merchantCode.asStateFlow()

    private val _merchantName = MutableStateFlow("")
    val merchantName = _merchantName.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount = _amount.asStateFlow()

    private val _remarks = MutableStateFlow("")
    val remarks = _remarks.asStateFlow()

    private val _paymentPhone = MutableStateFlow("")
    val paymentPhone = _paymentPhone.asStateFlow()

    // UI flows
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _showPinPrompt = MutableStateFlow(false)
    val showPinPrompt = _showPinPrompt.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin = _enteredPin.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError = _pinError.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _currentReceipt = MutableStateFlow<TransactionEntity?>(null)
    val currentReceipt = _currentReceipt.asStateFlow()

    private val _selectedTransactionItem = MutableStateFlow<TransactionEntity?>(null)
    val selectedTransactionItem = _selectedTransactionItem.asStateFlow()

    // Popular Ethiopian Merchants matching Telebirr Codes
    val popularMerchants = mapOf(
        "102030" to MerchantInfo("Sheger Café", "Food & Drink", "☕"),
        "405060" to MerchantInfo("Minab Mart", "Grocery Store", "🛒"),
        "708090" to MerchantInfo("Zemen Taxi", "Ride Sharing", "🚗"),
        "112233" to MerchantInfo("Total Energies", "Fuel Station", "⛽"),
        "445566" to MerchantInfo("Anbessa Bus", "Transportation", "🚌"),
        "778899" to MerchantInfo("Ethio Telecom", "Utilities & Telecom", "📞")
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())
        
        transactionsState = repository.allTransactions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Pre-populate if empty
        viewModelScope.launch {
            repository.allTransactions.collect { list ->
                if (list.isEmpty()) {
                    prePopulateDatabase()
                }
            }
        }
    }

    private suspend fun prePopulateDatabase() {
        val now = System.currentTimeMillis()
        val mockData = listOf(
            TransactionEntity(
                txId = "TXN09K284L",
                merchantName = "Sheger Café",
                merchantCode = "102030",
                amount = 240.00,
                timestamp = now - (3 * 3600 * 1000), // 3 hours ago
                remarks = "Breakfast combo",
                status = "SUCCESS",
                fee = 0.0
            ),
            TransactionEntity(
                txId = "TXN08P543Q",
                merchantName = "Minab Mart",
                merchantCode = "405060",
                amount = 1450.00,
                timestamp = now - (24 * 3600 * 1000), // 1 day ago
                remarks = "Weekly groceries",
                status = "SUCCESS",
                fee = 0.0
            ),
            TransactionEntity(
                txId = "TXN07S391A",
                merchantName = "Zemen Taxi",
                merchantCode = "708090",
                amount = 350.00,
                timestamp = now - (48 * 3600 * 1000), // 2 days ago
                remarks = "Office ride",
                status = "SUCCESS",
                fee = 0.0
            ),
            TransactionEntity(
                txId = "TXN05F120Z",
                merchantName = "Ethio Telecom",
                merchantCode = "778899",
                amount = 500.00,
                timestamp = now - (5 * 24 * 3600 * 1000), // 5 days ago
                remarks = "Monthly Airtime recharge",
                status = "SUCCESS",
                fee = 0.0
            )
        )
        for (item in mockData) {
            repository.insertTransaction(item)
            // also deduct from starting balance for added realism
            _balance.value -= item.amount
        }
    }

    fun onMerchantCodeChange(code: String) {
        if (code.length <= 10) {
            _merchantCode.value = code
            val matched = popularMerchants[code]
            if (matched != null) {
                _merchantName.value = matched.name
            } else {
                _merchantName.value = ""
            }
        }
    }

    fun onMerchantNameChange(name: String) {
        _merchantName.value = name
    }

    fun onAmountChange(value: String) {
        // filter input to allow only valid numeric decimals
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
            _amount.value = value
        }
    }

    fun onRemarksChange(text: String) {
        _remarks.value = text
    }

    fun onPaymentPhoneChange(number: String) {
        if (number.length <= 15) {
            _paymentPhone.value = number
        }
    }

    fun selectMerchantFromQuickPay(code: String) {
        onMerchantCodeChange(code)
    }

    fun selectTransaction(transaction: TransactionEntity?) {
        _selectedTransactionItem.value = transaction
    }

    fun dismissReceipt() {
        _currentReceipt.value = null
    }

    fun validateForm(): Boolean {
        _errorMessage.value = null
        val code = _merchantCode.value.trim()
        val name = _merchantName.value.trim()
        val amtStr = _amount.value.trim()
        val phone = _paymentPhone.value.trim()

        if (code.isEmpty() || code.length < 3) {
            _errorMessage.value = "Enter a valid Merchant Code (at least 3 digits)"
            return false
        }
        if (name.isEmpty()) {
            _errorMessage.value = "Merchant name could not be empty"
            return false
        }
        
        // Secure validation for phone number
        if (phone.isEmpty()) {
            _errorMessage.value = "Secure verification Phone Number is required"
            return false
        }
        val cleanPhone = phone.replace(" ", "").replace("-", "")
        // Matches: 09xxxxxxxx, 07xxxxxxxx, 2519xxxxxxxx, +2519xxxxxxxx, etc.
        val ethiopianPhoneRegex = Regex("^(\\+251|251|0)?(9|7)\\d{8}$")
        if (!cleanPhone.matches(ethiopianPhoneRegex)) {
            _errorMessage.value = "Enter a valid Ethiopian phone number (e.g., 09xxxxxxxx or +2519xxxxxxxx)"
            return false
        }

        val amt = amtStr.toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            _errorMessage.value = "Enter a valid amount greater than 0"
            return false
        }
        if (amt > _balance.value) {
            _errorMessage.value = "Insufficient Telebirr Balance (Available: %.2f ETB)".format(_balance.value)
            return false
        }
        return true
    }

    fun initiatePayment() {
        if (validateForm()) {
            _showPinPrompt.value = true
            _enteredPin.value = ""
            _pinError.value = null
        }
    }

    fun cancelPinPrompt() {
        _showPinPrompt.value = false
        _enteredPin.value = ""
        _pinError.value = null
    }

    fun enterPinDigit(digit: String) {
        if (_enteredPin.value.length < 4) {
            _enteredPin.value += digit
            _pinError.value = null
            
            // Auto submit PIN on 4th digit
            if (_enteredPin.value.length == 4) {
                submitPinAndDeduct()
            }
        }
    }

    fun deletePinDigit() {
        if (_enteredPin.value.isNotEmpty()) {
            _enteredPin.value = _enteredPin.value.dropLast(1)
            _pinError.value = null
        }
    }

    private fun submitPinAndDeduct() {
        // Default PIN to succeed is "1234" (realistic & standard)
        if (_enteredPin.value != "1234") {
            _pinError.value = "Incorrect Telebirr PIN digits! Please enter 1234."
            _enteredPin.value = ""
            return
        }

        viewModelScope.launch {
            _showPinPrompt.value = false
            _isProcessing.value = true
            
            // Simulate networking delay of Telebirr secure network
            kotlinx.coroutines.delay(1800)

            val amt = _amount.value.toDoubleOrNull() ?: 0.0
            val code = _merchantCode.value
            val name = _merchantName.value
            val rem = _remarks.value.ifEmpty { "Merchant Payment" }
            
            // Deduct from balance
            _balance.value -= amt

            val txId = generateTxId()
            val newTx = TransactionEntity(
                txId = txId,
                merchantName = name,
                merchantCode = code,
                amount = amt,
                timestamp = System.currentTimeMillis(),
                remarks = rem,
                status = "SUCCESS",
                fee = 0.0,
                phoneNumber = _paymentPhone.value
            )

            repository.insertTransaction(newTx)
            _currentReceipt.value = newTx
            
            // Clear fields for next transaction
            _merchantCode.value = ""
            _merchantName.value = ""
            _amount.value = ""
            _remarks.value = ""
            _paymentPhone.value = ""
            
            _isProcessing.value = false
        }
    }

    private fun generateTxId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val rnd = Random()
        val text = StringBuilder("TXN")
        for (i in 0 until 7) {
            text.append(chars[rnd.nextInt(chars.length)])
        }
        return text.toString()
    }
}

data class MerchantInfo(
    val name: String,
    val category: String,
    val icon: String
)
