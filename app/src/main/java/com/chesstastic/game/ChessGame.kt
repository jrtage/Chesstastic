package com.chesstastic.game

import kotlin.math.abs
import kotlin.math.max

class ChessGame {
    private val board = Array(8) { arrayOfNulls<Piece>(8) }
    var turn: PieceColor = PieceColor.WHITE
        private set
    var isCheck: Boolean = false
        private set
    var isCheckmate: Boolean = false
        private set
    
    // Square that can be captured via En Passant (the square BEHIND the pawn that moved 2 steps)
    var enPassantTarget: Square? = null
        private set
    
    private val moves = mutableListOf<String>()

    init {
        resetBoard()
    }

    fun resetBoard() {
        for (r in 0..7) {
            for (c in 0..7) {
                board[r][c] = null
            }
        }
        
        for (c in 0..7) {
            board[1][c] = Piece(PieceType.PAWN, PieceColor.WHITE)
            board[6][c] = Piece(PieceType.PAWN, PieceColor.BLACK)
        }

        val setupRow = { row: Int, color: PieceColor ->
            board[row][0] = Piece(PieceType.ROOK, color)
            board[row][1] = Piece(PieceType.KNIGHT, color)
            board[row][2] = Piece(PieceType.BISHOP, color)
            board[row][3] = Piece(PieceType.QUEEN, color)
            board[row][4] = Piece(PieceType.KING, color)
            board[row][5] = Piece(PieceType.BISHOP, color)
            board[row][6] = Piece(PieceType.KNIGHT, color)
            board[row][7] = Piece(PieceType.ROOK, color)
        }

        setupRow(0, PieceColor.WHITE)
        setupRow(7, PieceColor.BLACK)

        turn = PieceColor.WHITE
        isCheck = false
        isCheckmate = false
        enPassantTarget = null
        moves.clear()
    }

    fun getPiece(square: Square): Piece? {
        if (!isValidSquare(square)) return null
        return board[square.row][square.col]
    }

    private fun isValidSquare(square: Square): Boolean {
        return square.col in 0..7 && square.row in 0..7
    }

    fun makeMove(from: Square, to: Square): Boolean {
        if (!isValidMove(from, to, turn)) return false

        // Logic check for special moves
        // 1. Castling?
        val fromPiece = board[from.row][from.col]!!
        val isCastling = fromPiece.type == PieceType.KING && abs(to.col - from.col) == 2
        
        // 2. En Passant?
        val isEnPassant = fromPiece.type == PieceType.PAWN && 
                          from.col != to.col && 
                          board[to.row][to.col] == null

        // Execute Move (Optimistic)
        val capturedFn = {
            if (isEnPassant) {
                // Return captured pawn from adjacent square
                val capRow = from.row
                val capCol = to.col
                board[capRow][capCol]
            } else {
                board[to.row][to.col]
            }
        }
        val targetPiece = capturedFn()

        // Apply changes
        board[to.row][to.col] = fromPiece.copy(hasMoved = true)
        board[from.row][from.col] = null
        if (isEnPassant) {
            board[from.row][to.col] = null // Remove captured pawn
        }
        
        // Handle Castling Rook
        if (isCastling) {
            val rookCol = if (to.col > from.col) 7 else 0
            val rookDestCol = if (to.col > from.col) 5 else 3
            val rook = board[from.row][rookCol]
            // Move rook
            board[from.row][rookDestCol] = rook?.copy(hasMoved = true)
            board[from.row][rookCol] = null
        }

        // Verify Self-Check
        if (isKingInCheck(turn)) {
            // Revert!
            board[from.row][from.col] = fromPiece
            board[to.row][to.col] = if (isEnPassant) null else targetPiece
            
            if (isEnPassant) {
                board[from.row][to.col] = targetPiece
            }
            if (isCastling) {
                val rookCol = if (to.col > from.col) 7 else 0
                val rookDestCol = if (to.col > from.col) 5 else 3
                val rook = board[from.row][rookDestCol]
                board[from.row][rookCol] = rook?.copy(hasMoved = false) // technically assumed false if castling valid
                board[from.row][rookDestCol] = null
            }
            return false
        }
        
        // Update En Passant Target
        enPassantTarget = if (fromPiece.type == PieceType.PAWN && abs(to.row - from.row) == 2) {
             val midRow = (from.row + to.row) / 2
             Square(from.col, midRow)
        } else {
            null
        }

        moves.add("${from}-${to}")
        turn = turn.opposite()
        isCheck = isKingInCheck(turn)
        
        return true
    }

    private fun isValidMove(from: Square, to: Square, color: PieceColor): Boolean {
        if (!isValidSquare(from) || !isValidSquare(to)) return false
        if (from == to) return false
        val piece = board[from.row][from.col] ?: return false
        if (piece.color != color) return false

        val target = board[to.row][to.col]
        if (target != null && target.color == piece.color) return false

        val dx = to.col - from.col
        val dy = to.row - from.row
        val absDx = abs(dx)
        val absDy = abs(dy)

        return when (piece.type) {
            PieceType.PAWN -> isPawnMoveValid(piece, from, to, dx, dy, target)
            PieceType.ROOK -> (dx == 0 || dy == 0) && isPathClear(from, to)
            PieceType.BISHOP -> (absDx == absDy) && isPathClear(from, to)
            PieceType.QUEEN -> (dx == 0 || dy == 0 || absDx == absDy) && isPathClear(from, to)
            PieceType.KNIGHT -> (absDx == 1 && absDy == 2) || (absDx == 2 && absDy == 1)
            PieceType.KING -> isKingMoveValid(piece, from, to, absDx, absDy)
        }
    }
    
