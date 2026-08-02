package com.example.mstrackerapp.worker

import android.content.Context
import com.example.mstrackerapp.data.database.MSTrackerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecurringPaymentWorker(private val context: Context) {

    suspend fun checkUpcomingBills() = withContext(Dispatchers.IO) {
        val db = MSTrackerDatabase.getDatabase(context)
        // Check recurring payments and generate due notifications
    }
}
