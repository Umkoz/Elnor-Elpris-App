package se.elnor.elprisnu.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MineCell(
    val hasMine: Boolean = false,
    val isOpen: Boolean = false,
    val isFlagged: Boolean = false,
    val neighborMines: Int = 0
)

@Composable
fun MinesweeperScreen(onBack: () -> Unit) {
    val gridSize = 8
    val totalMines = 10

    var grid by remember { mutableStateOf(generateMineGrid(gridSize, totalMines)) }
    var gameState by remember { mutableStateOf("playing") } // playing, won, lost
    var flagMode by remember { mutableStateOf(false) }

    fun resetGame() {
        grid = generateMineGrid(gridSize, totalMines)
        gameState = "playing"
        flagMode = false
    }

    fun getNeighbors(idx: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        val row = idx / gridSize
        val col = idx % gridSize
        for (r in (row - 1)..(row + 1)) {
            for (c in (col - 1)..(col + 1)) {
                if (r in 0 until gridSize && c in 0 until gridSize) {
                    val nIdx = r * gridSize + c
                    if (nIdx != idx) neighbors.add(nIdx)
                }
            }
        }
        return neighbors
    }

    fun revealCell(idx: Int, currentGrid: MutableList<MineCell>): MutableList<MineCell> {
        if (currentGrid[idx].isOpen || currentGrid[idx].isFlagged) return currentGrid
        currentGrid[idx] = currentGrid[idx].copy(isOpen = true)
        if (currentGrid[idx].neighborMines == 0 && !currentGrid[idx].hasMine) {
            getNeighbors(idx).forEach { n ->
                revealCell(n, currentGrid)
            }
        }
        return currentGrid
    }

    fun handleCellClick(idx: Int) {
        if (gameState != "playing" || grid[idx].isOpen) return

        if (flagMode) {
            val newGrid = grid.toMutableList()
            newGrid[idx] = newGrid[idx].copy(isFlagged = !newGrid[idx].isFlagged)
            grid = newGrid
            return
        }

        if (grid[idx].isFlagged) return

        if (grid[idx].hasMine) {
            // Game over - reveal all mines
            val newGrid = grid.mapIndexed { i, cell ->
                if (cell.hasMine) cell.copy(isOpen = true) else cell
            }
            grid = newGrid
            gameState = "lost"
            return
        }

        val newGrid = grid.toMutableList()
        revealCell(idx, newGrid)
        grid = newGrid

        // Check win condition
        if (grid.filter { !it.hasMine }.all { it.isOpen }) {
            gameState = "won"
        }
    }

    val flaggedCount = grid.count { it.isFlagged }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Tillbaka"
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Minröj",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$flaggedCount/$totalMines minor",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = { resetGame() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Ny omgång",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF334155))
                        .padding(8.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridSize),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.size((gridSize * 42).dp)
                    ) {
                        itemsIndexed(grid) { idx, cell ->
                            MineCell(
                                cell = cell,
                                onClick = { handleCellClick(idx) }
                            )
                        }
                    }

                    // Game Over Overlay
                    if (gameState != "playing") {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (gameState == "won") "Röjt! 🎉" else "Boom! 💥",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (gameState == "won") Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { resetGame() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E293B)
                                        )
                                    ) {
                                        Text("Försök igen")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Dig button
                Button(
                    onClick = { flagMode = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!flagMode) Color(0xFF1E293B) else Color.Transparent,
                        contentColor = if (!flagMode) Color.White else Color.Gray
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⛏️ Gräv", fontWeight = FontWeight.Bold)
                }

                // Flag button
                Button(
                    onClick = { flagMode = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (flagMode) Color(0xFFEF4444) else Color.Transparent,
                        contentColor = if (flagMode) Color.White else Color.Gray
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(" Flagga", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MineCell(
    cell: MineCell,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        !cell.isOpen -> Color(0xFFF1F5F9)
        cell.hasMine -> Color(0xFFEF4444)
        else -> Color(0xFFE2E8F0)
    }

    val textColor = when (cell.neighborMines) {
        1 -> Color(0xFF3B82F6)
        2 -> Color(0xFF10B981)
        else -> Color(0xFFEF4444)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            cell.isFlagged && !cell.isOpen -> {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
            cell.isOpen && cell.hasMine -> {
                Text("💣", fontSize = 18.sp)
            }
            cell.isOpen && cell.neighborMines > 0 -> {
                Text(
                    text = cell.neighborMines.toString(),
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

private fun generateMineGrid(gridSize: Int, totalMines: Int): List<MineCell> {
    val cells = MutableList(gridSize * gridSize) { MineCell() }

    // Place mines
    var minesPlaced = 0
    while (minesPlaced < totalMines) {
        val idx = (0 until cells.size).random()
        if (!cells[idx].hasMine) {
            cells[idx] = cells[idx].copy(hasMine = true)
            minesPlaced++
        }
    }

    // Calculate neighbor counts
    fun getNeighbors(idx: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        val row = idx / gridSize
        val col = idx % gridSize
        for (r in (row - 1)..(row + 1)) {
            for (c in (col - 1)..(col + 1)) {
                if (r in 0 until gridSize && c in 0 until gridSize) {
                    val nIdx = r * gridSize + c
                    if (nIdx != idx) neighbors.add(nIdx)
                }
            }
        }
        return neighbors
    }

    return cells.mapIndexed { idx, cell ->
        if (cell.hasMine) cell
        else {
            val count = getNeighbors(idx).count { cells[it].hasMine }
            cell.copy(neighborMines = count)
        }
    }
}
