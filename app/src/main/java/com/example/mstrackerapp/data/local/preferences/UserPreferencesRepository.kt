package com.example.mstrackerapp.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private val PRIVACY_MASK_KEY = booleanPreferencesKey("is_privacy_masked")

    val isPrivacyMasked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PRIVACY_MASK_KEY] ?: false
    }

    suspend fun togglePrivacyMask() {
        context.dataStore.edit { prefs ->
            val current = prefs[PRIVACY_MASK_KEY] ?: false
            prefs[PRIVACY_MASK_KEY] = !current
        }
    }
}
