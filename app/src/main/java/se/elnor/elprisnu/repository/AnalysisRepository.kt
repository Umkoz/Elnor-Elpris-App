package se.elnor.elprisnu.repository

import se.elnor.elprisnu.model.DayData
import se.elnor.elprisnu.model.Region
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for electricity price analysis.
 * Uses rule-based analysis to find optimal times for electricity usage.
 */
class AnalysisRepository {

    /**
     * Analyzes electricity prices and provides recommendations.
     *
     * @param data The price data to analyze
     * @param region The electricity region
     */
    suspend fun analyzeElectricityPrices(
        data: DayData,
        region: Region
    ): String = withContext(Dispatchers.Default) {
        val prices = data.prices
        if (prices.isEmpty()) return@withContext "Ingen data tillgänglig för analys."

        // For quarterly data, we need to find the best hours (not quarters)
        val isQuarterly = prices.size > 24

        val hourlyPrices = if (isQuarterly) {
            // Aggregate quarters to hours
            (0 until 24).map { hour ->
                val quarterPrices = prices.filter { it.hour / 4 == hour }.map { it.price }
                hour to (quarterPrices.average())
            }
        } else {
            prices.map { it.hour to it.price }
        }

        // Find cheapest and most expensive hours
        val sortedByPrice = hourlyPrices.sortedBy { it.second }
        val cheapestHours = sortedByPrice.take(3).map { it.first }
        val expensiveHours = sortedByPrice.takeLast(3).map { it.first }

        fun formatHours(list: List<Int>): String =
            list.sorted().joinToString(", ") { h -> "${h.toString().padStart(2, '0')}:00" }

        val cheapestStr = formatHours(cheapestHours)
        val expensiveStr = formatHours(expensiveHours)

        val priceDiff = data.max - data.min
        val savingTip = when {
            priceDiff > 100 -> "Stor prisskillnad idag – du kan spara mycket genom att flytta förbrukning!"
            priceDiff > 50 -> "Betydande prisskillnad – det lönar sig att planera användningen."
            priceDiff > 20 -> "Viss besparingspotential finns."
            else -> "Priserna är relativt jämna idag."
        }

        val chargingAdvice = if (cheapestHours.any { it in 0..6 }) {
            "\n\n🔌 Tips: Bästa tiden för elbilsladdning är på natten/tidig morgon."
        } else if (cheapestHours.any { it in 12..15 }) {
            "\n\n🔌 Tips: Mitt på dagen kan vara bra för laddning idag."
        } else {
            ""
        }

        return@withContext "⚡ Bästa timmarna för tunga apparater:\n$cheapestStr\n\n" +
                "⚠️ Undvik helst:\n$expensiveStr\n\n" +
                savingTip + chargingAdvice
    }
}