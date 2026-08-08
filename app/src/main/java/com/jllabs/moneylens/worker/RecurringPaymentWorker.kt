package com.jllabs.moneylens.worker

import android.content.Context
import com.jllabs.moneylens.data.database.MoneyLensDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecurringPaymentWorker(private val context: Context) {

    suspend fun checkUpcomingBills() = withContext(Dispatchers.IO) {
        val db = MoneyLensDatabase.getDatabase(context)
        // Check recurring payments and generate due notifications
    }
}
