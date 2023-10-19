package chessbysebastianlague;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author yakir
 */
public class Evaluation {

    public static final int pawnValue = 100;
    public static final int knightValue = 300;
    public static final int bishopValue = 320;
    public static final int rookValue = 500;
    public static final int queenValue = 900;

    static final double endgameMaterialStart = rookValue * 2 + bishopValue + knightValue;
    Board board;

    // Performs static evaluation of the current position.
    // The position is assumed to be 'quiet', i.e no captures are available that could drastically affect the evaluation.
    // The score that's returned is given from the perspective of whoever's turn it is to move.
    // So a positive score means the player who's turn it is to move has an advantage, while a negative score indicates a disadvantage.
    public int Evaluate(Board board) {
        this.board = board;
        int whiteEval = 0;
        int blackEval = 0;

        int whiteMaterial = CountMaterial(Board.WHITE_INDEX);
        int blackMaterial = CountMaterial(Board.BLACK_INDEX);

        int whiteMaterialWithoutPawns = whiteMaterial - board.pawns[Board.WHITE_INDEX].Count * pawnValue;
        int blackMaterialWithoutPawns = blackMaterial - board.pawns[Board.BLACK_INDEX].Count * pawnValue;
        double whiteEndgamePhaseWeight = EndgamePhaseWeight(whiteMaterialWithoutPawns);
        double blackEndgamePhaseWeight = EndgamePhaseWeight(blackMaterialWithoutPawns);

        whiteEval += whiteMaterial;
        blackEval += blackMaterial;
        whiteEval += MopUpEval(Board.WHITE_INDEX, Board.BLACK_INDEX, whiteMaterial, blackMaterial, blackEndgamePhaseWeight);
        blackEval += MopUpEval(Board.BLACK_INDEX, Board.WHITE_INDEX, blackMaterial, whiteMaterial, whiteEndgamePhaseWeight);

        whiteEval += EvaluatePieceSquareTables(Board.WHITE_INDEX, blackEndgamePhaseWeight);
        blackEval += EvaluatePieceSquareTables(Board.BLACK_INDEX, whiteEndgamePhaseWeight);

        int eval = whiteEval - blackEval;

        int perspective = (board.WhiteToMove) ? 1 : -1;
        return eval * perspective;
    }

    double EndgamePhaseWeight(int materialCountWithoutPawns) {
        double multiplier = 1 / endgameMaterialStart;
        return 1 - Math.min(1, materialCountWithoutPawns * multiplier);
    }

    int MopUpEval(int friendlyIndex, int opponentIndex, int myMaterial, int opponentMaterial, double endgameWeight) {
        int mopUpScore = 0;
        if (myMaterial > opponentMaterial + pawnValue * 2 && endgameWeight > 0) {

            int friendlyKingSquare = board.KingSquare[friendlyIndex];
            int opponentKingSquare = board.KingSquare[opponentIndex];
            mopUpScore += PrecomputedMoveData.centreManhattanDistance[opponentKingSquare] * 10;
            // use ortho dst to promote direct opposition
            mopUpScore += (14 - PrecomputedMoveData.NumRookMovesToReachSquare(friendlyKingSquare, opponentKingSquare)) * 4;

            return (int) (mopUpScore * endgameWeight);
        }
        return 0;
    }

    int CountMaterial(int colourIndex) {
        int material = 0;
        material += board.pawns[colourIndex].Count * pawnValue;
        material += board.knights[colourIndex].Count * knightValue;
        material += board.bishops[colourIndex].Count * bishopValue;
        material += board.rooks[colourIndex].Count * rookValue;
        material += board.queens[colourIndex].Count * queenValue;

        return material;
    }

    int EvaluatePieceSquareTables(int colourIndex, double endgamePhaseWeight) {
        int value = 0;
        boolean isWhite = colourIndex == Board.WHITE_INDEX;
        value += EvaluatePieceSquareTable(PieceSquareTable.pawns, board.pawns[colourIndex], isWhite);
        value += EvaluatePieceSquareTable(PieceSquareTable.rooks, board.rooks[colourIndex], isWhite);
        value += EvaluatePieceSquareTable(PieceSquareTable.knights, board.knights[colourIndex], isWhite);
        value += EvaluatePieceSquareTable(PieceSquareTable.bishops, board.bishops[colourIndex], isWhite);
        value += EvaluatePieceSquareTable(PieceSquareTable.queens, board.queens[colourIndex], isWhite);
        int kingEarlyPhase = PieceSquareTable.Read(PieceSquareTable.kingMiddle, board.KingSquare[colourIndex], isWhite);
        value += (int) (kingEarlyPhase * (1 - endgamePhaseWeight));
        //value += PieceSquareTable.Read (PieceSquareTable.kingMiddle, board.KingSquare[colourIndex], isWhite);

        return value;
    }

    static int EvaluatePieceSquareTable(int[] table, PieceList pieceList, boolean isWhite) {
        int value = 0;
        for (int i = 0; i < pieceList.Count; i++) {
            value += PieceSquareTable.Read(table, pieceList.getStartSquare(i), isWhite);
        }
        return value;
    }
}
