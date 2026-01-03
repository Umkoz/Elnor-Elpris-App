package com.example.elpriscompose.ui.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// THEME COLORS
// ============================================================================
private object LightTheme {
    val pageBg = Color(0xFFF8FAFC)
    val uiCard = Color.White
    val uiBorder = Color(0xFFE2E8F0)
    val uiText = Color(0xFF1E293B)
    val uiTextMuted = Color(0xFF64748B)
    val accent = Color(0xFFF59E0B)
    val accentLight = Color(0xFFFBBF24)
    val progressBg = Color(0xFFE2E8F0)
    val floor = Color(0xFF0C1929)
    val floorLit = Color(0xFFFFF9EC)
    val floorLitCandle = Color(0xFFFFF0E0)
    val wall = Color(0xFF1A3352)
    val lampGlow = Color(0xFFFFDD88)
    val candleGlow = Color(0xFFFF9944)
    val success = Color(0xFF22C55E)
    val mirrorColor = Color(0xFF60A5FA)
}

// ============================================================================
// DATA CLASSES
// ============================================================================
data class Position(val x: Int, val y: Int)
data class Mirror(val x: Int, val y: Int, val dir: String)
data class PlacedCandle(val id: String, val x: Int, val y: Int)
data class AvailableCandle(val id: String, val timeRemaining: Float)

data class Level(
    val id: Int,
    val name: String,
    val description: String,
    val maxLamps: Int,
    val parLamps: Int,
    val targetPercent: Int,
    val walls: List<Position>,
    val mirrors: List<Mirror>,
    val candles: Int,
    val candleBurnTime: Float
)

private enum class Tool { LAMP, CANDLE }

