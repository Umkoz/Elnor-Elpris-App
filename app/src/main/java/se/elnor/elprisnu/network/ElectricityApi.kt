package se.elnor.elprisnu.network

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API definition for the elprisetjustnu.se API.
 * The API returns an array of objects containing the price in SEK per kWh
 * and a start time stamp for each price point.
 *
 * Since October 2025, the API returns quarterly prices (96 per day)
 * instead of hourly prices (24 per day).
 */
interface ElectricityApi {
    @GET("{year}/{monthDay}_{zone}.json")
    suspend fun getPrices(
        @Path("year") year: String,
        @Path("monthDay") monthDay: String,
        @Path("zone") zone: String
    ): List<ApiPrice>
}

/**
 * Response model for the API.
 *
 * The API returns multiple fields, but we only need SEK_per_kWh and time_start.
 * Using KotlinJsonAdapterFactory (reflection-based) which ignores unknown fields.
 *
 * Example API response:
 * {
 *   "SEK_per_kWh": 0.51234,
 *   "EUR_per_kWh": 0.04567,
 *   "EXR": 11.234,
 *   "time_start": "2025-12-13T22:00:00+01:00",
 *   "time_end": "2025-12-13T23:00:00+01:00"
 * }
 */
data class ApiPrice(
    @Json(name = "SEK_per_kWh") val sekPerKwh: Double,
    @Json(name = "time_start") val timeStart: String
)