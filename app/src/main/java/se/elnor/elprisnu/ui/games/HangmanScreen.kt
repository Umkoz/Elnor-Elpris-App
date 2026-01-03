package se.elnor.elprisnu.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun HangmanScreen(onBack: () -> Unit) {
    val wordList = listOf(
        "TRANSFORMATOR", "KILOWATTIMME", "SOLCELLER", "VINDKRAFT", "KÄRNKRAFT",
        "SPOTPRIS", "HÖGSPÄNNING", "EFFEKTBRIST", "LADDBOX", "ELRÄKNING"
    )
    var word by rememberSaveable { mutableStateOf("") }
    var guessed by rememberSaveable { mutableStateOf(setOf<Char>()) }
    var wrongGuesses by rememberSaveable { mutableStateOf(0) }
    val maxWrong = 6

    LaunchedEffect(Unit) {
        if (word.isEmpty()) {
            word = wordList.random(Random(System.currentTimeMillis()))
            guessed = emptySet()
            wrongGuesses = 0
        }
    }

    val isWinner = word.isNotEmpty() && word.all { it in guessed }
    val isLoser = wrongGuesses >= maxWrong

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Hänga gubbe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    word = wordList.random(Random(System.currentTimeMillis()))
                    guessed = emptySet()
                    wrongGuesses = 0
                }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Ny omgång")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                HangmanDrawing(wrongGuesses)
                if (isWinner || isLoser) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xAA000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(16.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isWinner) {
                                    Text(
                                        "Snyggt!",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        "Tyvärr",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text("Ordet var $word", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    word = wordList.random(Random(System.currentTimeMillis()))
                                    guessed = emptySet()
                                    wrongGuesses = 0
                                }) {
                                    Text(if (isWinner) "En till?" else "Försök igen")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                word.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(32.dp)
                            .background(Color.White, shape = RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val showChar = ch in guessed || isLoser
                        Text(if (showChar) ch.toString() else "", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZÅÄÖ"
            val columns = 7
            val rows = (letters.length + columns - 1) / columns

            Column {
                var index = 0
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until columns) {
                            if (index < letters.length) {
                                val letter = letters[index]
                                val isGuessed = letter in guessed
                                val enabled = !isGuessed && !isWinner && !isLoser
                                LetterButton(
                                    letter = letter,
                                    enabled = enabled,
                                    onClick = {
                                        if (letter !in guessed) {
                                            guessed = guessed + letter
                                            if (letter !in word) {
                                                wrongGuesses++
                                            }
                                        }
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            index++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.LetterButton(letter: Char, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 2.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color(0xFFE5E7EB)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter.toString(),
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.Black else Color.Gray
            )
        }
    }
}

@Composable
private fun HangmanDrawing(wrongGuesses: Int) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val lineColor = Color.White.copy(alpha = 0.6f)

        drawLine(lineColor, Offset(w * 0.2f, h * 0.9f), Offset(w * 0.8f, h * 0.9f), strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(lineColor, Offset(w * 0.4f, h * 0.9f), Offset(w * 0.4f, h * 0.1f), strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(lineColor, Offset(w * 0.4f, h * 0.1f), Offset(w * 0.75f, h * 0.1f), strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(lineColor, Offset(w * 0.75f, h * 0.1f), Offset(w * 0.75f, h * 0.2f), strokeWidth = 8f, cap = StrokeCap.Round)

        val bodyColor = Color(0xFFFB7185)
        if (wrongGuesses >= 1) {
            drawCircle(bodyColor, radius = w * 0.06f, center = Offset(w * 0.75f, h * 0.3f), style = Stroke(width = 8f))
        }
        if (wrongGuesses >= 2) {
            drawLine(bodyColor, Offset(w * 0.75f, h * 0.36f), Offset(w * 0.75f, h * 0.6f), strokeWidth = 8f, cap = StrokeCap.Round)
        }
        if (wrongGuesses >= 3) {
            drawLine(bodyColor, Offset(w * 0.75f, h * 0.42f), Offset(w * 0.65f, h * 0.5f), strokeWidth = 8f, cap = StrokeCap.Round)
        }
        if (wrongGuesses >= 4) {
            drawLine(bodyColor, Offset(w * 0.75f, h * 0.42f), Offset(w * 0.85f, h * 0.5f), strokeWidth = 8f, cap = StrokeCap.Round)
        }
        if (wrongGuesses >= 5) {
            drawLine(bodyColor, Offset(w * 0.75f, h * 0.6f), Offset(w * 0.65f, h * 0.75f), strokeWidth = 8f, cap = StrokeCap.Round)
        }
        if (wrongGuesses >= 6) {
            drawLine(bodyColor, Offset(w * 0.75f, h * 0.6f), Offset(w * 0.85f, h * 0.75f), strokeWidth = 8f, cap = StrokeCap.Round)
        }
    }
}
