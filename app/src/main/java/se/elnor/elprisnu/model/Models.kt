package se.elnor.elprisnu.model

/**
 * Swedish electricity price regions.
 * The enum name (SE1, SE2, etc.) is used directly in API calls.
 */
enum class Region(val label: String) {
    SE1("Norra Sverige (SE1)"),
    SE2("Norra Mellansverige (SE2)"),
    SE3("Södra Mellansverige (SE3)"),
    SE4("Södra Sverige (SE4)");

    companion object {
        fun fromName(name: String): Region = entries.find { it.name == name } ?: SE3
    }
}

/**
 * Represents a single hourly price point
 */
data class PricePoint(
    val hour: Int,
    val price: Double
)

/**
 * Contains price data for a single day
 */
data class DayData(
    val date: String,
    val prices: List<PricePoint>,
    val average: Double,
    val min: Double,
    val max: Double
)

/**
 * Settings for price alerts and display options
 */
data class AlertSettings(
    val highPrice: Double = 200.0,
    val lowPrice: Double = 10.0,
    val enabled: Boolean = false,
    val showVAT: Boolean = true
)

/**
 * State for AI analysis
 */
data class AnalysisState(
    val loading: Boolean = false,
    val content: String? = null,
    val error: String? = null
)