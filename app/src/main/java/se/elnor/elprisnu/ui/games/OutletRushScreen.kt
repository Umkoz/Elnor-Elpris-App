package se.elnor.elprisnu.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Socket(
    val state: SocketState = SocketState.EMPTY,
    val spawnTime: Long = 0L
)

enum class SocketState {
    EMPTY, PLUG, HAZARD
}

@Composable
fun OutletRushScreen(onBack: () -> Unit) {
    var sockets by remember { mutableStateOf(List(9) { Socket() }) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var gameActive by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var highScore by remember { mutableIntStateOf(0) }

    // Game timer
    LaunchedEffect(gameActive) {
        if (gameActive) {
            while (timeLeft > 0 && gameActive) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft <= 0) {
                gameActive = false
                gameOver = true
                if (score > highScore) {
                    highScore = score
                }
            }
        }
    }

    // Spawn items
    LaunchedEffect(gameActive) {
        if (gameActive) {
            while (gameActive) {
                delay(600)
                val currentTime = System.currentTimeMillis()
                sockets = sockets.toMutableList().apply {
                    val emptyIndices = indices.filter { this[it].state == SocketState.EMPTY }
                    if (emptyIndices.isNotEmpty()) {
                        val idx = emptyIndices.random()
                        val isHazard = Random.nextFloat() > 0.7f
                        this[idx] = Socket(
                            state = if (isHazard) SocketState.HAZARD else SocketState.PLUG,
                            spawnTime = currentTime
                        )
                    }
                }
            }
        }
    }

    // Auto-remove expired items
    LaunchedEffect(gameActive) {
        if (gameActive) {
            while (gameActive) {
                delay(100) // Check every 100ms
                val currentTime = System.currentTimeMillis()
                sockets = sockets.mapIndexed { _, socket ->
                    if (socket.state != SocketState.EMPTY) {
                        val maxAge = if (socket.state == SocketState.HAZARD) 2000L else 1200L
                        if (currentTime - socket.spawnTime > maxAge) {
                            Socket()
                        } else {
                            socket
                        }
                    } else {
                        socket
                    }
                }
            }
        }
    }

    fun startGame() {
        score = 0
        timeLeft = 30
        gameActive = true
        gameOver = false
        sockets = List(9) { Socket() }
    }

    fun handleTap(index: Int) {
        if (!gameActive) return
        
        when (sockets[index].state) {
            SocketState.PLUG -> {
                score += 10
                sockets = sockets.toMutableList().apply { this[index] = Socket() }
            }
            SocketState.HAZARD -> {
                score = maxOf(0, score - 50)
                sockets = sockets.toMutableList().apply { this[index] = Socket() }
            }
            SocketState.EMPTY -> { /* Do nothing */ }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tillbaka",
                            tint = Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "POÄNG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = score.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(48.dp)
                    ) {
                        Text(
                            text = "TID",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "${timeLeft}s",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (timeLeft < 10) Color(0xFFEF4444) else Color(0xFF1E293B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Game content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    gameOver -> {
                        // Game Over Screen
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Tiden är ute!",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Du fick $score poäng",
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { startGame() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Spela igen",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                    !gameActive && !gameOver -> {
                        // Start Screen
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Outlet Rush",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Dra ur sladdarna 🔌 men rör inte\nde trasiga uttagen ⚡!",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Text(
                                    text = "Highscore: $highScore",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { startGame() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    text = "Starta Spelet",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        // Game Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(sockets) { idx, socket ->
                                SocketButton(
                                    state = socket.state,
                                    onClick = { handleTap(idx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocketButton(
    state: SocketState,
    onClick: () -> Unit
) {
    val backgroundColor = when (state) {
        SocketState.EMPTY -> Color(0xFFF1F5F9)
        SocketState.PLUG -> Color.White
        SocketState.HAZARD -> Color(0xFFFEE2E2)
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(if (state != SocketState.EMPTY) 1.05f else 1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                SocketState.EMPTY -> {
                    // Empty socket holes
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp, 16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFCBD5E1))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp, 16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFCBD5E1))
                        )
                    }
                }
                SocketState.PLUG -> {
                    Icon(
                        imageVector = Icons.Default.Power,
                        contentDescription = null,
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(40.dp)
                    )
                }
                SocketState.HAZARD -> {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}
