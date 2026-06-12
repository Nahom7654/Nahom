package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun getTransactionByTxId(txId: String): TransactionEntity? {
        return transactionDao.getTransactionByTxId(txId)
    }

    suspend fun clearAll() {
        transactionDao.deleteAllTransactions()
    }
}
