package se.elnor.elprisnu.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun SudokuScreen(onBack: () -> Unit) {
    var puzzle by remember { mutableStateOf(generateSudoku()) }
    var board by remember { mutableStateOf(puzzle.first.toMutableList()) }
    var solution by remember { mutableStateOf(puzzle.second) }
    var initialBoard by remember { mutableStateOf(puzzle.first.toList()) }
    var selectedCell by remember { mutableStateOf<Int?>(null) }
    var mistakes by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }

    fun startNewGame() {
        puzzle = generateSudoku()
        board = puzzle.first.toMutableList()
        solution = puzzle.second
        initialBoard = puzzle.first.toList()
        selectedCell = null
        mistakes = 0
        isComplete = false
    }

    fun handleInput(num: Int) {
        val cell = selectedCell ?: return
        if (isComplete) return
        if (initialBoard[cell] != 0) return // Can't change initial values

        val newBoard = board.toMutableList()
        
        if (num == 0) {
            newBoard[cell] = 0
            board = newBoard
            return
        }

        if (num != solution[cell]) {
            mistakes++
        }

        newBoard[cell] = num
        board = newBoard

        // Check completion
        if (!newBoard.contains(0)) {
            val isCorrect = newBoard.indices.all { newBoard[it] == solution[it] }
            if (isCorrect) isComplete = true
        }
    }

    fun isRelatedCell(idx: Int): Boolean {
        val selected = selectedCell ?: return false
        val selectedRow = selected / 9
        val selectedCol = selected % 9
        val row = idx / 9
        val col = idx % 9
        return selectedRow == row || selectedCol == col
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
                        text = "Sudoku",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$mistakes misstag",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { startNewGame() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Ny omgång",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sudoku Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    // Board with 3x3 box borders
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(2.dp)
                    ) {
                        Column {
                            for (boxRow in 0..2) {
                                Row {
                                    for (boxCol in 0..2) {
                                        // 3x3 box
                                        Box(
                                            modifier = Modifier
                                                .padding(1.dp)
                                                .background(Color(0xFF64748B))
                                        ) {
                                            Column {
                                                for (r in 0..2) {
                                                    Row {
                                                        for (c in 0..2) {
                                                            val row = boxRow * 3 + r
                                                            val col = boxCol * 3 + c
                                                            val idx = row * 9 + col
                                                            val cell = board[idx]
                                                            val isInitial = initialBoard[idx] != 0
                                                            val isSelected = selectedCell == idx
                                                            val isRelated = isRelatedCell(idx)
                                                            val isError = cell != 0 && cell != solution[idx]

                                                            SudokuCell(
                                                                value = cell,
                                                                isInitial = isInitial,
                                                                isSelected = isSelected,
                                                                isRelated = isRelated,
                                                                isError = isError,
                                                                onClick = { selectedCell = idx }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Win message
                    if (isComplete) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏆", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sudoku Löst!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Number Pad
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(9) { idx ->
                    val num = idx + 1
                    NumberButton(
                        number = num,
                        onClick = { handleInput(num) }
                    )
                }
                item {
                    // Erase button
                    Button(
                        onClick = { handleInput(0) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Radera",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SudokuCell(
    value: Int,
    isInitial: Boolean,
    isSelected: Boolean,
    isRelated: Boolean,
    isError: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isError -> Color(0xFFFEE2E2)
        isRelated -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> Color.White
    }

    val textColor = when {
        isSelected -> Color.White
        isError -> Color(0xFFEF4444)
        isInitial -> Color(0xFF1E293B)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .padding(0.5.dp)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = if (isInitial) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun NumberButton(
    number: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF334155)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(56.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = number.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Sudoku generation functions
private fun generateSudoku(): Pair<List<Int>, List<Int>> {
    val solution = MutableList(81) { 0 }
    
    // Fill diagonal 3x3 boxes first (they don't affect each other)
    for (i in 0..2) {
        fillBox(solution, i * 3, i * 3)
    }
    
    // Solve the rest
    solveSudoku(solution)
    
    // Create puzzle by removing numbers
    val puzzle = solution.toMutableList()
    val attempts = 35 // Number of cells to try to remove
    val indices = (0..80).shuffled().take(attempts)
    indices.forEach { idx ->
        puzzle[idx] = 0
    }
    
    return Pair(puzzle, solution.toList())
}

private fun fillBox(board: MutableList<Int>, rowStart: Int, colStart: Int) {
    val nums = (1..9).shuffled()
    var idx = 0
    for (i in 0..2) {
        for (j in 0..2) {
            board[(rowStart + i) * 9 + (colStart + j)] = nums[idx++]
        }
    }
}

private fun solveSudoku(board: MutableList<Int>): Boolean {
    for (i in 0..80) {
        if (board[i] == 0) {
            for (num in 1..9) {
                if (isValidMove(board, i, num)) {
                    board[i] = num
                    if (solveSudoku(board)) return true
                    board[i] = 0
                }
            }
            return false
        }
    }
    return true
}

private fun isValidMove(board: List<Int>, idx: Int, num: Int): Boolean {
    val row = idx / 9
    val col = idx % 9
    
    // Check row
    for (c in 0..8) {
        if (board[row * 9 + c] == num) return false
    }
    
    // Check column
    for (r in 0..8) {
        if (board[r * 9 + col] == num) return false
    }
    
    // Check 3x3 box
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    for (r in boxRow until boxRow + 3) {
        for (c in boxCol until boxCol + 3) {
            if (board[r * 9 + c] == num) return false
        }
    }
    
    return true
}
