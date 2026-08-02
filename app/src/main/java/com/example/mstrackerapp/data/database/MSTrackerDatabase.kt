package com.example.mstrackerapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mstrackerapp.data.database.dao.*
import com.example.mstrackerapp.data.database.entities.*
import com.example.mstrackerapp.data.database.migrations.Migrations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        MerchantEntity::class,
        MerchantMappingEntity::class,
        MerchantAliasEntity::class,
        UserLearnedMappingEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        SMSInboxEntity::class,
        SMSQueueEntity::class,
        RegexRuleEntity::class,
        SettingsEntity::class,
        TagEntity::class,
        AttachmentEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class MSTrackerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun merchantAliasDao(): MerchantAliasDao
    abstract fun userLearnedMappingDao(): UserLearnedMappingDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun smsQueueDao(): SMSQueueDao
    abstract fun smsInboxDao(): SMSInboxDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MSTrackerDatabase? = null

        fun getDatabase(context: Context): MSTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MSTrackerDatabase::class.java,
                    "mstracker_database"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING) // WAL Journal Mode Optimization
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }

            private suspend fun seedDatabase(db: MSTrackerDatabase) {
                // Seed Accounts
                db.accountDao().insertAccounts(
                    listOf(
                        AccountEntity("acc-1", "Bank Account", "BANK", "Bank", 0L, "🏦", includeInNetWorth = true, order = 1),
                        AccountEntity("acc-2", "Cash Wallet", "CASH", "Cash", 0L, "💵", includeInNetWorth = true, order = 2),
                        AccountEntity("acc-3", "Credit Card", "CREDIT_CARD", "Credit Card", 0L, "💳", includeInNetWorth = true, order = 3)
                    )
                )

                // Seed Default 14 Categories
                db.categoryDao().insertCategories(
                    listOf(
                        CategoryEntity("cat-1", "Food", "🍔", "#FF6B6B", 500000L, 1),
                        CategoryEntity("cat-2", "Transport", "🚗", "#4ECDC4", 300000L, 2),
                        CategoryEntity("cat-3", "Shopping", "🛍️", "#45B7D1", 400000L, 3),
                        CategoryEntity("cat-4", "Entertainment", "🎬", "#96CEB4", 200000L, 4),
                        CategoryEntity("cat-5", "Education", "🎓", "#FFEEAD", 250000L, 5),
                        CategoryEntity("cat-6", "Health", "🏥", "#D4A5A5", 150000L, 6),
                        CategoryEntity("cat-7", "Travel", "✈️", "#9B59B6", 600000L, 7),
                        CategoryEntity("cat-8", "Fuel", "⛽", "#E67E22", 350000L, 8),
                        CategoryEntity("cat-9", "Bills", "🧾", "#34495E", 450000L, 9),
                        CategoryEntity("cat-10", "Investment", "📈", "#2ECC71", 1000000L, 10),
                        CategoryEntity("cat-11", "Salary", "💼", "#27AE60", 0L, 11),
                        CategoryEntity("cat-12", "Refund", "🔄", "#16A085", 0L, 12),
                        CategoryEntity("cat-13", "Transfer", "⇆", "#7F8C8D", 0L, 13),
                        CategoryEntity("cat-14", "Other", "📦", "#BDC3C7", 0L, 14)
                    )
                )
            }
        }
    }
}
