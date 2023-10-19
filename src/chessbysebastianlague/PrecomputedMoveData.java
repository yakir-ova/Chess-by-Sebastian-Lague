package chessbysebastianlague;

import java.util.ArrayList;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author yakir
 */
public final class PrecomputedMoveData {
    // First 4 are orthogonal, last 4 are diagonals (N, S, W, E, NW, SE, NE, SW)

    public static int[] directionOffsets = {8, -8, -1, 1, 7, -7, 9, -9};

    // Stores number of moves available in each of the 8 directions for every square on the board
    // Order of directions is: N, S, W, E, NW, SE, NE, SW
    // So for example, if availableSquares[0][1] == 7...
    // that means that there are 7 squares to the north of b1 (the square with index 1 in board array)
    public static int[][] numSquaresToEdge;

    // Stores array of indices for each square a knight can land on from any square on the board
    // So for example, knightMoves[0] is equal to {10, 17}, meaning a knight on a1 can jump to c2 and b3
    public static byte[][] knightMoves;
    public static byte[][] kingMoves;

    // Pawn attack directions for white and black (NW, NE; SW SE)
    public static byte[][] pawnAttackDirections = {
        new byte[]{4, 6},
        new byte[]{7, 5}
    };

    public static int[][] pawnAttacksWhite;
    public static int[][] pawnAttacksBlack;
    public static int[] directionLookup;

    public static long[] kingAttackBitboards;
    public static long[] knightAttackBitboards;
    public static long[][] pawnAttackBitboards;

    public static long[] rookMoves;
    public static long[] bishopMoves;
    public static long[] queenMoves;

    // Aka manhattan distance (answers how many moves for a rook to get from square a to square b)
    public static int[][] orthogonalDistance;
    // Aka chebyshev distance (answers how many moves for a king to get from square a to square b)
    public static int[][] kingDistance;
    public static int[] centreManhattanDistance;

    public static int NumRookMovesToReachSquare(int startSquare, int targetSquare) {
        return orthogonalDistance[startSquare][targetSquare];
    }

    public static int NumKingMovesToReachSquare(int startSquare, int targetSquare) {
        return kingDistance[startSquare][targetSquare];
    }

