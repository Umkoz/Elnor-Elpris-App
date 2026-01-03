package se.elnor.elprisnu.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import se.elnor.elprisnu.model.AnalysisState
import se.elnor.elprisnu.model.AlertSettings
import se.elnor.elprisnu.model.DayData
import se.elnor.elprisnu.model.PricePoint
import se.elnor.elprisnu.model.Region
import se.elnor.elprisnu.repository.AnalysisRepository
import se.elnor.elprisnu.repository.ElectricityRepository
import se.elnor.elprisnu.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Time range options for the price chart
 */
enum class TimeRange(val label: String, val days: Int) {
    TODAY("Idag", 1),
    WEEK("Vecka", 7),
    MONTH("Månad", 30),
    YEAR("År", 365)
}

/**
 * ViewModel orchestrating the home screen. It manages the selected region and date, loads
 * electricity price data, applies VAT if enabled, calculates current price information, and
 * performs analysis on demand.
 *
 * Features caching and background preloading for smooth user experience.
 */
class HomeViewModel(
    private val electricityRepository: ElectricityRepository,
    private val analysisRepository: AnalysisRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _region = MutableStateFlow(Region.SE3)
    val region: StateFlow<Region> = _region.asStateFlow()

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _timeRange = MutableStateFlow(TimeRange.TODAY)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    // Zoom level: 0.0 = full range, 1.0 = most zoomed in
    private val _zoomLevel = MutableStateFlow(0f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _alertSettings = MutableStateFlow(AlertSettings())
    val alertSettings: StateFlow<AlertSettings> = _alertSettings.asStateFlow()

    private val _dayData = MutableStateFlow<DayData?>(null)
    val dayData: StateFlow<DayData?> = _dayData.asStateFlow()

    // Multi-day data for week/month/year views
    private val _multiDayData = MutableStateFlow<List<DayData>>(emptyList())
    val multiDayData: StateFlow<List<DayData>> = _multiDayData.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _analysisState = MutableStateFlow(AnalysisState())
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    // Cache for preloaded data (per region)
    private data class CacheKey(val region: Region, val timeRange: TimeRange)
    private val dataCache = mutableMapOf<CacheKey, List<DayData>>()
    private val cacheLoadingState = mutableMapOf<CacheKey, Boolean>()

    init {
        // Observe settings changes
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { _alertSettings.value = it }
        }
        // Load saved region
        viewModelScope.launch {
            settingsRepository.regionFlow.collect { regionName ->
                val newRegion = Region.fromName(regionName)
                if (_region.value != newRegion) {
                    _region.value = newRegion
                    // Clear cache when region changes
                    dataCache.clear()
                    // Preload for new region
                    preloadAllRanges(newRegion)
                }
            }
        }
        // Initial load
        loadData()
        // Preload other time ranges in background
        viewModelScope.launch {
            preloadAllRanges(_region.value)
        }
        // Reload whenever date, region, or time range changes
        viewModelScope.launch {
            combine(_region, _date, _timeRange) { r, d, t -> Triple(r, d, t) }
                .collect { (_, _, _) -> loadData() }
        }
    }

    /** Preload data for all time ranges in background */
    private fun preloadAllRanges(region: Region) {
        viewModelScope.launch(Dispatchers.IO) {
            val zoneId = ZoneId.of("Europe/Stockholm")
            val today = LocalDate.now(zoneId)

            // Preload in order of likely usage
            listOf(TimeRange.TODAY, TimeRange.WEEK, TimeRange.MONTH, TimeRange.YEAR).forEach { range ->
                val cacheKey = CacheKey(region, range)

                // Skip if already cached or currently loading
                if (dataCache.containsKey(cacheKey) || cacheLoadingState[cacheKey] == true) {
                    return@forEach
                }

                cacheLoadingState[cacheKey] = true
                Log.d(TAG, "Preloading ${range.name} for $region...")

                try {
                    val data = when (range) {
                        TimeRange.TODAY -> {
                            val dayData = electricityRepository.getElectricityPrices(region, today)
                            listOf(dayData)
                        }
                        TimeRange.WEEK -> {
                            val dataList = loadDaysInRange(region, today.minusDays(6), today)
                            aggregateToDailyAverages(dataList)
                        }
                        TimeRange.MONTH -> {
                            val dataList = loadDaysInRange(region, today.minusDays(29), today)
                            aggregateToDailyAverages(dataList)
                        }
                        TimeRange.YEAR -> {
                            loadMonthlyRepresentatives(region, today)
                        }
                    }
                    dataCache[cacheKey] = data
                    Log.d(TAG, "Preloaded ${range.name}: ${data.size} items")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to preload ${range.name}: ${e.message}")
                } finally {
                    cacheLoadingState[cacheKey] = false
                }
            }
        }
    }

    /** Update the selected region and persist to DataStore. */
    fun setRegion(r: Region) {
        _region.value = r
        viewModelScope.launch {
            settingsRepository.updateRegion(r.name)
        }
    }

    /** Update the selected time range. */
    fun setTimeRange(range: TimeRange) {
        Log.d(TAG, "Changing time range from ${_timeRange.value} to $range")

        // IMPORTANT: When switching from TODAY to a multi-day view,
        // reset the date to today to avoid issues with future dates
        if (_timeRange.value == TimeRange.TODAY && range != TimeRange.TODAY) {
            val today = LocalDate.now(ZoneId.of("Europe/Stockholm"))
            Log.d(TAG, "Resetting date to today: $today")
            _date.value = today
        }

        _timeRange.value = range
        _zoomLevel.value = 0f // Reset zoom when changing range
    }

    /** Update zoom level (0.0 to 1.0) */
    fun setZoomLevel(level: Float) {
        _zoomLevel.value = level.coerceIn(0f, 1f)
    }

    /** Move date forward or backward by one day. Only valid in TODAY mode. */
    fun changeDate(deltaDays: Long) {
        if (_timeRange.value != TimeRange.TODAY) {
            Log.w(TAG, "changeDate called in non-TODAY mode, ignoring")
            return
        }

        val newDate = _date.value.plusDays(deltaDays)
        val zoneId = ZoneId.of("Europe/Stockholm")
        val today = LocalDate.now(zoneId)
        val now = java.time.ZonedDateTime.now(zoneId)

        // Calculate max allowed date (tomorrow only if after 13:00)
        var maxDate = today
        if (now.hour >= 13) {
            maxDate = maxDate.plusDays(1)
        }

        // Don't allow going beyond max date
        if (newDate.isAfter(maxDate)) {
            Log.w(TAG, "Cannot navigate to $newDate, max is $maxDate")
            return
        }

        _date.value = newDate
    }

    /** Toggle VAT display */
    fun toggleVAT() {
        val current = _alertSettings.value
        updateAlertSettings(current.copy(showVAT = !current.showVAT))
    }

    /** Persist updated alert settings. */
    fun updateAlertSettings(settings: AlertSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
            _alertSettings.value = settings
        }
    }

    /** Trigger analysis using the analysis repository. */
    fun analyze() {
        val data = processedDayData ?: return
        viewModelScope.launch {
            _analysisState.value = AnalysisState(loading = true, content = null, error = null)
            try {
                val result = analysisRepository.analyzeElectricityPrices(data, region.value)
                _analysisState.value = AnalysisState(loading = false, content = result, error = null)
            } catch (e: Exception) {
                _analysisState.value = AnalysisState(loading = false, content = null, error = e.message)
            }
        }
    }

    /** Perform remote or mock data load depending on date and region. */
    private fun loadData() {
        viewModelScope.launch {
            _error.value = null
            _analysisState.value = AnalysisState()

            val zoneId = ZoneId.of("Europe/Stockholm")
            val today = LocalDate.now(zoneId)
            val currentRegion = region.value
            val currentRange = _timeRange.value

            // Check cache first (except for TODAY with specific date navigation)
            val cacheKey = CacheKey(currentRegion, currentRange)
            val cachedData = dataCache[cacheKey]

            if (cachedData != null && currentRange != TimeRange.TODAY) {
                Log.d(TAG, "Using cached data for ${currentRange.name}")
                _multiDayData.value = cachedData
                _dayData.value = cachedData.lastOrNull()
                _loading.value = false
                return@launch
            }

            _loading.value = true

            try {
                val data = when (currentRange) {
                    TimeRange.TODAY -> {
                        Log.d(TAG, "Loading TODAY data for ${_date.value}")
                        val dayData = electricityRepository.getElectricityPrices(currentRegion, _date.value)
                        _dayData.value = dayData
                        listOf(dayData)
                    }
                    TimeRange.WEEK -> {
                        Log.d(TAG, "Loading WEEK data (7 days)")
                        val dataList = loadDaysInRange(currentRegion, today.minusDays(6), today)
                        val aggregated = aggregateToDailyAverages(dataList)
                        _dayData.value = dataList.lastOrNull()
                        aggregated
                    }
                    TimeRange.MONTH -> {
                        Log.d(TAG, "Loading MONTH data (30 days)")
                        val dataList = loadDaysInRange(currentRegion, today.minusDays(29), today)
                        val aggregated = aggregateToDailyAverages(dataList)
                        _dayData.value = dataList.lastOrNull()
                        aggregated
                    }
                    TimeRange.YEAR -> {
                        Log.d(TAG, "Loading YEAR data (12 months)")
                        val monthlyData = loadMonthlyRepresentatives(currentRegion, today)
                        _dayData.value = monthlyData.lastOrNull()
                        monthlyData
                    }
                }

                _multiDayData.value = data
                // Cache the result
                dataCache[cacheKey] = data

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data: ${e.message}", e)
                _error.value = e.message
                _dayData.value = null
                _multiDayData.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    /** Load data for a range of days */
    private suspend fun loadDaysInRange(region: Region, startDate: LocalDate, endDate: LocalDate): List<DayData> {
        val dataList = mutableListOf<DayData>()
        var currentDate = startDate
        var successCount = 0
        var errorCount = 0

        while (!currentDate.isAfter(endDate)) {
            try {
                val data = electricityRepository.getElectricityPrices(region, currentDate)
                dataList.add(data)
                successCount++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load data for $currentDate: ${e.message}")
                errorCount++
            }
            currentDate = currentDate.plusDays(1)
        }

        Log.d(TAG, "Loaded $successCount days, $errorCount errors")
        return dataList.sortedBy { it.date }
    }

    /** Aggregate raw day data to single daily averages */
    private fun aggregateToDailyAverages(dataList: List<DayData>): List<DayData> {
        return dataList.map { dayData ->
            val dailyAvg = dayData.average
            DayData(
                date = dayData.date,
                prices = listOf(PricePoint(hour = 0, price = dailyAvg)),
                average = dailyAvg,
                min = dayData.min,
                max = dayData.max
            )
        }
    }

    /** Load one representative day per month for yearly view */
    private suspend fun loadMonthlyRepresentatives(region: Region, today: LocalDate): List<DayData> {
        val monthlyData = mutableListOf<DayData>()

        for (monthsAgo in 11 downTo 0) {
            val targetMonth = today.minusMonths(monthsAgo.toLong())
            val targetDate = targetMonth.withDayOfMonth(
                minOf(15, targetMonth.lengthOfMonth())
            )

            try {
                val data = electricityRepository.getElectricityPrices(region, targetDate)
                monthlyData.add(
                    DayData(
                        date = targetDate.toString(),
                        prices = listOf(PricePoint(hour = monthsAgo, price = data.average)),
                        average = data.average,
                        min = data.min,
                        max = data.max
                    )
                )
                Log.d(TAG, "Loaded month ${targetMonth.month}: avg=${data.average}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load data for ${targetMonth.month}: ${e.message}")
            }
        }

        return monthlyData
    }

    /** Returns the processed DayData applying VAT if enabled. */
    val processedDayData: DayData?
        get() {
            val raw = dayData.value ?: return null
            val multiplier = if (alertSettings.value.showVAT) 1.25 else 1.0
            val prices = raw.prices.map { it.copy(price = kotlin.math.round(it.price * multiplier * 100) / 100) }
            val average = kotlin.math.round(raw.average * multiplier * 100) / 100
            val min = kotlin.math.round(raw.min * multiplier * 100) / 100
            val max = kotlin.math.round(raw.max * multiplier * 100) / 100
            return raw.copy(prices = prices, average = average, min = min, max = max)
        }

    /** Returns processed multi-day data with VAT applied if enabled */
    val processedMultiDayData: List<DayData>
        get() {
            val multiplier = if (alertSettings.value.showVAT) 1.25 else 1.0
            return multiDayData.value.map { raw ->
                val prices = raw.prices.map { it.copy(price = kotlin.math.round(it.price * multiplier * 100) / 100) }
                val average = kotlin.math.round(raw.average * multiplier * 100) / 100
                val min = kotlin.math.round(raw.min * multiplier * 100) / 100
                val max = kotlin.math.round(raw.max * multiplier * 100) / 100
                raw.copy(prices = prices, average = average, min = min, max = max)
            }
        }

    /** Get all price points for the selected time range, respecting zoom level */
    val zoomedPricePoints: List<PricePoint>
        get() {
            val allData = processedMultiDayData
            if (allData.isEmpty()) return emptyList()

            // For aggregated data (WEEK/MONTH/YEAR), each DayData has only 1 price point
            // For TODAY, we have 96 quarterly points
            val isAggregated = _timeRange.value != TimeRange.TODAY

            val allPoints = if (isAggregated) {
                // For aggregated views, create one point per day/month
                allData.mapIndexed { index, dayData ->
                    PricePoint(hour = index, price = dayData.average)
                }
            } else {
                // For TODAY view, use all quarterly data
                val pointsPerDay = allData.firstOrNull()?.prices?.size ?: 96
                allData.flatMapIndexed { dayIndex, dayData ->
                    dayData.prices.map { point ->
                        PricePoint(hour = dayIndex * pointsPerDay + point.hour, price = point.price)
                    }
                }
            }

            // No zoom applied
            if (_zoomLevel.value == 0f) {
                return allPoints
            }

            // Apply zoom: show subset of data (sliding window from end)
            val totalPoints = allPoints.size
            val minPoints = when (_timeRange.value) {
                TimeRange.TODAY -> 12  // Minimum 3 hours (12 x 15min)
                TimeRange.WEEK -> 3    // Minimum 3 days
                TimeRange.MONTH -> 7   // Minimum 1 week
                TimeRange.YEAR -> 3    // Minimum 3 months
            }

            if (totalPoints <= minPoints) {
                return allPoints
            }

            val pointsToShow = (totalPoints - (totalPoints - minPoints) * _zoomLevel.value).toInt()
                .coerceIn(minPoints, totalPoints)

            return allPoints.takeLast(pointsToShow)
        }

    /** Calculate statistics for the visible (zoomed) data */
    val zoomedStats: Triple<Double, Double, Double>
        get() {
            val points = zoomedPricePoints
            if (points.isEmpty()) return Triple(0.0, 0.0, 0.0)

            val prices = points.map { it.price }
            val avg = kotlin.math.round(prices.average() * 100) / 100
            val min = prices.minOrNull() ?: 0.0
            val max = prices.maxOrNull() ?: 0.0

            return Triple(avg, min, max)
        }

    /** Returns the current hour/quarter price only if viewing today. */
    val currentPricePoint: PricePoint?
        get() {
            if (_timeRange.value != TimeRange.TODAY) return null
            val data = processedDayData ?: return null

            val zoneId = ZoneId.of("Europe/Stockholm")
            val today = LocalDate.now(zoneId)
            if (_date.value != today) return null

            val now = LocalDateTime.now(zoneId)
            val currentHour = now.hour
            val currentMinute = now.minute

            // For quarterly prices (96 per day), find the current quarter
            // For hourly prices (24 per day), find the current hour
            return if (data.prices.size > 24) {
                // Quarterly prices: calculate quarter index
                val quarterIndex = currentHour * 4 + (currentMinute / 15)
                data.prices.find { it.hour == quarterIndex } ?: data.prices.firstOrNull()
            } else {
                // Hourly prices
                data.prices.find { it.hour == currentHour } ?: data.prices.firstOrNull()
            }
        }

    /** Indicates whether the user can increment the date (disabled if at max available date). */
    val isNextDayDisabled: Boolean
        get() {
            if (_timeRange.value != TimeRange.TODAY) return true

            val zoneId = ZoneId.of("Europe/Stockholm")
            val now = java.time.ZonedDateTime.now(zoneId)
            val todayStart = LocalDate.now(zoneId)
            var maxDate = todayStart
            if (now.hour >= 13) {
                maxDate = maxDate.plusDays(1)
            }
            return _date.value >= maxDate
        }
}