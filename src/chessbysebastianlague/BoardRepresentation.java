package chessbysebastianlague;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author yakir
 */
class BoardRepresentation {

    public static final String FILE_NAMES = "abcdefgh";
    public static final String RANK_NAMES = "12345678";

    public static final int A1 = 0;
    public static final int B1 = 1;
    public static final int C1 = 2;
    public static final int D1 = 3;
    public static final int E1 = 4;
    public static final int F1 = 5;
    public static final int G1 = 6;
    public static final int H1 = 7;

    public static final int A8 = 56;
    public static final int B8 = 57;
    public static final int C8 = 58;
    public static final int D8 = 59;
    public static final int E8 = 60;
    public static final int F8 = 61;
    public static final int G8 = 62;
    public static final int H8 = 63;

    // Rank (0 to 7) of square 
    public static int RankIndex(int squareIndex) {
        return squareIndex >> 3;
    }

    // File (0 to 7) of square 
    public static int FileIndex(int squareIndex) {
        return squareIndex & 0b000111;
    }

    public static int IndexFromCoord(int fileIndex, int rankIndex) {
        return rankIndex * 8 + fileIndex;
    }

    public static int IndexFromCoord(Coord coord) {
        return IndexFromCoord(coord.fileIndex, coord.rankIndex);
    }

    public static Coord CoordFromIndex(int squareIndex) {
        return new Coord(FileIndex(squareIndex), RankIndex(squareIndex));
    }

    public static boolean LightSquare(int fileIndex, int rankIndex) {
        return (fileIndex + rankIndex) % 2 != 0;
    }

    public static String SquareNameFromCoordinate(int fileIndex, int rankIndex) {
        return FILE_NAMES.charAt(fileIndex) + "" + (rankIndex + 1);
    }

    public static String SquareNameFromIndex(int squareIndex) {
        return SquareNameFromCoordinate(CoordFromIndex(squareIndex));
    }

    public static String SquareNameFromCoordinate(Coord coord) {
        return SquareNameFromCoordinate(coord.fileIndex, coord.rankIndex);
    }
}