// ============================================================================
// LEVEL DATA
// ============================================================================
private val levels = listOf(
    Level(
        id = 1,
        name = "Första rummet",
        description = "Placera lampor för att lysa upp rummet",
        maxLamps = 4, parLamps = 2, targetPercent = 80,
        walls = emptyList(), mirrors = emptyList(),
        candles = 0, candleBurnTime = 30f
    ),
    Level(
        id = 2,
        name = "Delat rum",
        description = "En vägg delar rummet",
        maxLamps = 4, parLamps = 2, targetPercent = 80,
        walls = listOf(
            Position(4, 2), Position(4, 3), Position(4, 4),
            Position(4, 5), Position(4, 6), Position(4, 7)
        ),
        mirrors = emptyList(), candles = 0, candleBurnTime = 30f
    ),
    Level(
        id = 3,
        name = "Pelarsalen",
        description = "Pelare kastar skuggor",
        maxLamps = 4, parLamps = 2, targetPercent = 80,
        walls = listOf(
            Position(2, 3), Position(5, 3), Position(2, 6), Position(5, 6)
        ),
        mirrors = emptyList(), candles = 0, candleBurnTime = 30f
    ),
    Level(
        id = 4,
        name = "Spegelgalleriet",
        description = "🪞 Speglar reflekterar ljus!",
        maxLamps = 3, parLamps = 2, targetPercent = 80,
        walls = listOf(
            Position(3, 3), Position(3, 4), Position(3, 5), Position(3, 6),
            Position(5, 3), Position(5, 4), Position(5, 5), Position(5, 6)
        ),
        mirrors = listOf(Mirror(4, 7, "up")),
        candles = 1, candleBurnTime = 30f
    ),
    Level(
        id = 5,
        name = "Vid levande ljus",
        description = "🕯️ Stearinljus brinner i 30 sek!",
        maxLamps = 2, parLamps = 1, targetPercent = 80,
        walls = listOf(
            Position(4, 0), Position(4, 1), Position(4, 2), Position(4, 3),
            Position(4, 6), Position(4, 7), Position(4, 8), Position(4, 9)
        ),
        mirrors = emptyList(), candles = 2, candleBurnTime = 30f
    ),
    Level(
        id = 6,
        name = "Fyra hörn",
        description = "Varje hörn är isolerat",
        maxLamps = 4, parLamps = 3, targetPercent = 80,
        walls = listOf(
            Position(0, 4), Position(1, 4), Position(2, 4),
            Position(5, 4), Position(6, 4), Position(7, 4),
            Position(4, 0), Position(4, 1), Position(4, 2),
            Position(4, 7), Position(4, 8), Position(4, 9),
            Position(3, 4), Position(4, 4), Position(4, 5), Position(4, 3)
        ),
        mirrors = emptyList(), candles = 0, candleBurnTime = 30f
    ),
    Level(
        id = 7,
        name = "Spegelkorridoren",
        description = "Studsa ljuset genom korridoren",
        maxLamps = 2, parLamps = 1, targetPercent = 80,
        walls = listOf(
            Position(0, 0), Position(0, 1), Position(0, 2), Position(0, 3),
            Position(0, 6), Position(0, 7), Position(0, 8), Position(0, 9),
            Position(7, 0), Position(7, 1), Position(7, 2), Position(7, 3),
            Position(7, 6), Position(7, 7), Position(7, 8), Position(7, 9),
            Position(2, 2), Position(3, 2), Position(4, 2),
            Position(3, 7), Position(4, 7), Position(5, 7)
        ),
        mirrors = listOf(Mirror(1, 4, "right"), Mirror(6, 5, "left")),
        candles = 1, candleBurnTime = 30f
    ),
    Level(
        id = 8,
        name = "Trappan",
        description = "Ljuset måste klättra uppåt",
        maxLamps = 3, parLamps = 2, targetPercent = 80,
        walls = listOf(
            Position(6, 0), Position(7, 0),
            Position(5, 1), Position(6, 1), Position(7, 1),
            Position(5, 2), Position(6, 2), Position(7, 2),
            Position(4, 3), Position(5, 3), Position(6, 3), Position(7, 3),
            Position(4, 4), Position(5, 4), Position(6, 4), Position(7, 4),
            Position(0, 5), Position(1, 5), Position(2, 5), Position(3, 5),
            Position(0, 6), Position(1, 6), Position(2, 6),
            Position(0, 7), Position(1, 7),
            Position(0, 8)
        ),
        mirrors = listOf(Mirror(3, 8, "all")),
        candles = 1, candleBurnTime = 30f
    ),
    Level(
        id = 9,
        name = "Fästningen",
        description = "Inre och yttre rum",
        maxLamps = 3, parLamps = 2, targetPercent = 80,
        walls = listOf(
            Position(2, 2), Position(3, 2), Position(4, 2), Position(5, 2),
            Position(2, 3), Position(5, 3),
            Position(2, 4), Position(5, 4),
            Position(2, 5), Position(5, 5),
            Position(2, 6), Position(5, 6),
            Position(2, 7), Position(3, 7), Position(4, 7), Position(5, 7)
        ),
        mirrors = listOf(Mirror(3, 3, "down"), Mirror(4, 6, "up")),
        candles = 1, candleBurnTime = 30f
    ),
    Level(
        id = 10,
        name = "Mästaren",
        description = "Allt du lärt dig - kombinerat!",
        maxLamps = 3, parLamps = 2, targetPercent = 80,
        walls = listOf(
            Position(0, 0), Position(1, 0), Position(2, 0),
            Position(0, 1), Position(1, 1),
            Position(0, 2),
            Position(4, 1), Position(4, 2),
            Position(3, 4), Position(4, 4), Position(5, 4),
            Position(4, 6), Position(4, 7),
            Position(6, 7), Position(7, 7),
            Position(6, 8), Position(7, 8),
            Position(6, 9), Position(7, 9)
        ),
        mirrors = listOf(
            Mirror(2, 4, "right"),
            Mirror(6, 4, "left"),
            Mirror(4, 8, "all")
        ),
        candles = 2, candleBurnTime = 30f
    )
)

// ============================================================================
// CONSTANTS
// ============================================================================
private const val BOARD_WIDTH = 8
private const val BOARD_HEIGHT = 10
private const val LIGHT_FADE = 0.08f
private const val LIT_THRESHOLD = 0.25f

