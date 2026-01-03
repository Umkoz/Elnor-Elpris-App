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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.elnor.elprisnu.ui.theme.BackgroundCard
import se.elnor.elprisnu.ui.theme.BackgroundPrimary
import se.elnor.elprisnu.ui.theme.BrandBlue
import se.elnor.elprisnu.ui.theme.TextPrimary
import se.elnor.elprisnu.ui.theme.TextSecondary
import se.elnor.elprisnu.ui.theme.TextTertiary

@Composable
fun GameCenterScreen(onNavigateToHangman: () -> Unit = {}) {
    var activeGame by remember { mutableStateOf<String?>(null) }

    // Render active game
    when (activeGame) {
        "hangman" -> HangmanScreen(onBack = { activeGame = null })
        "minesweeper" -> MinesweeperScreen(onBack = { activeGame = null })
        "sudoku" -> SudokuScreen(onBack = { activeGame = null })
        "chess" -> ChessScreen(onBack = { activeGame = null })
        "powergrid" -> PowerGridScreen(onBack = { activeGame = null })
        "outlet" -> OutletRushScreen(onBack = { activeGame = null })
        "lightarchitect" -> LightArchitectScreen(onBack = { activeGame = null })
        else -> {
            // Game Menu
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundPrimary)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Nöjeshörnan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Försök slå ihjäl tiden medan elpriset är högt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                val games = listOf(
                    GameItemData("Hänga Gubbe", "ORDLEK", "👻", Color(0xFF8B5CF6), "hangman"),
                    GameItemData("Minröj", "KLASSIKER", "💣", Color(0xFFEF4444), "minesweeper"),
                    GameItemData("Sudoku", "LOGIK", "🔢", Color(0xFF10B981), "sudoku"),
                    GameItemData("Schack", "STRATEGI", "♟️", Color(0xFF1E293B), "chess"),
                    GameItemData("Strömavbrottet", "MINNE", "💡", Color(0xFFEAB308), "powergrid"),
                    GameItemData("Ljusarkitekten", "PUSSEL", "☀️", Color(0xFFF97316), "lightarchitect"),
                    GameItemData("Outlet Rush", "REAKTION", "⚡", Color(0xFF3B82F6), "outlet")
                )

                for (row in games.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { item ->
                            GameCard(
                                item = item,
                                onClick = { activeGame = item.gameId },
                                enabled = true
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                }

                Spacer(modifier = Modifier.size(80.dp)) // Bottom nav spacing
            }
        }
    }
}

data class GameItemData(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val accentColor: Color,
    val gameId: String
)

@Composable
private fun RowScope.GameCard(
    item: GameItemData,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji icon in circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(item.accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                color = if (enabled) TextPrimary else TextTertiary,
                fontSize = 14.sp
            )

            Text(
                text = if (enabled) item.subtitle else "KOMMER SNART",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                letterSpacing = 1.sp
            )
        }
    }
}
