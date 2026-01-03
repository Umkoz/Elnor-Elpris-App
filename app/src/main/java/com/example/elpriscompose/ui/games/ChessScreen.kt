package com.example.elpriscompose.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChessPiece(
    val color: Char, // 'w' or 'b'
    val type: Char   // 'p', 'r', 'n', 'b', 'q', 'k'
)

@Composable
fun ChessScreen(onBack: () -> Unit) {
    var board by remember { mutableStateOf(createInitialBoard()) }
    var turn by remember { mutableStateOf('w') }
    var selectedSquare by remember { mutableStateOf<Int?>(null) }
    var possibleMoves by remember { mutableStateOf<List<Int>>(emptyList()) }
    var winner by remember { mutableStateOf<Char?>(null) }

    fun resetGame() {
        board = createInitialBoard()
        turn = 'w'
        selectedSquare = null
        possibleMoves = emptyList()
        winner = null
    }

    fun handleSquareClick(idx: Int) {
        if (winner != null) return

        val piece = board[idx]

        // If no piece selected
        if (selectedSquare == null) {
            if (piece != null && piece.color == turn) {
                selectedSquare = idx
                possibleMoves = getValidMoves(idx, board)
            }
        } else {
            // If clicking a possible move
            if (possibleMoves.contains(idx)) {
                val newBoard = board.toMutableList()
                
                // Check if capturing king
                if (newBoard[idx]?.type == 'k') {
                    winner = turn
                }

                // Move piece
                newBoard[idx] = newBoard[selectedSquare!!]
                newBoard[selectedSquare!!] = null

                // Auto-promote pawn to queen
                val movedPiece = newBoard[idx]
                if (movedPiece?.type == 'p' && (idx < 8 || idx >= 56)) {
                    newBoard[idx] = ChessPiece(movedPiece.color, 'q')
                }

                board = newBoard
                turn = if (turn == 'w') 'b' else 'w'
                selectedSquare = null
                possibleMoves = emptyList()
            } else {
                // Select different piece or deselect
                if (piece != null && piece.color == turn) {
                    selectedSquare = idx
                    possibleMoves = getValidMoves(idx, board)
                } else {
                    selectedSquare = null
                    possibleMoves = emptyList()
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF1F5F9))
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
                        tint = Color(0xFF64748B)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCHACK",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = Color(0xFF1E293B)
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (turn == 'w') Color.White else Color(0xFF1E293B)
                        )
                    ) {
                        Text(
                            text = if (turn == 'w') "Vits tur" else "Svarts tur",
                            color = if (turn == 'w') Color(0xFF1E293B) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = { resetGame() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Ny omgång",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            // Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
                ) {
                    Box {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(8),
                            userScrollEnabled = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            itemsIndexed(board) { idx, piece ->
                                ChessSquare(
                                    idx = idx,
                                    piece = piece,
                                    isSelected = selectedSquare == idx,
                                    isPossibleMove = possibleMoves.contains(idx),
                                    isCapture = possibleMoves.contains(idx) && board[idx] != null,
                                    onClick = { handleSquareClick(idx) }
                                )
                            }
                        }

                        // Winner overlay
                        if (winner != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0F172A).copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Text(
                                            text = "🏆",
                                            fontSize = 48.sp
                                        )
                                        Text(
                                            text = if (winner == 'w') "Vit vann!" else "Svart vann!",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF1E293B)
                                        )
                                        Button(
                                            onClick = { resetGame() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF3B82F6)
                                            ),
                                            shape = RoundedCornerShape(24.dp),
                                            modifier = Modifier.padding(top = 16.dp)
                                        ) {
                                            Text(
                                                text = "Spela igen",
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
}

@Composable
private fun ChessSquare(
    idx: Int,
    piece: ChessPiece?,
    isSelected: Boolean,
    isPossibleMove: Boolean,
    isCapture: Boolean,
    onClick: () -> Unit
) {
    val row = idx / 8
    val col = idx % 8
    val isBlackSquare = (row + col) % 2 == 1

    val backgroundColor = when {
        isSelected -> Color(0xFF3B82F6).copy(alpha = 0.5f)
        isBlackSquare -> Color(0xFF64748B)
        else -> Color(0xFFE2E8F0)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Move indicator
        if (isPossibleMove && !isCapture) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.5f))
            )
        }

        // Capture indicator
        if (isCapture) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEF4444).copy(alpha = 0.3f))
            )
        }

        // Piece
        if (piece != null) {
            val symbol = getPieceSymbol(piece)
            Text(
                text = symbol,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                color = if (piece.color == 'w') Color.White else Color.Black
            )
        }
    }
}

