package se.elnor.elprisnu.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import se.elnor.elprisnu.notification.PriceCheckWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Persistent storage for user settings using AndroidX DataStore. The settings are stored in
 * preferences and exposed as a flow of [AlertSettings].
 */
class SettingsRepository(private val context: Context) {

    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val KEY_HIGH_PRICE = doublePreferencesKey("high_price")
    private val KEY_LOW_PRICE = doublePreferencesKey("low_price")
    private val KEY_ENABLED = booleanPreferencesKey("alerts_enabled")
    private val KEY_SHOW_VAT = booleanPreferencesKey("show_vat")
    private val KEY_REGION = stringPreferencesKey("selected_region")

    /** Returns a flow emitting the current [AlertSettings]. Defaults are used if not set. */
    val settingsFlow: Flow<AlertSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw exception
        }
        .map { prefs ->
            AlertSettings(
                highPrice = prefs[KEY_HIGH_PRICE] ?: 200.0,
                lowPrice = prefs[KEY_LOW_PRICE] ?: 10.0,
                enabled = prefs[KEY_ENABLED] ?: false,
                showVAT = prefs[KEY_SHOW_VAT] ?: true
            )
        }

    /** Returns the saved region name, or SE3 as default */
    val regionFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[KEY_REGION] ?: "SE3"
        }

    /** Persist the provided [AlertSettings] into DataStore. */
    suspend fun updateSettings(settings: AlertSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HIGH_PRICE] = settings.highPrice
            prefs[KEY_LOW_PRICE] = settings.lowPrice
            prefs[KEY_ENABLED] = settings.enabled
            prefs[KEY_SHOW_VAT] = settings.showVAT
        }
    }

    /** Persist the selected region */
    suspend fun updateRegion(regionName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REGION] = regionName
        }
    }
}