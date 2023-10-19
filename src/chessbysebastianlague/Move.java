package chessbysebastianlague;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author yakir
 */
public class Move {

    int StartSquare;
    int TargetSquare;
    int MoveFlag;
    boolean Promotion;

    static class Flag {

        public static final int NONE = 0;
        public static final int EN_PASSANT_CAPTURE = 1;
        public static final int CASTLING = 2;
        public static final int PROMOTE_TO_QUEEN = 3;
        public static final int PROMOTE_TO_KNIGHT = 4;
        public static final int PROMOTE_TO_ROOK = 5;
        public static final int PROMOTE_TO_BISHOP = 6;
        public static final int PAWN_TWO_FORWARD = 7;
    }

    short moveValue;

    static final short START_SQUARE_MASK = 0b0000000000111111;
    static final short TARGET_SQUARE_MASK = 0b0000111111000000;
    static final short FLAG_MASK = 0b111100000000000;

    public Move(short moveValue) {
        this.moveValue = moveValue;
    }

    public Move(int startSquare, int targetSquare) {
        moveValue = (short) (startSquare | targetSquare << 6);
    }

    public Move(int startSquare, int targetSquare, int flag) {
        moveValue = (short) (startSquare | targetSquare << 6 | flag << 12);
    }

    public int getStartSquare() {
        return moveValue & START_SQUARE_MASK;
    }

    public int getTargetSquare() {
        return (moveValue & TARGET_SQUARE_MASK) >> 6;
    }

    public boolean IsPromotion() {
        int flag = MoveFlag;
        return flag == Flag.PROMOTE_TO_QUEEN || flag == Flag.PROMOTE_TO_ROOK || flag == Flag.PROMOTE_TO_KNIGHT || flag == Flag.PROMOTE_TO_BISHOP;
    }

    public int getMoveFlag() {
        return moveValue >> 12;
    }

    public int PromotionPieceType() {
        return switch (MoveFlag) {
            case Flag.PROMOTE_TO_ROOK -> Piece.ROOK;
            case Flag.PROMOTE_TO_KNIGHT -> Piece.KNIGHT;
            case Flag.PROMOTE_TO_BISHOP -> Piece.BISHOP;
            case Flag.PROMOTE_TO_QUEEN -> Piece.QUEEN;
            default -> Piece.NONE;
        };
    }
    
    public static Move InvalidMove(){
        return new Move((short)0);
    }
    
    public static boolean SameMove(Move a, Move b){
        return a.moveValue == b.moveValue;
    }
    
    public short getValue(){
        return moveValue;
    }
    
    public boolean isInvalid(){
        return moveValue == 0;
    }
    
    public String getName(){
        return BoardRepresentation.SquareNameFromIndex (StartSquare) + "-" + BoardRepresentation.SquareNameFromIndex (TargetSquare);
    }

}
