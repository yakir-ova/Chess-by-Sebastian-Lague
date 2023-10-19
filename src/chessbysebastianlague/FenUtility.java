package chessbysebastianlague;


import java.util.HashMap;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author yakir
 */
class FenUtility {

    static String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    // a Hash Map for the pieces Types
    static HashMap<Character, Integer> pieceTypeFromSymbol = new HashMap<>();

    static LoadedPositionInfo PositionFromFen(String fen) {
        LoadedPositionInfo loadedPositionInfo = new LoadedPositionInfo();

        // KING = 1
        pieceTypeFromSymbol.put('k', 1);
        // QUEEN = 2
        pieceTypeFromSymbol.put('q', 2);
        // PAWN = 3
        pieceTypeFromSymbol.put('p', 3);
        // KNIGHT = 4
        pieceTypeFromSymbol.put('n', 4);
        // BISHOP = 5
        pieceTypeFromSymbol.put('b', 5);
        // ROOK = 6
        pieceTypeFromSymbol.put('r', 6);

        String[] sections = fen.split(" ");

        int file = 0;
        int rank = 7;

        for (Character symbol : sections[0].toCharArray()) {
            if (symbol == '/') {
                file = 0;
                rank--;
            } else {
                if (Character.isDigit(symbol)) {
                    file += (int) Character.getNumericValue(symbol);
                } else {
                    int pieceColor = (Character.isUpperCase(symbol)) ? Piece.WHITE : Piece.BLACK;
                    int pieceType = pieceTypeFromSymbol.get(Character.toLowerCase(symbol));
                    loadedPositionInfo.squares[rank * 8 + file] = pieceType | pieceColor;
                    file++;
                }
            }
        }

        loadedPositionInfo.whiteToMove = ("w".equals(sections[1]));

        String castlingRights = (sections.length > 2) ? sections[2] : "KQkq";
        loadedPositionInfo.whiteCastleKingside = castlingRights.contains("K");
        loadedPositionInfo.whiteCastleQueenside = castlingRights.contains("Q");
        loadedPositionInfo.blackCastleKingside = castlingRights.contains("k");
        loadedPositionInfo.blackCastleQueenside = castlingRights.contains("q");

        if (sections.length > 3) {
            String enPassantFileName = sections[3];
            if (BoardRepresentation.FILE_NAMES.contains(enPassantFileName)) {
                loadedPositionInfo.epFile = BoardRepresentation.FILE_NAMES.indexOf(enPassantFileName) + 1;
            }
        }

        // Half-move clock
//        if (sections.length > 4) {
//            Integer.parseInt(sections[4], loadedPositionInfo.plyCount);
//        }
        return loadedPositionInfo;
    }

    // Get the fen string of the current position
    public static String CurrentFen(Board board) {
        String fen = "";
        for (int rank = 7; rank >= 0; rank--) {
            int numEmptyFiles = 0;
            for (int file = 0; file < 8; file++) {
                int i = rank * 8 + file;
                int piece = board.Square[i];
                if (piece != 0) {
                    if (numEmptyFiles != 0) {
                        fen += numEmptyFiles;
                        numEmptyFiles = 0;
                    }
                    boolean isBlack = Piece.IsColor(piece, Piece.BLACK);
                    int pieceType = Piece.PieceType(piece);
                    char pieceChar = ' ';
                    switch (pieceType) {
                        case Piece.ROOK -> pieceChar = 'R';
                        case Piece.KNIGHT -> pieceChar = 'N';
                        case Piece.BISHOP -> pieceChar = 'B';
                        case Piece.QUEEN -> pieceChar = 'Q';
                        case Piece.KING -> pieceChar = 'K';
                        case Piece.PAWN -> pieceChar = 'P';
                    }
                    fen += (isBlack) ? Character.toString(Character.toLowerCase(pieceChar)) : Character.toString(pieceChar);
                } else {
                    numEmptyFiles++;
                }

            }
            if (numEmptyFiles != 0) {
                fen += numEmptyFiles;
            }
            if (rank != 0) {
                fen += '/';
            }
        }

        // Side to move
        fen += ' ';
        fen += (board.WhiteToMove) ? 'w' : 'b';

        // Castling
        boolean whiteKingside = (board.currentGameState & 1) == 1;
        boolean whiteQueenside = (board.currentGameState >> 1 & 1) == 1;
        boolean blackKingside = (board.currentGameState >> 2 & 1) == 1;
        boolean blackQueenside = (board.currentGameState >> 3 & 1) == 1;
        fen += ' ';
        fen += (whiteKingside) ? "K" : "";
        fen += (whiteQueenside) ? "Q" : "";
        fen += (blackKingside) ? "k" : "";
        fen += (blackQueenside) ? "q" : "";
        fen += ((board.currentGameState & 15) == 0) ? "-" : "";

        // En-passant
        fen += ' ';
        int epFile = (int) (board.currentGameState >> 4) & 15;
        if (epFile == 0) {
            fen += '-';
        } else {
            char fileName = BoardRepresentation.FILE_NAMES.charAt(epFile - 1);
            int epRank = (board.WhiteToMove) ? 6 : 3;
            fen += fileName + epRank;
        }

        // 50 move counter
        fen += ' ';
        fen += board.fiftyMoveCounter;

        // Full-move count (should be one at start, and increase after each move by black)
        fen += ' ';
        fen += (board.plyCount / 2) + 1;

        return fen;
    }

    public static class LoadedPositionInfo {

        public int[] squares;
        public boolean whiteCastleKingside;
        public boolean whiteCastleQueenside;
        public boolean blackCastleKingside;
        public boolean blackCastleQueenside;
        public int epFile;
        public boolean whiteToMove;
        public int plyCount;

        public LoadedPositionInfo() {
            squares = new int[64];
        }
    }
}
