package com.chesstastic.game

enum class PieceType {
    PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
}

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class Piece(val type: PieceType, val color: PieceColor, val hasMoved: Boolean = false)

data class Square(val col: Int, val row: Int) {
    override fun toString(): String {
        return "${(col + 'a'.code).toChar()}${row + 1}"
    }
}

// Helper to create squares from string e.g. "e2"
fun String.toSquare(): Square? {
    if (this.length != 2) return null
    val col = this[0] - 'a'
    val row = this[1] - '1'
    if (col !in 0..7 || row !in 0..7) return null
    return Square(col, row)
}
