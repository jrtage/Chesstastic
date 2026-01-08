package com.chesstastic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chesstastic.game.ChessGame
import com.chesstastic.game.Piece
import com.chesstastic.game.PieceColor
import com.chesstastic.game.PieceType
import com.chesstastic.game.Square

@Composable
fun ChessBoard(
    game: ChessGame,
    onMove: (Square, Square) -> Unit,
    modifier: Modifier = Modifier
) {
    // 8x8 Grid
    val selectedSquare = remember { androidx.compose.runtime.mutableStateOf<Square?>(null) }

    Column(modifier = modifier.aspectRatio(1f)) {
        for (row in 7 downTo 0) { // Rank 8 at top, Rank 1 at bottom
            androidx.compose.foundation.layout.Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) { // File A to H
                    val square = Square(col, row)
                    val isLight = (col + row) % 2 != 0
                    val isSelected = selectedSquare.value == square
                    
                    val baseColor = if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)
                    val bgColor = if (isSelected) Color(0xFFBACA44) else baseColor // Highlight if selected

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(bgColor)
                            .clickable {
                                val currentSelection = selectedSquare.value
                                if (currentSelection == null) {
                                    // Select a piece if it's ours
                                    val piece = game.getPiece(square)
                                    if (piece != null && piece.color == game.turn) {
                                        selectedSquare.value = square
                                    }
                                } else {
                                    if (currentSelection == square) {
                                        // Deselect
                                        selectedSquare.value = null
                                    } else {
                                        // Attempt Move
                                        onMove(currentSelection, square)
                                        selectedSquare.value = null
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val piece = game.getPiece(square)
                        if (piece != null) {
                            PieceView(piece)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceView(piece: Piece) {
    // For MVP using simple text or drawing. 
    // Ideally use SVGs or VectorDrawables.
    // Using Unicode chess pieces for simplicity.
    val symbol = when(piece.color) {
        PieceColor.WHITE -> when(piece.type) {
            PieceType.KING -> "♔"
            PieceType.QUEEN -> "♕"
            PieceType.ROOK -> "♖"
            PieceType.BISHOP -> "♗"
            PieceType.KNIGHT -> "♘"
            PieceType.PAWN -> "♙"
        }
        PieceColor.BLACK -> when(piece.type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
    }
    
    Text(
        text = symbol,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = if (piece.color == PieceColor.BLACK) Color.Black else Color.White // Outline needed for white pieces on light bg?
        // Actually standard unicode pieces are black/white filled.
        // Let's stick to black text for visibility on all squares for now, relying on the glyph itself
    )
    
    // Better visualization:
    // White pieces usually white with black outline. Black pieces black.
    // Unicode white pieces are outlines effectively.
    // Let's assume user accepts this for MVP.
}
