package chessbysebastianlague;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author yakir
 */
class Coord {

    public static int fileIndex;
    public static int rankIndex;

    Coord(int FileIndex, int RankIndex) {
        this.fileIndex = FileIndex;
        this.rankIndex = RankIndex;
    }

    public boolean IsLightSquare() {
        return (fileIndex + rankIndex) % 2 != 0;
    }

    public int CompareTo(Coord other) {
        return (fileIndex == other.fileIndex && rankIndex == other.rankIndex) ? 0 : 1;
    }
}
