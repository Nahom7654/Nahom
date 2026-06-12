package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.ui.viewmodel.TelebirrViewModel
import java.text.SimpleDateFormat
import java.util.*

// Brand Design Constants
val TelebirrTeal = Color(0xFF00A294)
val TelebirrDarkBlue = Color(0xFF132A42)
val TelebirrGold = Color(0xFFF9BF12)
val TelebirrBackgroundGray = Color(0xFFF5F7F8)
val TelebirrCardBlue = Color(0xFF1C3A5A)
val ReceiptWaveColor = Color(0xFFE3E8EC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelebirrMerchantPayScreen(
    viewModel: TelebirrViewModel,
    modifier: Modifier = Modifier
) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val phoneNumber by viewModel.phoneNumber.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    
    val merchantCode by viewModel.merchantCode.collectAsStateWithLifecycle()
    val merchantName by viewModel.merchantName.collectAsStateWithLifecycle()
    val paymentPhone by viewModel.paymentPhone.collectAsStateWithLifecycle()
    val amount by viewModel.amount.collectAsStateWithLifecycle()
    val remarks by viewModel.remarks.collectAsStateWithLifecycle()
    
    val transactions by viewModel.transactionsState.collectAsStateWithLifecycle()

    val isMerchantCodeValid = merchantCode.length >= 3
    val isMerchantNameValid = merchantName.isNotEmpty()
    val isPaymentPhoneValid = paymentPhone.replace(" ", "").replace("-", "").matches(Regex("^(\\+251|251|0)?(9|7)\\d{8}$"))
    val amtDouble = amount.toDoubleOrNull() ?: 0.0
    val isAmountValid = amtDouble > 0.0 && amtDouble <= balance
    
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val showPinPrompt by viewModel.showPinPrompt.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val enteredPin by viewModel.enteredPin.collectAsStateWithLifecycle()
    val formError by viewModel.errorMessage.collectAsStateWithLifecycle()
    val currentReceipt by viewModel.currentReceipt.collectAsStateWithLifecycle()
    val selectedTxItem by viewModel.selectedTransactionItem.collectAsStateWithLifecycle()

    var isBalanceVisible by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCategoryFilter by remember { mutableStateOf("All") }

    // Screen Layout
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "tele",
                            color = TelebirrGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            "birr",
                            color = Color.White,
                            fontWeight = FontWeight.Normal,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Pay",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .background(TelebirrTeal, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TelebirrDarkBlue,
                    titleContentColor = Color.White
                ),
                modifier = Modifier.testTag("telebirr_top_app_bar")
            )
        },
        containerColor = TelebirrBackgroundGray,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // 1. Telebirr Account Summary Card
                item {
                    AccountSummaryCard(
                        userName = userName,
                        phoneNumber = phoneNumber,
                        balance = balance,
                        isBalanceVisible = isBalanceVisible,
                        onToggleBalance = { isBalanceVisible = !isBalanceVisible }
                    )
                }

                // 2. Active payment options (Manual details OR Click-to-pay)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("merchant_payment_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Pay Merchant",
                                style = MaterialTheme.typography.titleMedium,
                                color = TelebirrDarkBlue,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Quick Pay popular Ethiopian Merchants Row
                            Text(
                                "Quick selection",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(viewModel.popularMerchants.toList()) { (code, info) ->
                                    QuickMerchantItem(
                                        info = info,
                                        isSelected = (merchantCode == code),
                                        onSelect = { viewModel.selectMerchantFromQuickPay(code) }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))

                            // Merchant Code Field
                            OutlinedTextField(
                                value = merchantCode,
                                onValueChange = { viewModel.onMerchantCodeChange(it) },
                                label = { Text("Merchant Code") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = "Merchant Code", tint = TelebirrTeal) },
                                trailingIcon = {
                                    if (isMerchantCodeValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid Merchant Code",
                                            tint = Color(0xFF2E7D32)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("merchant_code_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TelebirrTeal,
                                    focusedLabelColor = TelebirrTeal
                                )
                            )

                            // Merchant Name Custom/Resolved Field
                            OutlinedTextField(
                                value = merchantName,
                                onValueChange = { viewModel.onMerchantNameChange(it) },
                                label = { Text("Merchant Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Merchant Name", tint = TelebirrTeal) },
                                trailingIcon = {
                                    if (isMerchantNameValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid Merchant Name",
                                            tint = Color(0xFF2E7D32)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("merchant_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TelebirrTeal,
                                    focusedLabelColor = TelebirrTeal
                                )
                            )

                            // Phone Number Field
                            OutlinedTextField(
                                value = paymentPhone,
                                onValueChange = { viewModel.onPaymentPhoneChange(it) },
                                label = { Text("Phone Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone Number", tint = TelebirrTeal) },
                                trailingIcon = {
                                    if (isPaymentPhoneValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid Phone Number",
                                            tint = Color(0xFF2E7D32)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                supportingText = { 
                                    Text("Format: 09xxxxxxxx or +2519xxxxxxxx", fontSize = 11.sp, color = Color.Gray) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_phone_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TelebirrTeal,
                                    focusedLabelColor = TelebirrTeal
                                )
                            )

                            // Amount Field
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { viewModel.onAmountChange(it) },
                                label = { Text("Amount (ETB)") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Amount", tint = TelebirrTeal) },
                                trailingIcon = {
                                    if (isAmountValid) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Valid Amount",
                                            tint = Color(0xFF2E7D32)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TelebirrTeal,
                                    focusedLabelColor = TelebirrTeal
                                )
                            )

                            // Remarks Field
                            OutlinedTextField(
                                value = remarks,
                                onValueChange = { viewModel.onRemarksChange(it) },
                                label = { Text("Remarks (Optional)") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Remarks", tint = TelebirrTeal) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("remarks_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TelebirrTeal,
                                    focusedLabelColor = TelebirrTeal
                                )
                            )

                            // Error Prompt
                            if (formError != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formError ?: "",
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // Payment Action Button
                            Button(
                                onClick = { viewModel.initiatePayment() },
                                colors = ButtonDefaults.buttonColors(containerColor = TelebirrTeal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("pay_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Pay Securely with telebirr",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 3. Transaction History Header + Filters
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Transaction History",
                            style = MaterialTheme.typography.titleMedium,
                            color = TelebirrDarkBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                        
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by Merchant or Txn ID...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TelebirrTeal,
                                focusedLabelColor = TelebirrTeal
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Categories List Bar
                        val categories = listOf("All", "Cafe", "Grocery", "Ride", "Telecom")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = (activeCategoryFilter == category),
                                    onClick = { activeCategoryFilter = category },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TelebirrTeal,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Transaction List Items
                val filteredTransactions = transactions.filter { tx ->
                    val matchesSearch = tx.merchantName.contains(searchQuery, ignoreCase = true) ||
                            tx.merchantCode.contains(searchQuery, ignoreCase = true) ||
                            tx.txId.contains(searchQuery, ignoreCase = true)
                    
                    val matchesCategory = when (activeCategoryFilter) {
                        "All" -> true
                        "Cafe" -> tx.merchantName.contains("Café", ignoreCase = true)
                        "Grocery" -> tx.merchantName.contains("Mart", ignoreCase = true)
                        "Ride" -> tx.merchantName.contains("Taxi", ignoreCase = true)
                        "Telecom" -> tx.merchantName.contains("Telecom", ignoreCase = true)
                        else -> true
                    }
                    matchesSearch && matchesCategory
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyHistoryState()
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        TransactionRowItem(
                            transaction = transaction,
                            onItemClick = { viewModel.selectTransaction(transaction) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // A background spinner while the secure network simulates payments
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = TelebirrTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Authenticating with telebirr Node...",
                                fontWeight = FontWeight.Bold,
                                color = TelebirrDarkBlue,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Securing dynamic payment channels...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // PIN Code Prompt Sheet Dialog
            if (showPinPrompt) {
                PaymentPinDialog(
                    amount = amount,
                    merchantName = merchantName,
                    enteredPin = enteredPin,
                    pinError = pinError,
                    onDigitClick = { viewModel.enterPinDigit(it) },
                    onDeleteClick = { viewModel.deletePinDigit() },
                    onCancelClick = { viewModel.cancelPinPrompt() }
                )
            }

            // Dynamic Receipt details Dialog on completed payments
            currentReceipt?.let { receipt ->
                TelebirrReceiptDialog(
                    transaction = receipt,
                    onClose = { viewModel.dismissReceipt() }
                )
            }

            // History details receipt dialog
            selectedTxItem?.let { historyItem ->
                TelebirrReceiptDialog(
                    transaction = historyItem,
                    onClose = { viewModel.selectTransaction(null) }
                )
            }
        }
    }
}

// Balance and profile visual component
@Composable
fun AccountSummaryCard(
    userName: String,
    phoneNumber: String,
    balance: Double,
    isBalanceVisible: Boolean,
    onToggleBalance: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TelebirrDarkBlue),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = userName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = phoneNumber,
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Telebirr Watermark circular visual
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(TelebirrTeal, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "tb",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "AVAILABLE TELEBIRR BALANCE",
                color = Color.LightGray.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBalanceVisible) "%.2f ETB".format(balance) else "•••••• ETB",
                    color = TelebirrGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )

                TextButton(
                    onClick = onToggleBalance,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (isBalanceVisible) "HIDE" else "SHOW",
                        color = TelebirrGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// Quick click selection grid items
@Composable
fun QuickMerchantItem(
    info: com.example.ui.viewmodel.MerchantInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(85.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) TelebirrTeal.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) TelebirrTeal else Color.LightGray.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(TelebirrTeal.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(info.icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = info.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TelebirrDarkBlue,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = info.category,
            fontSize = 9.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// Individual historic records list component
@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    onItemClick: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateString = formatter.format(Date(transaction.timestamp))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circle with visual initials
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(TelebirrDarkBlue.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.merchantName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = TelebirrDarkBlue,
                        fontSize = 14.sp
                    )
                }

                Column {
                    Text(
                        text = transaction.merchantName,
                        fontWeight = FontWeight.Bold,
                        color = TelebirrDarkBlue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ID: ${transaction.txId}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = dateString,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "-%.2f ETB".format(transaction.amount),
                    color = Color(0xFFD32F2F), // Paid expense red
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Box(
                    modifier = Modifier
                        .background(
                            color = if (transaction.status == "SUCCESS") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = transaction.status,
                        color = if (transaction.status == "SUCCESS") Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

// Transaction list is empty
@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "No History",
            tint = Color.LightGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No payments recorded",
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            fontSize = 15.sp
        )
        Text(
            "Begin a merchant pay to register transactions",
            color = Color.LightGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Secured custom PIN keyboard dialog
@Composable
fun PaymentPinDialog(
    amount: String,
    merchantName: String,
    enteredPin: String,
    pinError: String?,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Dialog(
        onDismissRequest = { onCancelClick() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(top = 10.dp)
                .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header details
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.LightGray, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "telebirr Secure Gateway",
                        color = TelebirrTeal,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Paying Merchant",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        merchantName,
                        fontWeight = FontWeight.ExtraBold,
                        color = TelebirrDarkBlue,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Amount: $amount ETB",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD32F2F),
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Enter 4-Digit Security PIN to authorize",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // PIN Code visual bullets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isEntered = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isEntered) TelebirrTeal else Color.LightGray.copy(alpha = 0.5f))
                                    .border(1.dp, if (isEntered) TelebirrTeal else Color.LightGray, CircleShape)
                            )
                        }
                    }

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError,
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Test PIN: 1234",
                            color = TelebirrGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ATM Style secure numeric custom keyboard grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Cancel", "0", "DEL")
                    )

                    for (row in keys) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (key in row) {
                                KeypadButton(
                                    label = key,
                                    onClick = {
                                        when (key) {
                                            "Cancel" -> onCancelClick()
                                            "DEL" -> onDeleteClick()
                                            else -> onDigitClick(key)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Round Keypad buttons
@Composable
fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    val isAction = label == "Cancel" || label == "DEL"
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (isAction) Color.LightGray.copy(alpha = 0.2f) else TelebirrDarkBlue.copy(
                    alpha = 0.05f
                )
            )
            .clickable { onClick() }
            .testTag("keypad_$label"),
        contentAlignment = Alignment.Center
    ) {
        if (label == "DEL") {
            Icon(Icons.Default.Clear, contentDescription = "Delete", tint = TelebirrDarkBlue)
        } else {
            Text(
                text = label,
                color = if (isAction) Color.DarkGray else TelebirrDarkBlue,
                fontSize = if (isAction) 13.sp else 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Gorgeous Custom Receipt Dialogue (with wavy edges and verified green status badge)
@Composable
fun TelebirrReceiptDialog(
    transaction: TransactionEntity,
    onClose: () -> Unit
) {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateString = formatter.format(Date(transaction.timestamp))

    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .testTag("receipt_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Receipt Top branding Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("tele", color = TelebirrGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("birr", color = TelebirrTeal, fontWeight = FontWeight.Normal, fontSize = 20.sp)
                }

                Text(
                    "TRANSACTION RECEIPT",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Verified tick
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success tick",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Payment Successful",
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Large Amount Text
                Text(
                    text = "%.2f ETB".format(transaction.amount),
                    color = TelebirrDarkBlue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Divider line dashed style using canvas drawing
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = pathEffect,
                        strokeWidth = 2f
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown details
                ReceiptRow(label = "Merchant Partner", value = transaction.merchantName)
                ReceiptRow(label = "Merchant ID / Code", value = transaction.merchantCode)
                if (transaction.phoneNumber.isNotEmpty()) {
                    ReceiptRow(label = "Secure phone number", value = transaction.phoneNumber)
                }
                ReceiptRow(label = "Transaction Tx ID", value = transaction.txId)
                ReceiptRow(label = "Transaction Date", value = dateString)
                ReceiptRow(label = "Reference / Remarks", value = transaction.remarks)
                ReceiptRow(label = "Transaction Service Fee", value = "0.00 ETB")
                ReceiptRow(label = "Payment Status", value = transaction.status)

                Spacer(modifier = Modifier.height(16.dp))

                // Wave cut style decoration
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {
                    val waveWidth = 14.sp.toPx()
                    val waveHeight = 6.sp.toPx()
                    val path = Path()
                    path.moveTo(0f, 0f)
                    var currentX = 0f
                    var toggle = true
                    while (currentX < size.width) {
                        val nextX = currentX + waveWidth
                        if (toggle) {
                            path.quadraticTo(
                                currentX + waveWidth / 2,
                                waveHeight,
                                nextX,
                                0f
                            )
                        } else {
                            path.quadraticTo(
                                currentX + waveWidth / 2,
                                -waveHeight,
                                nextX,
                                0f
                            )
                        }
                        currentX = nextX
                        toggle = !toggle
                    }
                    drawPath(path = path, color = ReceiptWaveColor, style = Stroke(width = 2f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = TelebirrDarkBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(
            text = value,
            color = TelebirrDarkBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 180.dp)
        )
    }
}