private fun getPieceSymbol(piece: ChessPiece): String {
    return when {
        piece.color == 'w' && piece.type == 'p' -> "♙"
        piece.color == 'w' && piece.type == 'r' -> "♖"
        piece.color == 'w' && piece.type == 'n' -> "♘"
        piece.color == 'w' && piece.type == 'b' -> "♗"
        piece.color == 'w' && piece.type == 'q' -> "♕"
        piece.color == 'w' && piece.type == 'k' -> "♔"
        piece.color == 'b' && piece.type == 'p' -> "♟"
        piece.color == 'b' && piece.type == 'r' -> "♜"
        piece.color == 'b' && piece.type == 'n' -> "♞"
        piece.color == 'b' && piece.type == 'b' -> "♝"
        piece.color == 'b' && piece.type == 'q' -> "♛"
        piece.color == 'b' && piece.type == 'k' -> "♚"
        else -> ""
    }
}

private fun createInitialBoard(): List<ChessPiece?> {
    val board = MutableList<ChessPiece?>(64) { null }
    
    // Black pieces (top)
    val backRow = listOf('r', 'n', 'b', 'q', 'k', 'b', 'n', 'r')
    for (i in 0..7) {
        board[i] = ChessPiece('b', backRow[i])
        board[8 + i] = ChessPiece('b', 'p')
    }
    
    // White pieces (bottom)
    for (i in 0..7) {
        board[48 + i] = ChessPiece('w', 'p')
        board[56 + i] = ChessPiece('w', backRow[i])
    }
    
    return board
}

private fun getValidMoves(idx: Int, board: List<ChessPiece?>): List<Int> {
    val piece = board[idx] ?: return emptyList()
    val moves = mutableListOf<Int>()
    val row = idx / 8
    val col = idx % 8

    fun addMoveIfValid(r: Int, c: Int): Boolean {
        if (r !in 0..7 || c !in 0..7) return false
        val targetIdx = r * 8 + c
        val target = board[targetIdx]
        
        if (target == null) {
            moves.add(targetIdx)
            return true // Can continue sliding
        } else if (target.color != piece.color) {
            moves.add(targetIdx) // Capture
            return false // Stop sliding
        }
        return false // Blocked by own piece
    }

    val orthoDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    val knightMoves = listOf(-2 to -1, -2 to 1, 2 to -1, 2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2)

    when (piece.type) {
        'p' -> {
            val dir = if (piece.color == 'w') -1 else 1
            val startRow = if (piece.color == 'w') 6 else 1
            
            // Move forward
            val forwardIdx = (row + dir) * 8 + col
            if (row + dir in 0..7 && board[forwardIdx] == null) {
                moves.add(forwardIdx)
                // Double move from start
                if (row == startRow) {
                    val doubleIdx = (row + dir * 2) * 8 + col
                    if (board[doubleIdx] == null) {
                        moves.add(doubleIdx)
                    }
                }
            }
            
            // Capture diagonally
            listOf(-1, 1).forEach { dc ->
                val tr = row + dir
                val tc = col + dc
                if (tr in 0..7 && tc in 0..7) {
                    val target = board[tr * 8 + tc]
                    if (target != null && target.color != piece.color) {
                        moves.add(tr * 8 + tc)
                    }
                }
            }
        }
        'n' -> {
            knightMoves.forEach { (dr, dc) ->
                addMoveIfValid(row + dr, col + dc)
            }
        }
        'k' -> {
            (orthoDirs + diagDirs).forEach { (dr, dc) ->
                addMoveIfValid(row + dr, col + dc)
            }
        }
        'r' -> {
            orthoDirs.forEach { (dr, dc) ->
                var r = row + dr
                var c = col + dc
                while (addMoveIfValid(r, c)) {
                    r += dr
                    c += dc
                }
            }
        }
        'b' -> {
            diagDirs.forEach { (dr, dc) ->
                var r = row + dr
                var c = col + dc
                while (addMoveIfValid(r, c)) {
                    r += dr
                    c += dc
                }
            }
        }
        'q' -> {
            (orthoDirs + diagDirs).forEach { (dr, dc) ->
                var r = row + dr
                var c = col + dc
                while (addMoveIfValid(r, c)) {
                    r += dr
                    c += dc
                }
            }
        }
    }

    return moves
}