// ============================================================================
// LIGHT CALCULATION
// ============================================================================
private fun calculateLight(
    level: Level,
    placedLamps: List<Position>,
    placedCandles: List<PlacedCandle>,
    candleTimeRemaining: Map<String, Float>
): Pair<Array<FloatArray>, Int> {
    val lightMap = Array(BOARD_HEIGHT) { FloatArray(BOARD_WIDTH) { 0f } }
    val wallSet = level.walls.toSet()
    val mirrorMap = level.mirrors.associateBy { Position(it.x, it.y) }

    fun isWall(x: Int, y: Int) = Position(x, y) in wallSet
    fun getMirror(x: Int, y: Int) = mirrorMap[Position(x, y)]

    fun castLight(startX: Int, startY: Int, startIntensity: Float, dirX: Int, dirY: Int, depth: Int = 0) {
        if (depth > 10) return

        var x = startX + dirX
        var y = startY + dirY
        var intensity = startIntensity

        while (x in 0 until BOARD_WIDTH && y in 0 until BOARD_HEIGHT && intensity > 0.05f) {
            if (isWall(x, y)) break

            val mirror = getMirror(x, y)
            if (mirror != null && depth < 3) {
                lightMap[y][x] = min(1f, lightMap[y][x] + intensity * 0.5f)

                when (mirror.dir) {
                    "all" -> {
                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                if (dx == 0 && dy == 0) continue
                                if (dx == -dirX && dy == -dirY) continue
                                castLight(x, y, intensity * 0.7f, dx, dy, depth + 1)
                            }
                        }
                    }
                    "left" -> if (dirX > 0) {
                        castLight(x, y, intensity * 0.8f, 0, -1, depth + 1)
                        castLight(x, y, intensity * 0.8f, 0, 1, depth + 1)
                    }
                    "right" -> if (dirX < 0) {
                        castLight(x, y, intensity * 0.8f, 0, -1, depth + 1)
                        castLight(x, y, intensity * 0.8f, 0, 1, depth + 1)
                    }
                    "up" -> if (dirY > 0) {
                        castLight(x, y, intensity * 0.8f, -1, 0, depth + 1)
                        castLight(x, y, intensity * 0.8f, 1, 0, depth + 1)
                    }
                    "down" -> if (dirY < 0) {
                        castLight(x, y, intensity * 0.8f, -1, 0, depth + 1)
                        castLight(x, y, intensity * 0.8f, 1, 0, depth + 1)
                    }
                }
                break
            }

            intensity = max(0f, intensity - LIGHT_FADE)
            lightMap[y][x] = min(1f, lightMap[y][x] + intensity)

            x += dirX
            y += dirY
        }
    }

    val directions = listOf(
        0 to -1, 0 to 1, -1 to 0, 1 to 0,
        -1 to -1, 1 to -1, -1 to 1, 1 to 1
    )

    // Process lamps
    placedLamps.forEach { lamp ->
        if (lamp.x in 0 until BOARD_WIDTH && lamp.y in 0 until BOARD_HEIGHT) {
            lightMap[lamp.y][lamp.x] = min(1f, lightMap[lamp.y][lamp.x] + 1f)
        }
        directions.forEach { (dx, dy) ->
            castLight(lamp.x, lamp.y, 1f, dx, dy)
        }
    }

    // Process candles
    placedCandles.forEach { candle ->
        val timeLeft = candleTimeRemaining[candle.id] ?: 0f
        if (timeLeft > 0) {
            val intensity = 0.8f * min(1f, timeLeft / 5f)
            if (candle.x in 0 until BOARD_WIDTH && candle.y in 0 until BOARD_HEIGHT) {
                lightMap[candle.y][candle.x] = min(1f, lightMap[candle.y][candle.x] + intensity)
            }
            directions.forEach { (dx, dy) ->
                castLight(candle.x, candle.y, intensity, dx, dy)
            }
        }
    }

    // Calculate percentage lit
    var litCount = 0
    var totalWalkable = 0
    for (y in 0 until BOARD_HEIGHT) {
        for (x in 0 until BOARD_WIDTH) {
            if (!isWall(x, y) && getMirror(x, y) == null) {
                totalWalkable++
                if (lightMap[y][x] > LIT_THRESHOLD) litCount++
            }
        }
    }

    val percentLit = if (totalWalkable > 0) {
        ((litCount.toFloat() / totalWalkable) * 100).roundToInt()
    } else 0

    return lightMap to percentLit
}

