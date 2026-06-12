package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val txId: String,
    val merchantName: String,
    val merchantCode: String,
    val amount: Double,
    val timestamp: Long,
    val remarks: String,
    val status: String, // "SUCCESS", "FAILEED", "PENDING"
    val fee: Double = 0.0,
    val phoneNumber: String = ""
)
