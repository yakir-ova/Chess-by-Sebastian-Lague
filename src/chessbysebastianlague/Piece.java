package chessbysebastianlague;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author yakir
 */
public class Piece {

    public static final int NONE = 0;
    public static final int KING = 1;
    public static final int PAWN = 2;
    public static final int KNIGHT = 3;
    public static final int BISHOP = 5;
    public static final int ROOK = 6;
    public static final int QUEEN = 7;

    public final static int WHITE = 8;
    public final static int BLACK = 16;

    static final int TYPE_MASK = 0b00111;
    static final int BLACK_MASK = 0b10000;
    static final int WHITE_MASK = 0b01000;
    static final int COLOR_MASK = WHITE_MASK | BLACK_MASK;

    public static boolean IsColor(int piece, int color) {
        return (piece & COLOR_MASK) == color;
    }
    
    public static int color(int piece){
        return piece & COLOR_MASK;
    }

    public static int PieceType(int piece) {
        return piece & TYPE_MASK;
    }

    public static boolean IsRookOrQueen(int piece) {
        return (piece & 0b110) == 0b110;
    }

    public static boolean IsBishopOrQueen(int piece) {
        return (piece & 0b101) == 0b101;
    }
    
    public static boolean IsSlidingPiece(int piece) {
        return (piece & 0b100) != 0;
    }

}
