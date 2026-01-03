package com.example.elpriscompose.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elpriscompose.model.Region
import com.example.elpriscompose.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Converts a quarter index (0-95) to a time string like "14:15"
 */
fun quarterIndexToTime(index: Int): String {
    val hour = (index / 4) % 24
    val minute = (index % 4) * 15
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/**
 * Converts a quarter index to a time range string like "14:15-14:30"
 */
fun quarterIndexToTimeRange(index: Int): String {
    val startHour = (index / 4) % 24
    val startMinute = (index % 4) * 15
    val endMinute = startMinute + 15
    val endHour = if (endMinute >= 60) (startHour + 1) % 24 else startHour
    val endMin = endMinute % 60

    return "${startHour.toString().padStart(2, '0')}:${startMinute.toString().padStart(2, '0')}-${endHour.toString().padStart(2, '0')}:${endMin.toString().padStart(2, '0')}"
}

/**
 * Determines if data is quarterly (96 points) or hourly (24 points)
 */
fun isQuarterlyData(pointCount: Int): Boolean = pointCount > 24

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val dayData by viewModel.dayData.collectAsState()
    val region by viewModel.region.collectAsState()
    val date by viewModel.date.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val alertSettings by viewModel.alertSettings.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header with region selector
        HeaderWithRegionSelector(
            region = region,
            onRegionChange = { viewModel.setRegion(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date navigation (only for "Today" view)
        if (timeRange == TimeRange.TODAY) {
            DateHeader(
                date = date,
                onPrevious = { viewModel.changeDate(-1) },
                onNext = { viewModel.changeDate(1) },
                isNextDisabled = viewModel.isNextDayDisabled
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Current price card (only for today view)
        if (timeRange == TimeRange.TODAY) {
            viewModel.currentPricePoint?.let { pricePoint ->
                val isQuarterly = dayData?.prices?.size?.let { isQuarterlyData(it) } ?: false
                CurrentPriceCard(
                    price = pricePoint.price,
                    quarterIndex = pricePoint.hour,
                    isQuarterly = isQuarterly,
                    showVAT = alertSettings.showVAT
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Loading indicator
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
        }

        // Error message
        error?.let { errorMsg ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StatRed.copy(alpha = 0.1f))
            ) {
                Text(
                    text = errorMsg,
                    color = StatRed,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats row - overview of period
        val stats = viewModel.zoomedStats
        StatsRow(
            average = stats.first,
            min = stats.second,
            max = stats.third
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Price chart section with time range tabs
        PriceChartSection(
            viewModel = viewModel,
            timeRange = timeRange,
            zoomLevel = zoomLevel,
            onTimeRangeChange = { viewModel.setTimeRange(it) },
            onZoomChange = { viewModel.setZoomLevel(it) },
            showVAT = alertSettings.showVAT,
            onToggleVAT = { viewModel.toggleVAT() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendations card
        RecommendationsCard(
            analysisState = analysisState,
            onAnalyze = { viewModel.analyze() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HeaderWithRegionSelector(
    region: Region,
    onRegionChange: (Region) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Elpris Nu",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Region dropdown
        Box {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { expanded = true }
                    .border(1.dp, BrandBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = region.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Välj region",
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Region.entries.forEach { r ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = r.label,
                                fontWeight = if (r == region) FontWeight.Bold else FontWeight.Normal,
                                color = if (r == region) BrandBlue else TextPrimary
                            )
                        },
                        onClick = {
                            onRegionChange(r)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateHeader(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isNextDisabled: Boolean
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("sv", "SE"))
    val dateText = if (date == LocalDate.now()) {
        "Idag, ${date.format(formatter)}"
    } else {
        date.format(formatter).replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Föregående dag",
                tint = BrandBlue
            )
        }

        Text(
            text = dateText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        IconButton(
            onClick = onNext,
            enabled = !isNextDisabled
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Nästa dag",
                tint = if (isNextDisabled) TextTertiary else BrandBlue
            )
        }
    }
}

@Composable
fun CurrentPriceCard(
    price: Double,
    quarterIndex: Int,
    isQuarterly: Boolean,
    showVAT: Boolean
) {
    // For quarterly data, show time range like "22:15-22:30"
    // For hourly data, show hour like "22:00"
    val timeText = if (isQuarterly) {
        quarterIndexToTimeRange(quarterIndex)
    } else {
        "${quarterIndex.toString().padStart(2, '0')}:00"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BrandBlue,
                            BrandBlue.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Just nu (kl $timeText)",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format("%.1f", price),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = " öre/kWh",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (showVAT) {
                    Text(
                        text = "inkl. moms",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "exkl. moms",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun PriceChartSection(
    viewModel: HomeViewModel,
    timeRange: TimeRange,
    zoomLevel: Float,
    onTimeRangeChange: (TimeRange) -> Unit,
    onZoomChange: (Float) -> Unit,
    showVAT: Boolean,
    onToggleVAT: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title row with VAT toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prisutveckling",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                // VAT Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleVAT() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Moms",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showVAT,
                        onCheckedChange = { onToggleVAT() },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = TextTertiary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time range tabs
            TimeRangeTabs(
                selectedRange = timeRange,
                onRangeSelected = onTimeRangeChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Period indicator
            PeriodIndicator(timeRange = timeRange, zoomLevel = zoomLevel)

            Spacer(modifier = Modifier.height(16.dp))

            // Chart - pass whether data is quarterly and the time range
            val dayData by viewModel.dayData.collectAsState()
            val isQuarterly = dayData?.prices?.size?.let { isQuarterlyData(it) } ?: false
            val multiDayData by viewModel.multiDayData.collectAsState()

            PriceChart(
                viewModel = viewModel,
                timeRange = timeRange,
                multiDayData = multiDayData,
                isQuarterly = isQuarterly
            )

            // Zoom slider for all views
            Spacer(modifier = Modifier.height(16.dp))
            ZoomSlider(
                zoomLevel = zoomLevel,
                onZoomChange = onZoomChange,
                timeRange = timeRange
            )
        }
    }
}

@Composable
fun TimeRangeTabs(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BackgroundSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) BackgroundCard else Color.Transparent,
                    label = "tabBackground"
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRangeSelected(range) },
                    shape = RoundedCornerShape(8.dp),
                    color = backgroundColor,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Text(
                        text = range.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodIndicator(
    timeRange: TimeRange,
    zoomLevel: Float
) {
    val text = when (timeRange) {
        TimeRange.TODAY -> "24 timmar • 96 kvartar"
        TimeRange.WEEK -> {
            val days = (7 - (6 * zoomLevel)).toInt().coerceAtLeast(1)
            "Senaste $days ${if (days == 1) "dag" else "dagar"}"
        }
        TimeRange.MONTH -> {
            val days = (30 - (29 * zoomLevel)).toInt().coerceAtLeast(1)
            "Senaste $days ${if (days == 1) "dag" else "dagar"}"
        }
        TimeRange.YEAR -> {
            val days = (365 - (364 * zoomLevel)).toInt().coerceAtLeast(1)
            if (days >= 30) {
                val months = days / 30
                "Senaste $months ${if (months == 1) "månad" else "månader"}"
            } else {
                "Senaste $days ${if (days == 1) "dag" else "dagar"}"
            }
        }
    }

    Text(
        text = text,
        fontSize = 12.sp,
        color = TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ZoomSlider(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    timeRange: TimeRange
) {
    val (leftLabel, rightLabel) = when (timeRange) {
        TimeRange.TODAY -> "Hela dagen" to "Senaste timmarna"
        TimeRange.WEEK -> "Hela veckan" to "Senaste dagarna"
        TimeRange.MONTH -> "Hela månaden" to "Senaste veckan"
        TimeRange.YEAR -> "Hela året" to "Senaste månaderna"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = leftLabel,
                fontSize = 11.sp,
                color = TextTertiary
            )
            Text(
                text = rightLabel,
                fontSize = 11.sp,
                color = TextTertiary
            )
        }
        Slider(
            value = zoomLevel,
            onValueChange = onZoomChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = BrandBlue,
                activeTrackColor = BrandBlue,
                inactiveTrackColor = BrandBlue.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun PriceChart(
    viewModel: HomeViewModel,
    timeRange: TimeRange,
    multiDayData: List<com.example.elpriscompose.model.DayData>,
    isQuarterly: Boolean = false
) {
    val pricePoints = viewModel.zoomedPricePoints
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Reset selection when time range changes or data changes
    LaunchedEffect(timeRange, pricePoints.size) {
        selectedIndex = null
    }

    if (pricePoints.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Laddar data...",
                color = TextTertiary
            )
        }
        return
    }

    val prices = pricePoints.map { it.price.toFloat() }
    val minPrice = (prices.minOrNull() ?: 0f) * 0.9f
    val maxPrice = (prices.maxOrNull() ?: 100f) * 1.1f
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1f)
    val avgPrice = prices.average().toFloat()

    // Calculate zoom offset for aggregated views
    // When zoomed, we show the LAST N items, so we need to offset into multiDayData
    val zoomOffset = if (timeRange != TimeRange.TODAY) {
        (multiDayData.size - pricePoints.size).coerceAtLeast(0)
    } else {
        0
    }

    // Swedish month names
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Maj", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dec")
    val weekDayNames = listOf("Mån", "Tis", "Ons", "Tor", "Fre", "Lör", "Sön")

    Column {
        // Selected point info box
        if (selectedIndex != null) {
            val idx = selectedIndex!!
            val point = pricePoints.getOrNull(idx)
            if (point != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val timeText = when (timeRange) {
                                TimeRange.TODAY -> {
                                    if (isQuarterly) {
                                        quarterIndexToTimeRange(point.hour)
                                    } else {
                                        "Kl ${point.hour.toString().padStart(2, '0')}:00"
                                    }
                                }
                                TimeRange.WEEK -> {
                                    val dataIdx = idx + zoomOffset
                                    val dayData = multiDayData.getOrNull(dataIdx)
                                    if (dayData != null) {
                                        try {
                                            val date = LocalDate.parse(dayData.date)
                                            val dayOfWeek = weekDayNames[(date.dayOfWeek.value - 1) % 7]
                                            "$dayOfWeek ${date.dayOfMonth}/${date.monthValue}"
                                        } catch (e: Exception) {
                                            "Dag ${idx + 1}"
                                        }
                                    } else "Dag ${idx + 1}"
                                }
                                TimeRange.MONTH -> {
                                    val dataIdx = idx + zoomOffset
                                    val dayData = multiDayData.getOrNull(dataIdx)
                                    if (dayData != null) {
                                        try {
                                            val date = LocalDate.parse(dayData.date)
                                            "${date.dayOfMonth}/${date.monthValue}"
                                        } catch (e: Exception) {
                                            "Dag ${idx + 1}"
                                        }
                                    } else "Dag ${idx + 1}"
                                }
                                TimeRange.YEAR -> {
                                    val dataIdx = idx + zoomOffset
                                    val dayData = multiDayData.getOrNull(dataIdx)
                                    if (dayData != null) {
                                        try {
                                            val date = LocalDate.parse(dayData.date)
                                            "${monthNames[date.monthValue - 1]} ${date.year}"
                                        } catch (e: Exception) {
                                            "Månad ${idx + 1}"
                                        }
                                    } else "Månad ${idx + 1}"
                                }
                            }
                            Text(
                                text = timeText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            val hintText = when (timeRange) {
                                TimeRange.TODAY -> "Tryck på grafen för annan tid"
                                TimeRange.WEEK, TimeRange.MONTH -> "Dagligt snittpris"
                                TimeRange.YEAR -> "Månadens representativa pris"
                            }
                            Text(
                                text = hintText,
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${String.format("%.1f", point.price)} öre",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            val diffFromAvg = point.price - avgPrice
                            val diffText = if (diffFromAvg >= 0) "+${String.format("%.1f", diffFromAvg)}" else String.format("%.1f", diffFromAvg)
                            Text(
                                text = "$diffText vs snitt",
                                fontSize = 11.sp,
                                color = if (diffFromAvg >= 0) StatRed else StatGreen
                            )
                        }
                    }
                }
            }
        } else {
            // Hint text varies by time range
            val hintText = when (timeRange) {
                TimeRange.TODAY -> "💡 Tryck på grafen för att se pris för en specifik tid"
                TimeRange.WEEK -> "💡 Tryck på grafen för att se dagligt snittpris"
                TimeRange.MONTH -> "💡 Tryck för att se snittpris per dag"
                TimeRange.YEAR -> "💡 Tryck för att se månadens pris"
            }
            Text(
                text = hintText,
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(pricePoints) {
                    detectTapGestures { offset ->
                        val padding = 50f
                        val chartWidth = size.width - padding * 2

                        val relativeX = (offset.x - padding).coerceIn(0f, chartWidth)
                        val tappedIndex = ((relativeX / chartWidth) * (prices.size - 1)).toInt()
                            .coerceIn(0, prices.size - 1)

                        selectedIndex = tappedIndex
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val padding = 50f
            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            // Draw average line (dashed)
            val avgY = height - padding - ((avgPrice - minPrice) / priceRange) * chartHeight
            val dashLength = 10f
            var x = padding
            while (x < width - padding) {
                drawLine(
                    color = TextTertiary.copy(alpha = 0.5f),
                    start = Offset(x, avgY),
                    end = Offset((x + dashLength).coerceAtMost(width - padding), avgY),
                    strokeWidth = 1f
                )
                x += dashLength * 2
            }

            // Build path for price line
            val path = Path()
            val areaPath = Path()

            prices.forEachIndexed { index, price ->
                val xPos = padding + (index.toFloat() / (prices.size - 1).coerceAtLeast(1)) * chartWidth
                val yPos = height - padding - ((price - minPrice) / priceRange) * chartHeight

                if (index == 0) {
                    path.moveTo(xPos, yPos)
                    areaPath.moveTo(xPos, height - padding)
                    areaPath.lineTo(xPos, yPos)
                } else {
                    path.lineTo(xPos, yPos)
                    areaPath.lineTo(xPos, yPos)
                }
            }

            // Close area path
            areaPath.lineTo(padding + chartWidth, height - padding)
            areaPath.close()

            // Draw filled area
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BrandBlue.copy(alpha = 0.3f),
                        BrandBlue.copy(alpha = 0.05f)
                    )
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = BrandBlue,
                style = Stroke(width = 3f)
            )

            // Draw selected point indicator
            selectedIndex?.let { idx ->
                val selectedPrice = prices.getOrNull(idx) ?: return@let
                val xPos = padding + (idx.toFloat() / (prices.size - 1).coerceAtLeast(1)) * chartWidth
                val yPos = height - padding - ((selectedPrice - minPrice) / priceRange) * chartHeight

                drawLine(
                    color = BrandBlue.copy(alpha = 0.5f),
                    start = Offset(xPos, padding),
                    end = Offset(xPos, height - padding),
                    strokeWidth = 1.5f
                )

                drawCircle(
                    color = BrandBlue.copy(alpha = 0.3f),
                    radius = 16f,
                    center = Offset(xPos, yPos)
                )

                drawCircle(
                    color = BrandBlue,
                    radius = 8f,
                    center = Offset(xPos, yPos)
                )

                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(xPos, yPos)
                )
            }

            // Draw Y-axis labels
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 28f
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText("${maxPrice.toInt()}", padding - 8, padding + 10, paint)
                drawText("${minPrice.toInt()}", padding - 8, height - padding, paint)
                drawText("${avgPrice.toInt()}", padding - 8, avgY + 5, paint)
            }

            // Draw X-axis labels based on time range
            val xPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 22f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val labelCount = when (timeRange) {
                TimeRange.TODAY -> 6
                TimeRange.WEEK -> minOf(7, prices.size)
                TimeRange.MONTH -> 6
                TimeRange.YEAR -> minOf(6, prices.size)
            }

            for (i in 0 until labelCount.coerceAtMost(prices.size)) {
                val index = if (prices.size <= labelCount) i else (i * (prices.size - 1) / (labelCount - 1).coerceAtLeast(1))
                val xPos = padding + (index.toFloat() / (prices.size - 1).coerceAtLeast(1)) * chartWidth

                val label = when (timeRange) {
                    TimeRange.TODAY -> {
                        val originalIndex = pricePoints.getOrNull(index)?.hour ?: 0
                        if (isQuarterly) {
                            val hour = (originalIndex / 4) % 24
                            "${hour.toString().padStart(2, '0')}"
                        } else {
                            "${(originalIndex % 24).toString().padStart(2, '0')}"
                        }
                    }
                    TimeRange.WEEK -> {
                        val dataIdx = index + zoomOffset
                        val dayData = multiDayData.getOrNull(dataIdx)
                        if (dayData != null) {
                            try {
                                val date = LocalDate.parse(dayData.date)
                                weekDayNames[(date.dayOfWeek.value - 1) % 7]
                            } catch (e: Exception) { "?" }
                        } else "?"
                    }
                    TimeRange.MONTH -> {
                        val dataIdx = index + zoomOffset
                        val dayData = multiDayData.getOrNull(dataIdx)
                        if (dayData != null) {
                            try {
                                val date = LocalDate.parse(dayData.date)
                                "${date.dayOfMonth}"
                            } catch (e: Exception) { "?" }
                        } else "?"
                    }
                    TimeRange.YEAR -> {
                        val dataIdx = index + zoomOffset
                        val dayData = multiDayData.getOrNull(dataIdx)
                        if (dayData != null) {
                            try {
                                val date = LocalDate.parse(dayData.date)
                                monthNames[date.monthValue - 1]
                            } catch (e: Exception) { "?" }
                        } else "?"
                    }
                }

                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    xPos,
                    height - 8,
                    xPaint
                )
            }
        }
    }
}

@Composable
fun StatsRow(
    average: Double,
    min: Double,
    max: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "SNITT",
            value = average,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "LÄGST",
            value = min,
            color = StatGreen,
            modifier = Modifier.weight(1f),
            showArrow = true,
            arrowDown = true
        )
        StatCard(
            label = "HÖGST",
            value = max,
            color = StatRed,
            modifier = Modifier.weight(1f),
            showArrow = true,
            arrowDown = false
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
    showArrow: Boolean = false,
    arrowDown: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextTertiary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showArrow) {
                    Text(
                        text = if (arrowDown) "↓" else "↑",
                        fontSize = 16.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = String.format("%.1f", value),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun RecommendationsCard(
    analysisState: com.example.elpriscompose.model.AnalysisState,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Rekommendationer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Utifrån dagens prisbild",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                analysisState.loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BrandBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                analysisState.content != null -> {
                    Text(
                        text = analysisState.content,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
                analysisState.error != null -> {
                    Text(
                        text = "Fel: ${analysisState.error}",
                        fontSize = 14.sp,
                        color = StatRed
                    )
                }
                else -> {
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue
                        )
                    ) {
                        Text(
                            text = "Visa rekommendationer",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}