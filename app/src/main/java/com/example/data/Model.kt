package com.example.data

import java.util.UUID

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val supplierName: String = "",
    val ico: String? = null,
    val dic: String? = null,
    val issueDate: String = "",
    val totalAmount: Double = 0.0,
    val vatAmount: Double? = null,
    val vatRate: Double? = null,
    val category: String = "Iné",
    val description: String? = null,
    val imageUrl: String? = null,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class UserStats(
    val userId: String = "",
    val credits: Int = 10,
    val isSubscribed: Boolean = false,
    val totalInvoiced: Double = 0.0,
    val invoiceCount: Int = 0
)