// ============================================================================
// MAIN GAME COMPOSABLE
// ============================================================================
@Composable
fun LightArchitectScreen(onBack: () -> Unit) {
    var currentLevelId by remember { mutableIntStateOf(1) }
    var placedLamps by remember { mutableStateOf(listOf<Position>()) }
    var placedCandles by remember { mutableStateOf(listOf<PlacedCandle>()) }
    var candleTimeRemaining by remember { mutableStateOf(mapOf<String, Float>()) }
    var availableCandles by remember { mutableStateOf(listOf<AvailableCandle>()) }
    var selectedTool by remember { mutableStateOf(Tool.LAMP) }
    var showSuccess by remember { mutableStateOf(false) }

    val level = remember(currentLevelId) { levels.find { it.id == currentLevelId } ?: levels[0] }

    // Pulse animation for candles
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    // Reset level
    fun resetLevel() {
        placedLamps = emptyList()
        placedCandles = emptyList()
        candleTimeRemaining = emptyMap()
        availableCandles = (0 until level.candles).map {
            AvailableCandle("candle-$it", level.candleBurnTime)
        }
        showSuccess = false
        selectedTool = Tool.LAMP
    }

    // Init level
    LaunchedEffect(currentLevelId) {
        resetLevel()
    }

    // Candle timer
    LaunchedEffect(placedCandles) {
        while (true) {
            delay(100)
            if (placedCandles.isNotEmpty()) {
                candleTimeRemaining = candleTimeRemaining.mapValues { (id, time) ->
                    if (placedCandles.any { it.id == id }) {
                        max(0f, time - 0.1f)
                    } else time
                }
            }
        }
    }

    // Calculate light
    val (lightMap, percentLit) = remember(level, placedLamps, placedCandles, candleTimeRemaining) {
        calculateLight(level, placedLamps, placedCandles, candleTimeRemaining)
    }

    // Check win
    LaunchedEffect(percentLit) {
        if (percentLit >= level.targetPercent && !showSuccess) {
            delay(500)
            showSuccess = true
        }
    }

    val wallSet = remember(level) { level.walls.toSet() }
    val mirrorMap = remember(level) { level.mirrors.associateBy { Position(it.x, it.y) } }

    fun handleCellClick(x: Int, y: Int) {
        if (showSuccess) return
        if (Position(x, y) in wallSet) return
        if (mirrorMap.containsKey(Position(x, y))) return

        // Check for existing lamp
        val existingLamp = placedLamps.find { it.x == x && it.y == y }
        if (existingLamp != null) {
            placedLamps = placedLamps - existingLamp
            return
        }

        // Check for existing candle
        val existingCandle = placedCandles.find { it.x == x && it.y == y }
        if (existingCandle != null) {
            val remaining = candleTimeRemaining[existingCandle.id] ?: 0f
            placedCandles = placedCandles - existingCandle
            availableCandles = availableCandles + AvailableCandle(existingCandle.id, remaining)
            return
        }

        // Place new item
        when (selectedTool) {
            Tool.LAMP -> {
                if (placedLamps.size < level.maxLamps) {
                    placedLamps = placedLamps + Position(x, y)
                }
            }
            Tool.CANDLE -> {
                if (availableCandles.isNotEmpty()) {
                    val candle = availableCandles.first()
                    availableCandles = availableCandles.drop(1)
                    placedCandles = placedCandles + PlacedCandle(candle.id, x, y)
                    candleTimeRemaining = candleTimeRemaining + (candle.id to candle.timeRemaining)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightTheme.pageBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header - kompaktare
            GameHeader(
                level = level,
                onBack = onBack,
                onReset = { resetLevel() }
            )

            // Progress bar
            ProgressSection(
                percentLit = percentLit,
                targetPercent = level.targetPercent
            )

            // Game board - tar upp allt tillgängligt utrymme
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                GameBoard(
                    level = level,
                    lightMap = lightMap,
                    placedLamps = placedLamps,
                    placedCandles = placedCandles,
                    candleTimeRemaining = candleTimeRemaining,
                    wallSet = wallSet,
                    mirrorMap = mirrorMap,
                    pulsePhase = pulsePhase,
                    onCellClick = { x, y -> handleCellClick(x, y) }
                )
            }

            // Controls - kompaktare
            ControlsSection(
                level = level,
                selectedTool = selectedTool,
                remainingLamps = level.maxLamps - placedLamps.size,
                availableCandles = availableCandles.size,
                onSelectTool = { selectedTool = it }
            )

            // Lite extra utrymme för bottom navigation
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Success overlay
        if (showSuccess) {
            SuccessOverlay(
                percentLit = percentLit,
                usedLamps = placedLamps.size,
                parLamps = level.parLamps,
                hasNextLevel = levels.any { it.id == currentLevelId + 1 },
                onNextLevel = {
                    val nextLevel = levels.find { it.id == currentLevelId + 1 }
                    if (nextLevel != null) {
                        currentLevelId = nextLevel.id
                    } else {
                        onBack()
                    }
                },
                onPlayAgain = { resetLevel() }
            )
        }
    }
}

// ============================================================================
// UI COMPONENTS
// ============================================================================
@Composable
private fun GameHeader(
    level: Level,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightTheme.uiCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(LightTheme.accent.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Tillbaka",
                    tint = LightTheme.accent
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LightTheme.accent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${level.id}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightTheme.accent
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = level.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = LightTheme.uiText
                    )
                }
                Text(
                    text = level.description,
                    fontSize = 11.sp,
                    color = LightTheme.uiTextMuted
                )
            }

            IconButton(
                onClick = onReset,
                modifier = Modifier
                    .size(36.dp)
                    .background(LightTheme.accent.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Återställ",
                    tint = LightTheme.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(
    percentLit: Int,
    targetPercent: Int
) {
    val isComplete = percentLit >= targetPercent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightTheme.uiCard)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LJUSNIVÅ",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = LightTheme.uiTextMuted
                )
                Text(
                    text = "$percentLit% / 100%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isComplete) LightTheme.success else LightTheme.accent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LightTheme.progressBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percentLit / 100f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isComplete) {
                                    listOf(LightTheme.success, Color(0xFF4ADE80))
                                } else {
                                    listOf(LightTheme.accent, LightTheme.accentLight)
                                }
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun GameBoard(
    level: Level,
    lightMap: Array<FloatArray>,
    placedLamps: List<Position>,
    placedCandles: List<PlacedCandle>,
    candleTimeRemaining: Map<String, Float>,
    wallSet: Set<Position>,
    mirrorMap: Map<Position, Mirror>,
    pulsePhase: Float,
    onCellClick: (Int, Int) -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // Calculate cell size to fill available space
        // Account for padding and gaps
        val totalGapWidth = (BOARD_WIDTH - 1) * with(density) { 2.dp.toPx() }
        val totalGapHeight = (BOARD_HEIGHT - 1) * with(density) { 2.dp.toPx() }
        val paddingPx = with(density) { 16.dp.toPx() } // Card padding

        val availableWidth = maxWidthPx - paddingPx - totalGapWidth
        val availableHeight = maxHeightPx - paddingPx - totalGapHeight

        val cellByWidth = availableWidth / BOARD_WIDTH
        val cellByHeight = availableHeight / BOARD_HEIGHT

        // Use the smaller dimension to maintain square cells
        val cellSizePx = minOf(cellByWidth, cellByHeight).coerceAtLeast(28f)
        val cellSizeDp = with(density) { cellSizePx.toDp() }
        val gap = 2.dp

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightTheme.uiCard)
        ) {
            Column(
                modifier = Modifier
                    .background(LightTheme.floor)
                    .padding(6.dp)
            ) {
                for (y in 0 until BOARD_HEIGHT) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        for (x in 0 until BOARD_WIDTH) {
                            val isWall = Position(x, y) in wallSet
                            val mirror = mirrorMap[Position(x, y)]
                            val isLamp = placedLamps.any { it.x == x && it.y == y }
                            val candle = placedCandles.find { it.x == x && it.y == y }
                            val light = lightMap[y][x]

                            GameCell(
                                size = cellSizeDp,
                                isWall = isWall,
                                mirror = mirror,
                                isLamp = isLamp,
                                candle = candle,
                                candleTimeRemaining = candle?.let { candleTimeRemaining[it.id] ?: 0f } ?: 0f,
                                candleBurnTime = level.candleBurnTime,
                                light = light,
                                pulsePhase = pulsePhase,
                                onClick = { onCellClick(x, y) }
                            )
                        }
                    }
                    if (y < BOARD_HEIGHT - 1) {
                        Spacer(modifier = Modifier.height(gap))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCell(
    size: Dp,
    isWall: Boolean,
    mirror: Mirror?,
    isLamp: Boolean,
    candle: PlacedCandle?,
    candleTimeRemaining: Float,
    candleBurnTime: Float,
    light: Float,
    pulsePhase: Float,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isWall -> LightTheme.wall
        mirror != null -> Color(0xFF1E3A5A)
        light > 0.1f -> {
            val lightAlpha = (light * 255).roundToInt().coerceIn(0, 255)
            Color(
                red = (LightTheme.floor.red * (1 - light) + LightTheme.floorLit.red * light),
                green = (LightTheme.floor.green * (1 - light) + LightTheme.floorLit.green * light),
                blue = (LightTheme.floor.blue * (1 - light) + LightTheme.floorLit.blue * light)
            )
        }
        else -> LightTheme.floor
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = when {
                    isWall -> Color(0xFF6484AA).copy(alpha = 0.3f)
                    mirror != null -> LightTheme.mirrorColor.copy(alpha = 0.5f)
                    light > 0.2f -> LightTheme.lampGlow.copy(alpha = 0.4f)
                    else -> Color(0xFF648CB4).copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = !isWall && mirror == null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Mirror icon
        if (mirror != null) {
            Canvas(modifier = Modifier.size(size * 0.5f)) {
                if (mirror.dir == "all") {
                    // Draw cross for omnidirectional mirror
                    drawLine(
                        color = LightTheme.mirrorColor,
                        start = Offset(0f, this.size.height / 2),
                        end = Offset(this.size.width, this.size.height / 2),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = LightTheme.mirrorColor,
                        start = Offset(this.size.width / 2, 0f),
                        end = Offset(this.size.width / 2, this.size.height),
                        strokeWidth = 3f
                    )
                } else {
                    // Draw line for directional mirror
                    val isHorizontal = mirror.dir == "up" || mirror.dir == "down"
                    if (isHorizontal) {
                        drawLine(
                            color = LightTheme.mirrorColor,
                            start = Offset(0f, this.size.height / 2),
                            end = Offset(this.size.width, this.size.height / 2),
                            strokeWidth = 3f
                        )
                    } else {
                        drawLine(
                            color = LightTheme.mirrorColor,
                            start = Offset(this.size.width / 2, 0f),
                            end = Offset(this.size.width / 2, this.size.height),
                            strokeWidth = 3f
                        )
                    }
                }
            }
        }

        // Lamp
        if (isLamp) {
            LampIcon(size = size * 0.8f)
        }

        // Candle
        if (candle != null && candleTimeRemaining > 0) {
            CandleIcon(
                size = size * 0.8f,
                timePercent = candleTimeRemaining / candleBurnTime,
                pulsePhase = pulsePhase
            )

            // Timer bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(2.dp)
                    .fillMaxWidth(0.9f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(candleTimeRemaining / candleBurnTime)
                        .background(
                            if (candleTimeRemaining < 5f) Color(0xFFEF4444)
                            else LightTheme.candleGlow
                        )
                )
            }
        }
    }
}

@Composable
private fun LampIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val bulbRadius = this.size.minDimension * 0.35f

        // Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    LightTheme.lampGlow.copy(alpha = 0.6f),
                    LightTheme.lampGlow.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = bulbRadius * 1.8f
            ),
            radius = bulbRadius * 1.8f,
            center = center
        )

        // Bulb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    LightTheme.lampGlow,
                    LightTheme.lampGlow.copy(alpha = 0.8f)
                ),
                center = Offset(center.x - bulbRadius * 0.3f, center.y - bulbRadius * 0.3f),
                radius = bulbRadius
            ),
            radius = bulbRadius,
            center = center
        )

        // Filament
        val path = Path().apply {
            moveTo(center.x - bulbRadius * 0.3f, center.y - bulbRadius * 0.4f)
            lineTo(center.x + bulbRadius * 0.3f, center.y - bulbRadius * 0.1f)
            lineTo(center.x - bulbRadius * 0.3f, center.y + bulbRadius * 0.2f)
            lineTo(center.x + bulbRadius * 0.3f, center.y + bulbRadius * 0.5f)
        }
        drawPath(
            path = path,
            color = LightTheme.lampGlow,
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )

        // Base
        drawRect(
            color = Color.Gray,
            topLeft = Offset(center.x - bulbRadius * 0.25f, center.y + bulbRadius * 0.8f),
            size = Size(bulbRadius * 0.5f, bulbRadius * 0.3f)
        )
    }
}