    // Initialize lookup data
    public PrecomputedMoveData() {
        pawnAttacksWhite = new int[64][];
        pawnAttacksBlack = new int[64][];
        numSquaresToEdge = new int[64][];
        knightMoves = new byte[64][];
        kingMoves = new byte[64][];

        rookMoves = new long[64];
        bishopMoves = new long[64];
        queenMoves = new long[64];

        // Calculate knight jumps and available squares for each square on the board.
        // See comments by variable definitions for more info.
        int[] allKnightJumps = {15, 17, -17, -15, 10, -6, 6, -10};
        knightAttackBitboards = new long[64];
        kingAttackBitboards = new long[64];
        pawnAttackBitboards = new long[64][];

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {

            int y = squareIndex / 8;
            int x = squareIndex - y * 8;

            int north = 7 - y;
            int south = y;
            int west = x;
            int east = 7 - x;
            numSquaresToEdge[squareIndex] = new int[8];
            numSquaresToEdge[squareIndex][0] = north;
            numSquaresToEdge[squareIndex][1] = south;
            numSquaresToEdge[squareIndex][2] = west;
            numSquaresToEdge[squareIndex][3] = east;
            numSquaresToEdge[squareIndex][4] = Integer.min(north, west);
            numSquaresToEdge[squareIndex][5] = Integer.min(south, east);
            numSquaresToEdge[squareIndex][6] = Integer.min(north, east);
            numSquaresToEdge[squareIndex][7] = Integer.min(south, west);

            // Calculate all squares knight can jump to from current square
            ArrayList<Byte> legalKnightJumps = new ArrayList<>();
            long knightBitboard = 0;
            for (int knightJumpDelta : allKnightJumps) {
                int knightJumpSquare = squareIndex + knightJumpDelta;
                if (knightJumpSquare >= 0 && knightJumpSquare < 64) {
                    int knightSquareY = knightJumpSquare / 8;
                    int knightSquareX = knightJumpSquare - knightSquareY * 8;
                    // Ensure knight has moved max of 2 squares on x/y axis (to reject indices that have wrapped around side of board)
                    int maxCoordMoveDst = Integer.max(Math.abs(x - knightSquareX), Math.abs(y - knightSquareY));
                    if (maxCoordMoveDst == 2) {
                        legalKnightJumps.add((byte) knightJumpSquare);
                        knightBitboard |= 1L << knightJumpSquare;
                    }
                }
            }
            knightMoves[squareIndex] = toByteArray(legalKnightJumps);
            knightAttackBitboards[squareIndex] = knightBitboard;

            // Calculate all squares king can move to from current square (not including castling)
            ArrayList<Byte> legalKingMoves = new ArrayList<>();
            for (int kingMoveDelta : directionOffsets) {
                int kingMoveSquare = squareIndex + kingMoveDelta;
                if (kingMoveSquare >= 0 && kingMoveSquare < 64) {
                    int kingSquareY = kingMoveSquare / 8;
                    int kingSquareX = kingMoveSquare - kingSquareY * 8;
                    // Ensure king has moved max of 1 square on x/y axis (to reject indices that have wrapped around side of board)
                    int maxCoordMoveDst = Integer.max(Math.abs(x - kingSquareX), Math.abs(y - kingSquareY));
                    if (maxCoordMoveDst == 1) {
                        legalKingMoves.add((byte) kingMoveSquare);
                        kingAttackBitboards[squareIndex] |= 1L << kingMoveSquare;
                    }
                }
            }
            kingMoves[squareIndex] = toByteArray(legalKingMoves);

            // Calculate legal pawn captures for white and black
            ArrayList<Integer> pawnCapturesWhite = new ArrayList<>();
            ArrayList<Integer> pawnCapturesBlack = new ArrayList<>();
            pawnAttackBitboards[squareIndex] = new long[2];
            if (x > 0) {
                if (y < 7) {
                    pawnCapturesWhite.add(squareIndex + 7);
                    pawnAttackBitboards[squareIndex][Board.WHITE_INDEX] |= 1L << (squareIndex + 7);
                }
                if (y > 0) {
                    pawnCapturesBlack.add(squareIndex - 9);
                    pawnAttackBitboards[squareIndex][Board.BLACK_INDEX] |= 1L << (squareIndex - 9);
                }
            }
            if (x < 7) {
                if (y < 7) {
                    pawnCapturesWhite.add(squareIndex + 9);
                    pawnAttackBitboards[squareIndex][Board.WHITE_INDEX] |= 1L << (squareIndex + 9);
                }
                if (y > 0) {
                    pawnCapturesBlack.add(squareIndex - 7);
                    pawnAttackBitboards[squareIndex][Board.BLACK_INDEX] |= 1L << (squareIndex - 7);
                }
            }
            pawnAttacksWhite[squareIndex] = toIntArray(pawnCapturesWhite);
            pawnAttacksBlack[squareIndex] = toIntArray(pawnCapturesBlack);

            // Rook moves
            for (int directionIndex = 0; directionIndex < 4; directionIndex++) {
                int currentDirOffset = directionOffsets[directionIndex];
                for (int n = 0; n < numSquaresToEdge[squareIndex][directionIndex]; n++) {
                    int targetSquare = squareIndex + currentDirOffset * (n + 1);
                    rookMoves[squareIndex] |= 1L << targetSquare;
                }
            }
            // Bishop moves
            for (int directionIndex = 4; directionIndex < 8; directionIndex++) {
                int currentDirOffset = directionOffsets[directionIndex];
                for (int n = 0; n < numSquaresToEdge[squareIndex][directionIndex]; n++) {
                    int targetSquare = squareIndex + currentDirOffset * (n + 1);
                    bishopMoves[squareIndex] |= 1L << targetSquare;
                }
            }
            queenMoves[squareIndex] = rookMoves[squareIndex] | bishopMoves[squareIndex];
        }

        directionLookup = new int[127];
        for (int i = 0; i < 127; i++) {
            int offset = i - 63;
            int absOffset = Math.abs(offset);
            int absDir = 1;
            if (absOffset % 9 == 0) {
                absDir = 9;
            } else if (absOffset % 8 == 0) {
                absDir = 8;
            } else if (absOffset % 7 == 0) {
                absDir = 7;
            }

            directionLookup[i] = absDir * Integer.signum(offset);
        }

        // Distance lookup
        orthogonalDistance = new int[64][64];
        kingDistance = new int[64][64];
        centreManhattanDistance = new int[64];
        for (int squareA = 0; squareA < 64; squareA++) {
            Coord coordA = BoardRepresentation.CoordFromIndex(squareA);
            int fileDstFromCentre = Integer.max(3 - coordA.fileIndex, coordA.fileIndex - 4);
            int rankDstFromCentre = Integer.max(3 - coordA.rankIndex, coordA.rankIndex - 4);
            centreManhattanDistance[squareA] = fileDstFromCentre + rankDstFromCentre;

            for (int squareB = 0; squareB < 64; squareB++) {

                Coord coordB = BoardRepresentation.CoordFromIndex(squareB);
                int rankDistance = Math.abs(coordA.rankIndex - coordB.rankIndex);
                int fileDistance = Math.abs(coordA.fileIndex - coordB.fileIndex);
                orthogonalDistance[squareA][squareB] = fileDistance + rankDistance;
                kingDistance[squareA][squareB] = Integer.max(fileDistance, rankDistance);
            }
        }
    }

    public byte[] toByteArray(ArrayList<Byte> O) {
        byte[] newArray = new byte[O.size()];
        for (int i = 0; i < O.size(); i++) {
            newArray[i] = O.get(i);
        }
        return newArray;
    }

    private int[] toIntArray(ArrayList<Integer> O) {
        int[] newArray = new int[O.size()];
        for (int i = 0; i < O.size(); i++) {
            newArray[i] = O.get(i);
        }
        return newArray;
    }
}
