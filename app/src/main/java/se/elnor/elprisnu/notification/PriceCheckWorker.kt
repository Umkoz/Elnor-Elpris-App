package se.elnor.elprisnu.notification

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import se.elnor.elprisnu.network.ElectricityApi
import se.elnor.elprisnu.repository.ElectricityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker that periodically checks electricity prices
 * and sends notifications when thresholds are crossed.
 */
class PriceCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "PriceCheckWorker"
        const val WORK_NAME = "price_check_work"

        // DataStore keys (matching SettingsRepository)
        private val Context.dataStore by preferencesDataStore(name = "settings")
        private val KEY_HIGH_PRICE = doublePreferencesKey("high_price")
        private val KEY_LOW_PRICE = doublePreferencesKey("low_price")
        private val KEY_ENABLED = booleanPreferencesKey("alerts_enabled")
        private val KEY_SHOW_VAT = booleanPreferencesKey("show_vat")
        private val KEY_REGION = stringPreferencesKey("selected_region")

        // Track last notification to avoid spam
        private val KEY_LAST_LOW_NOTIFICATION = stringPreferencesKey("last_low_notification")
        private val KEY_LAST_HIGH_NOTIFICATION = stringPreferencesKey("last_high_notification")

        /**
         * Schedules the periodic price check worker.
         * Runs every 15 minutes (minimum for WorkManager).
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<PriceCheckWorker>(
                15, TimeUnit.MINUTES  // Check every 15 minutes
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Don't replace if already running
                workRequest
            )

            Log.d(TAG, "Price check worker scheduled")
        }

        /**
         * Cancels the periodic price check worker.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Price check worker cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Price check worker running...")

        return try {
            // Read settings from DataStore
            val prefs = context.dataStore.data.first()

            val enabled = prefs[KEY_ENABLED] ?: false
            if (!enabled) {
                Log.d(TAG, "Alerts disabled, skipping check")
                return Result.success()
            }

            val highThreshold = prefs[KEY_HIGH_PRICE] ?: 200.0
            val lowThreshold = prefs[KEY_LOW_PRICE] ?: 10.0
            val showVAT = prefs[KEY_SHOW_VAT] ?: true
            val regionName = prefs[KEY_REGION] ?: "SE3"

            // Get current price from API
            val repository = ElectricityRepository()
            val now = LocalDateTime.now()
            val region = se.elnor.elprisnu.model.Region.fromName(regionName)

            val dayData = repository.getElectricityPrices(region, now.toLocalDate())

            if (dayData.prices.isEmpty()) {
                Log.w(TAG, "No price data available")
                return Result.retry()
            }

            // Find current price (handle both quarterly and hourly data)
            val currentHour = now.hour
            val currentMinute = now.minute

            val currentPrice = if (dayData.prices.size > 24) {
                // Quarterly data (96 points)
                val quarterIndex = currentHour * 4 + (currentMinute / 15)
                dayData.prices.find { it.hour == quarterIndex }?.price
            } else {
                // Hourly data (24 points)
                dayData.prices.find { it.hour == currentHour }?.price
            }

            if (currentPrice == null) {
                Log.w(TAG, "Could not find current price")
                return Result.retry()
            }

            // Apply VAT if enabled
            val priceToCheck = if (showVAT) currentPrice * 1.25 else currentPrice

            Log.d(TAG, "Current price: $priceToCheck öre (VAT: $showVAT), thresholds: low=$lowThreshold, high=$highThreshold")

            val notificationHelper = NotificationHelper(context)
            val currentTimeKey = "${now.toLocalDate()}-${now.hour}"

            // Check low price threshold
            if (priceToCheck <= lowThreshold) {
                val lastLowNotification = prefs[KEY_LAST_LOW_NOTIFICATION]
                if (lastLowNotification != currentTimeKey) {
                    Log.d(TAG, "Price below low threshold - sending notification")
                    notificationHelper.showLowPriceNotification(priceToCheck, lowThreshold, region.label)

                    // Update last notification time
                    context.dataStore.updateData { preferences ->
                        preferences.toMutablePreferences().apply {
                            set(KEY_LAST_LOW_NOTIFICATION, currentTimeKey)
                        }
                    }
                }
            }

            // Check high price threshold
            if (priceToCheck >= highThreshold) {
                val lastHighNotification = prefs[KEY_LAST_HIGH_NOTIFICATION]
                if (lastHighNotification != currentTimeKey) {
                    Log.d(TAG, "Price above high threshold - sending notification")
                    notificationHelper.showHighPriceNotification(priceToCheck, highThreshold, region.label)

                    // Update last notification time
                    context.dataStore.updateData { preferences ->
                        preferences.toMutablePreferences().apply {
                            set(KEY_LAST_HIGH_NOTIFICATION, currentTimeKey)
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking prices", e)
            Result.retry()
        }
    }
}