@Composable
private fun CandleIcon(size: Dp, timePercent: Float, pulsePhase: Float) {
    val pulseScale = 1f + sin(pulsePhase) * 0.06f
    val opacity = min(1f, timePercent * 1.5f)

    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer(alpha = opacity)
    ) {
        val center = Offset(this.size.width / 2, this.size.height)
        val candleWidth = this.size.width * 0.25f
        val candleHeight = this.size.height * 0.5f

        // Candle body
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFEAE6E0),
                    Color(0xFFFFFEF8),
                    Color(0xFFFFFEF8),
                    Color(0xFFEAE6E0)
                )
            ),
            topLeft = Offset(center.x - candleWidth / 2, center.y - candleHeight),
            size = Size(candleWidth, candleHeight)
        )

        // Wick
        drawRect(
            color = Color(0xFF1A1A1A),
            topLeft = Offset(center.x - 1f, center.y - candleHeight - 8f),
            size = Size(2f, 8f)
        )

        // Flame
        val flameCenter = Offset(center.x, center.y - candleHeight - 20f * pulseScale)

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    LightTheme.candleGlow.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = flameCenter,
                radius = 25f * pulseScale
            ),
            radius = 25f * pulseScale,
            center = flameCenter
        )

        // Flame shape
        val flamePath = Path().apply {
            moveTo(flameCenter.x, flameCenter.y - 15f * pulseScale)
            quadraticBezierTo(
                flameCenter.x + 8f * pulseScale, flameCenter.y,
                flameCenter.x, flameCenter.y + 12f * pulseScale
            )
            quadraticBezierTo(
                flameCenter.x - 8f * pulseScale, flameCenter.y,
                flameCenter.x, flameCenter.y - 15f * pulseScale
            )
            close()
        }

        drawPath(
            path = flamePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFEE88),
                    Color(0xFFFF9900),
                    Color(0xFFCC3300)
                )
            )
        )

        // Inner flame
        drawCircle(
            color = Color(0xFFFFFEF5),
            radius = 4f * pulseScale,
            center = Offset(flameCenter.x, flameCenter.y - 5f * pulseScale)
        )
    }
}