    private fun isPawnMoveValid(piece: Piece, from: Square, to: Square, dx: Int, dy: Int, target: Piece?): Boolean {
        val direction = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRow = if (piece.color == PieceColor.WHITE) 1 else 6
        
        // Forward 1
        if (dx == 0 && dy == direction && target == null) return true
        
        // Forward 2
        if (dx == 0 && dy == 2 * direction && target == null) {
            return from.row == startRow && board[from.row + direction][from.col] == null
        }
        
        // Capture
        if (abs(dx) == 1 && dy == direction) {
            if (target != null && target.color != piece.color) return true
            // En Passant
            if (target == null && enPassantTarget != null && enPassantTarget == to) return true
        }
        
        return false
    }
    
    private fun isKingMoveValid(piece: Piece, from: Square, to: Square, absDx: Int, absDy: Int): Boolean {
        // Normal move
        if (absDx <= 1 && absDy <= 1) return true
        
        // Castling
        if (!piece.hasMoved && absDy == 0 && absDx == 2) {
            // Cannot castle out of check
            if (isKingInCheck(piece.color)) return false
            
            val rookCol = if (to.col > from.col) 7 else 0
            val rook = board[from.row][rookCol]
            if (rook == null || rook.type != PieceType.ROOK || rook.hasMoved) return false
            
            // Path must be clear
            if (!isPathClear(from, Square(rookCol, from.row))) return false
            
            // King cannot pass through check
            val midCol = (from.col + to.col) / 2
            // We need to check if mid square is attacked.
            // Simplified: temporarily make the move to mid square and check isKingInCheck? 
            // Better: Check if opponent attacks 'midCol'
            // Re-using logic: 
            // 1. Check if we can move 1 step safely?
            // Expensive.
            // Let's implement `isSquareAttacked` helper.
            if (isSquareAttacked(Square(midCol, from.row), piece.color)) return false
            
            return true
        }
        return false
    }

    private fun isPathClear(from: Square, to: Square): Boolean {
        val dx = to.col - from.col
        val dy = to.row - from.row
        val steps = max(abs(dx), abs(dy))
        val stepX = if (dx == 0) 0 else dx / abs(dx)
        val stepY = if (dy == 0) 0 else dy / abs(dy)

        var curCol = from.col + stepX
        var curRow = from.row + stepY
        
        while (curCol != to.col || curRow != to.row) {
            if (board[curRow][curCol] != null) return false
            curCol += stepX
            curRow += stepY
        }
        return true
    }

    private fun isKingInCheck(color: PieceColor): Boolean {
        var kingSquare: Square? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    kingSquare = Square(c, r)
                    break
                }
            }
        }
        return if (kingSquare != null) isSquareAttacked(kingSquare, color) else false
    }
    
    // Check if a square is attacked by opponent
    private fun isSquareAttacked(square: Square, myColor: PieceColor): Boolean {
        val opponent = myColor.opposite()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == opponent) {
                    // Check if this piece CAN attack target
                    // Note: Pawns attack differently than they move!
                    if (p.type == PieceType.PAWN) {
                        val direction = if (opponent == PieceColor.WHITE) 1 else -1
                        val dx = abs(square.col - c)
                        val dy = square.row - r
                        if (dx == 1 && dy == direction) return true
                    } else {
                         // Standard move check, ignoring "target is empty" for slider pieces?
                         // Actually isValidMove handles capture logic, so if target has MY piece it says false(protected),
                         // but here we want to know if it IS ATTACKED.
                         // So we simulate target being opponent's piece? 
                         // Or just reuse logic but ignore target content check?
                         // Reusing logic is safest but tricky with Pawn.
                         
                         // Hack: temporarily set target square to NOT contain my piece if it does?
                         // Or just strictly check geometry + path.
                         if (isValidAttackGeometry(p, Square(c, r), square)) return true
                    }
                }
            }
        }
        return false
    }
    
    private fun isValidAttackGeometry(piece: Piece, from: Square, to: Square): Boolean {
         val dx = to.col - from.col
         val dy = to.row - from.row
         val absDx = abs(dx)
         val absDy = abs(dy)
         
         return when (piece.type) {
             PieceType.PAWN -> false // Handled separately
             PieceType.ROOK -> (dx == 0 || dy == 0) && isPathClear(from, to)
             PieceType.BISHOP -> (absDx == absDy) && isPathClear(from, to)
             PieceType.QUEEN -> (dx == 0 || dy == 0 || absDx == absDy) && isPathClear(from, to)
             PieceType.KNIGHT -> (absDx == 1 && absDy == 2) || (absDx == 2 && absDy == 1)
             PieceType.KING -> absDx <= 1 && absDy <= 1
         }
    }

    // Convert to FEN (simplified)
    fun toFen(): String {
        return "" 
    }
}
