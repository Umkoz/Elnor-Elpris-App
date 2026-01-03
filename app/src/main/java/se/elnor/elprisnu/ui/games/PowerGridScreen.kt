package se.elnor.elprisnu.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LightButton(
    val id: Int,
    val color: Color,
    val activeColor: Color
)

@Composable
fun PowerGridScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    
    val buttons = remember {
        listOf(
            LightButton(0, Color(0xFF22C55E), Color(0xFF86EFAC)), // Green
            LightButton(1, Color(0xFFEF4444), Color(0xFFFCA5A5)), // Red
            LightButton(2, Color(0xFFEAB308), Color(0xFFFDE047)), // Yellow
            LightButton(3, Color(0xFF3B82F6), Color(0xFF93C5FD))  // Blue
        )
    }

    val sequence = remember { mutableStateListOf<Int>() }
    var userStep by remember { mutableIntStateOf(0) }
    var isPlayingSequence by remember { mutableStateOf(false) }
    var activeLight by remember { mutableStateOf<Int?>(null) }
    var gameState by remember { mutableStateOf("start") } // start, playing, gameover
    var score by remember { mutableIntStateOf(0) }

    // Play sequence when it changes
    LaunchedEffect(sequence.size, gameState) {
        if (sequence.isNotEmpty() && gameState == "playing") {
            isPlayingSequence = true
            delay(500) // Initial delay
            
            for (lightId in sequence) {
                activeLight = lightId
                delay(400)
                activeLight = null
                delay(400)
            }
            
            isPlayingSequence = false
        }
    }

    fun flashLight(id: Int) {
        scope.launch {
            activeLight = id
            delay(200)
            activeLight = null
        }
    }

    fun startGame() {
        sequence.clear()
        sequence.add(Random.nextInt(4))
        userStep = 0
        score = 0
        gameState = "playing"
    }

    fun handleInput(id: Int) {
        if (isPlayingSequence || gameState != "playing") return

        flashLight(id)

        if (id == sequence[userStep]) {
            // Correct!
            if (userStep == sequence.size - 1) {
                // Completed sequence - add new light
                score++
                userStep = 0
                scope.launch {
                    delay(1000)
                    sequence.add(Random.nextInt(4))
                }
            } else {
                userStep++
            }
        } else {
            // Wrong!
            gameState = "gameover"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Tillbaka",
                        tint = Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = "Strömavbrottet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Box(modifier = Modifier.size(48.dp)) // Spacer for alignment
            }

            // Game content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Light buttons grid
                Column(
                    modifier = Modifier
                        .then(
                            if (gameState != "playing") {
                                Modifier.background(Color.Black.copy(alpha = 0.5f))
                            } else Modifier
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LightButtonView(
                            button = buttons[0],
                            isActive = activeLight == 0,
                            enabled = gameState == "playing" && !isPlayingSequence,
                            onClick = { handleInput(0) }
                        )
                        LightButtonView(
                            button = buttons[1],
                            isActive = activeLight == 1,
                            enabled = gameState == "playing" && !isPlayingSequence,
                            onClick = { handleInput(1) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LightButtonView(
                            button = buttons[2],
                            isActive = activeLight == 2,
                            enabled = gameState == "playing" && !isPlayingSequence,
                            onClick = { handleInput(2) }
                        )
                        LightButtonView(
                            button = buttons[3],
                            isActive = activeLight == 3,
                            enabled = gameState == "playing" && !isPlayingSequence,
                            onClick = { handleInput(3) }
                        )
                    }
                }

                // Start screen overlay
                if (gameState == "start") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Minnesspel",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Upprepa ljusmönstret",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { startGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Starta",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }

                // Game over overlay
                if (gameState == "gameover") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                            .padding(32.dp)
                    ) {
                        Text(
                            text = "Kortslutning!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Du klarade $score ronder",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { startGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = "Försök igen",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }

            // Status text
            if (gameState == "playing") {
                Text(
                    text = if (isPlayingSequence) "Titta..." else "Din tur!",
                    color = Color(0xFF94A3B8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LightButtonView(
    button: LightButton,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isActive) button.activeColor else button.color
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .then(
                if (isActive) {
                    Modifier.shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = button.activeColor
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled) { onClick() }
    )
}