@Composable
private fun ControlsSection(
    level: Level,
    selectedTool: Tool,
    remainingLamps: Int,
    availableCandles: Int,
    onSelectTool: (Tool) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightTheme.uiCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lamp button
            ToolButton(
                selected = selectedTool == Tool.LAMP,
                label = "$remainingLamps kvar",
                onClick = { onSelectTool(Tool.LAMP) }
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = if (selectedTool == Tool.LAMP) LightTheme.accent else LightTheme.uiTextMuted,
                        radius = 6f,
                        center = center
                    )
                    drawLine(
                        color = if (selectedTool == Tool.LAMP) LightTheme.accent else LightTheme.uiTextMuted,
                        start = Offset(center.x, center.y + 6f),
                        end = Offset(center.x, center.y + 11f),
                        strokeWidth = 2.5f
                    )
                }
            }

            // Candle button (if level has candles)
            if (level.candles > 0) {
                Spacer(modifier = Modifier.width(10.dp))

                ToolButton(
                    selected = selectedTool == Tool.CANDLE,
                    label = "$availableCandles kvar",
                    accentColor = Color(0xFFEA580C),
                    onClick = { onSelectTool(Tool.CANDLE) }
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        // Simple flame icon
                        val path = Path().apply {
                            moveTo(size.width / 2, 3f)
                            quadraticBezierTo(size.width - 3f, size.height / 2, size.width / 2, size.height - 3f)
                            quadraticBezierTo(3f, size.height / 2, size.width / 2, 3f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = if (selectedTool == Tool.CANDLE) Color(0xFFEA580C) else LightTheme.uiTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    selected: Boolean,
    label: String,
    accentColor: Color = LightTheme.accent,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accentColor.copy(alpha = 0.15f) else LightTheme.progressBg,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, accentColor)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) accentColor else LightTheme.uiTextMuted
            )
        }
    }
}

@Composable
private fun SuccessOverlay(
    percentLit: Int,
    usedLamps: Int,
    parLamps: Int,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val stars = if (usedLamps <= parLamps) 3 else 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 300.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LightTheme.uiCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(LightTheme.success, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rummet upplyst!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTheme.uiText
                )

                Text(
                    text = "Du nådde $percentLit% ljusnivå.",
                    fontSize = 14.sp,
                    color = LightTheme.uiTextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stars
                Row {
                    repeat(3) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < stars) Color(0xFFFACC15) else Color(0xFFD1D5DB),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightTheme.accent)
                ) {
                    Text(
                        text = if (hasNextLevel) "Nästa Nivå" else "Tillbaka",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Spela igen",
                        fontWeight = FontWeight.Bold,
                        color = LightTheme.uiText
                    )
                }
            }
        }
    }
}