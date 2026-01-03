package se.elnor.elprisnu.repository

import android.util.Log
import se.elnor.elprisnu.model.DayData
import se.elnor.elprisnu.model.PricePoint
import se.elnor.elprisnu.model.Region
import se.elnor.elprisnu.network.ApiPrice
import se.elnor.elprisnu.network.ElectricityApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

/**
 * Repository responsible for fetching electricity prices from the elprisetjustnu.se API.
 *
 * Data source: https://www.elprisetjustnu.se/elpris-api
 * Supports all Swedish regions: SE1, SE2, SE3, SE4
 *
 * IMPORTANT: Since October 1, 2025, the API returns quarterly prices (96 per day) instead
 * of hourly prices (24 per day). This repository handles both cases.
 */
class ElectricityRepository {
    private val api: ElectricityApi

    companion object {
        private const val TAG = "ElectricityRepo"
    }

    init {
        // Create logging interceptor to see raw API responses
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, "HTTP: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        // Configure Moshi to handle unknown fields gracefully
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.elprisetjustnu.se/api/v1/prices/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(ElectricityApi::class.java)
    }

    /**
     * Fetches electricity prices for the specified region and date.
     *
     * @param region The Swedish electricity region (SE1–SE4).
     * @param date The date of interest.
     * @return A [DayData] instance containing price points and summary stats.
     */
    suspend fun getElectricityPrices(region: Region, date: LocalDate): DayData = withContext(Dispatchers.IO) {
        val zoneId = ZoneId.of("Europe/Stockholm")
        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)

        Log.d(TAG, "=== getElectricityPrices called ===")
        Log.d(TAG, "Region: ${region.name}, Date: $date, Today: $today")

        // Reject requests beyond tomorrow
        if (date.isAfter(tomorrow)) {
            Log.e(TAG, "Date $date is after tomorrow - rejecting")
            throw IllegalArgumentException("Priser för detta datum är inte fastställda än.")
        }

        // Reject tomorrow before 13:00 Stockholm time
        if (date.isEqual(tomorrow)) {
            val nowHour = ZonedDateTime.now(zoneId).hour
            Log.d(TAG, "Requesting tomorrow's prices, current hour: $nowHour")
            if (nowHour < 13) {
                throw IllegalStateException("Morgondagens priser blir tillgängliga ca kl 13:00.")
            }
        }

        // Format path segments
        val year = date.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = date.format(DateTimeFormatter.ofPattern("MM"))
        val day = date.format(DateTimeFormatter.ofPattern("dd"))
        val monthDay = "$month-$day"
        val zone = region.name

        val url = "https://www.elprisetjustnu.se/api/v1/prices/$year/${monthDay}_${zone}.json"
        Log.d(TAG, ">>> Fetching from: $url")

        return@withContext try {
            val response: List<ApiPrice> = api.getPrices(year, monthDay, zone)
            Log.d(TAG, "<<< API Success: ${response.size} price points received")

            if (response.isEmpty()) {
                throw IllegalStateException("Tom data från leverantör")
            }

            // Log first few items for debugging
            response.take(3).forEachIndexed { index, item ->
                Log.d(TAG, "  [$index] time=${item.timeStart}, SEK/kWh=${item.sekPerKwh}")
            }

            // Determine if this is quarterly or hourly data
            val isQuarterly = response.size > 24
            Log.d(TAG, "Data type: ${if (isQuarterly) "Quarterly (${response.size} points)" else "Hourly (${response.size} points)"}")

            val points = response.mapNotNull { item ->
                try {
                    // Parse the time_start field
                    // Format: "2025-12-13T22:15:00+01:00" or "2025-12-13T22:00:00+01:00"
                    val timeStr = item.timeStart

                    // Extract hour and minute from the ISO timestamp
                    val hourStr = timeStr.substring(11, 13)
                    val minuteStr = timeStr.substring(14, 16)

                    val hour = hourStr.toIntOrNull() ?: 0
                    val minute = minuteStr.toIntOrNull() ?: 0

                    // Calculate index based on data type
                    val index = if (isQuarterly) {
                        // For quarterly: hour * 4 + quarter (0-3)
                        hour * 4 + (minute / 15)
                    } else {
                        hour
                    }

                    // Convert SEK/kWh to öre/kWh
                    // API returns e.g. 0.51 SEK = 51 öre
                    // TypeScript reference: item.SEK_per_kWh * 100
                    val priceOre = round(item.sekPerKwh * 100 * 100) / 100.0

                    PricePoint(index, priceOre)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse price point: ${item.timeStart}", e)
                    null
                }
            }.sortedBy { it.hour }

            if (points.isEmpty()) {
                throw IllegalStateException("Kunde inte tolka prisdata")
            }

            val values = points.map { it.price }
            val sum = values.sum()
            val avg = sum / values.size

            Log.d(TAG, "✓ Successfully parsed ${points.size} prices")
            Log.d(TAG, "✓ Stats - Avg: ${"%.2f".format(avg)} öre, Min: ${values.minOrNull()} öre, Max: ${values.maxOrNull()} öre")

            Log.d(TAG, "Returning ${points.size} price points (quarterly data preserved)")

            DayData(
                date = date.toString(),
                prices = points,
                average = round(avg * 100) / 100.0,
                min = values.minOrNull() ?: 0.0,
                max = values.maxOrNull() ?: 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "!!! API Error: ${e.javaClass.simpleName} - ${e.message}", e)

            // Don't use mock data in production - throw error so user knows data is unavailable
            throw IllegalStateException("Kunde inte hämta elpriser. Kontrollera internetanslutningen.", e)
        }
    }

    /**
     * Aggregates quarterly prices (96 points) to hourly averages (24 points)
     * for simpler chart display.
     */
    private fun aggregateToHourly(quarterlyPoints: List<PricePoint>): List<PricePoint> {
        return (0 until 24).map { hour ->
            val quarterStart = hour * 4
            val quarterEnd = quarterStart + 3

            val quarterPrices = quarterlyPoints.filter {
                it.hour in quarterStart..quarterEnd
            }.map { it.price }

            val avgPrice = if (quarterPrices.isNotEmpty()) {
                round(quarterPrices.average() * 100) / 100.0
            } else {
                0.0
            }

            PricePoint(hour, avgPrice)
        }
    }
}