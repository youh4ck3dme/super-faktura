package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val supplierName: String,
    val ico: String?,
    val dic: String?,
    val issueDate: String,
    val totalAmount: Double,
    val vatAmount: Double?,
    val vatRate: Double?,
    val category: String,
    val description: String?,
    val imageUrl: String?,
    val userId: String,
    val createdAt: Long
) {
    fun toInvoice() = Invoice(
        id = id,
        supplierName = supplierName,
        ico = ico,
        dic = dic,
        issueDate = issueDate,
        totalAmount = totalAmount,
        vatAmount = vatAmount,
        vatRate = vatRate,
        category = category,
        description = description,
        imageUrl = imageUrl,
        userId = userId,
        createdAt = createdAt
    )
}

fun Invoice.toEntity() = InvoiceEntity(
    id = id,
    supplierName = supplierName,
    ico = ico,
    dic = dic,
    issueDate = issueDate,
    totalAmount = totalAmount,
    vatAmount = vatAmount,
    vatRate = vatRate,
    category = category,
    description = description,
    imageUrl = imageUrl,
    userId = userId,
    createdAt = createdAt
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val userId: String,
    val credits: Int,
    val isSubscribed: Boolean,
    val totalInvoiced: Double,
    val invoiceCount: Int
) {
    fun toUserStats() = UserStats(
        userId = userId,
        credits = credits,
        isSubscribed = isSubscribed,
        totalInvoiced = totalInvoiced,
        invoiceCount = invoiceCount
    )
}

fun UserStats.toEntity() = UserStatsEntity(
    userId = userId,
    credits = credits,
    isSubscribed = isSubscribed,
    totalInvoiced = totalInvoiced,
    invoiceCount = invoiceCount
)

// --- DAOs ---

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE userId = :userId ORDER BY createdAt DESC")
    fun getInvoicesForUser(userId: String): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: String)
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    suspend fun getUserStats(userId: String): UserStatsEntity?

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    fun getUserStatsFlow(userId: String): Flow<UserStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStatsEntity)
}

// --- AppDatabase ---

@Database(entities = [InvoiceEntity::class, UserStatsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bleskova_faktura_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
