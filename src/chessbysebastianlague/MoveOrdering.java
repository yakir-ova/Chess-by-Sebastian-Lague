package chessbysebastianlague;

import java.util.ArrayList;
import java.util.Comparator;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author yakir
 */
public class MoveOrdering {

    int[] moveScores;
    public static final int MAX_MOVE_COUNT = 218;

    public static final int squareControlledByOpponentPawnPenalty = 350;
    public static final int capturedPieceValueMultiplier = 10;

    MoveGenerator moveGenerator;
    TranspositionTable transpositionTable;
    Move invalidMove;

    public MoveOrdering(MoveGenerator moveGenerator, TranspositionTable tt) {
        moveScores = new int[MAX_MOVE_COUNT];
        this.moveGenerator = moveGenerator;
        this.transpositionTable = tt;
        invalidMove = Move.InvalidMove();
    }

    public void OrderMoves(Board board, ArrayList<Move> moves, boolean useTT) {
        Move hashMove = invalidMove;
        if (useTT) {
            hashMove = transpositionTable.GetStoredMove();
        }

        for (int i = 0; i < moves.size(); i++) {
            int score = 0;
            int movePieceType = Piece.PieceType(board.Square[moves.get(i).StartSquare]);
            int capturePieceType = Piece.PieceType(board.Square[moves.get(i).TargetSquare]);
            int flag = moves.get(i).MoveFlag;

            if (capturePieceType != Piece.NONE) {
                // Order moves to try capturing the most valuable opponent piece with least valuable of own pieces first
                // The capturedPieceValueMultiplier is used to make even 'bad' captures like QxP rank above non-captures
                score = capturedPieceValueMultiplier * GetPieceValue(capturePieceType) - GetPieceValue(movePieceType);
            }

            if (movePieceType == Piece.PAWN) {

                switch (flag) {
                    case Move.Flag.PROMOTE_TO_QUEEN ->
                        score += Evaluation.queenValue;
                    case Move.Flag.PROMOTE_TO_KNIGHT ->
                        score += Evaluation.knightValue;
                    case Move.Flag.PROMOTE_TO_ROOK ->
                        score += Evaluation.rookValue;
                    case Move.Flag.PROMOTE_TO_BISHOP ->
                        score += Evaluation.bishopValue;
                    default -> {
                    }
                }
            } else {
                // Penalize moving piece to a square attacked by opponent pawn
                if (BitBoardUtility.ContainsSquare(moveGenerator.opponentPawnAttackMap, moves.get(i).TargetSquare)) {
                    score -= squareControlledByOpponentPawnPenalty;
                }
            }
            if (Move.SameMove(moves.get(i), hashMove)) {
                score += 10000;
            }

            moveScores[i] = score;
        }

//        Sort(moves);
    }

    static int GetPieceValue(int pieceType) {
        return switch (pieceType) {
            case Piece.QUEEN ->
                Evaluation.queenValue;
            case Piece.ROOK ->
                Evaluation.rookValue;
            case Piece.KNIGHT ->
                Evaluation.knightValue;
            case Piece.BISHOP ->
                Evaluation.bishopValue;
            case Piece.PAWN ->
                Evaluation.pawnValue;
            default ->
                0;
        };
    }

//    void Sort(ArrayList<Move> moves) {
//        ArrayList<Move> sortedMoves = new ArrayList<>();
//        // Sort the moves list based on scores
//        for (int i = 0; i < moves.size() - 1; i++) {
//            for (int j = i + 1; j > 0; j--) {
//                int swapIndex = j - 1;
//                if (moveScores[swapIndex] < moveScores[j]) {
//                    swap(j, swapIndex);
//                    
//                    (moves[j], moves[swapIndex]) = (moves[swapIndex], moves[j]);
//						(moveScores[j], moveScores[swapIndex]) = (moveScores[swapIndex], moveScores[j]);
//					}
//				}
//			}
//		}
//    public void swap(int from, int to, ArrayList<Move> moves, ArrayList<Move> sortedMoves) {
//        Comparator comp = new Comparator() {
//            @Override
//            public int compare(Object o1, Object o2) {
//                if(moveScores[to] < moveScores[from])
//            }
//        }
//    }
}
