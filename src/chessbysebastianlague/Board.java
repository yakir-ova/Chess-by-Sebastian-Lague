package chessbysebastianlague;


import java.util.Stack;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author yakir
 */
public class Board {

    public static final int WHITE_INDEX = 0;
    public static final int BLACK_INDEX = 1;

    // Stores piece code for each square on the board.
    // Piece code is defined as piecetype | color code
    public int[] Square;

    public boolean WhiteToMove;
    public int ColorToMove;
    public int OpponentColor;
    public int ColorToMoveIndex;

    // Bits 0-3 store white and black kingside/queenside castling legality
    // Bits 4-7 store file of ep square (starting at 1, so 0 = no ep square)
    // bits 8-13 captured piece
    // Bits 14- ... fifty mover counter
    Stack gameStateHistory;
    public int currentGameState;

    public int plyCount; // Total plies played in game
    public int fiftyMoveCounter; // Num ply since last pawn move or capture

    public long ZobristKey;
    // List of zobrist keys
    public Stack RepetitionPositionHistory;

    public int[] KingSquare; // index of square of white and black king

    public PieceList[] rooks;
    public PieceList[] bishops;
    public PieceList[] queens;
    public PieceList[] knights;
    public PieceList[] pawns;

    PieceList[] allPieceLists;

    final int whiteCastleKingsideMask = 0b1111111111111110;
    final int whiteCastleQueensideMask = 0b1111111111111101;
    final int blackCastleKingsideMask = 0b1111111111111011;
    final int blackCastleQueensideMask = 0b1111111111110111;

    final int whiteCastleMask = whiteCastleKingsideMask & whiteCastleQueensideMask;
    final int blackCastleMask = blackCastleKingsideMask & blackCastleQueensideMask;

    PieceList GetPieceList(int pieceType, int colorIndex) {
        return allPieceLists[colorIndex * 8 + pieceType];
    }

    // Make a move on the board
    // The inSearch parameter controls whether this move should be recorded in the game history (for detecting three-fold repetition)
    public void MakeMove(Move move, boolean inSearch) {
        int oldEnPassantFile = (currentGameState >> 4) & 15;
        int originalCastleState = currentGameState & 15;
        int newCastleState = originalCastleState;
        currentGameState = 0;

        int opponentColorIndex = 1 - ColorToMoveIndex;
        int moveFrom = move.StartSquare;
        int moveTo = move.TargetSquare;

        int capturedPieceType = Piece.PieceType(Square[moveTo]);
        int movePiece = Square[moveFrom];
        int movePieceType = Piece.PieceType(movePiece);

        int moveFlag = move.MoveFlag;
        boolean isPromotion = move.Promotion;
        boolean isEnPassant = moveFlag == Move.Flag.EN_PASSANT_CAPTURE;

        // Handle captures
        currentGameState |= (short) (capturedPieceType << 8);
        if (capturedPieceType != 0 && !isEnPassant) {
            ZobristKey ^= Zobrist.piecesArray[capturedPieceType][opponentColorIndex][moveTo];
            GetPieceList(capturedPieceType, opponentColorIndex).RemovePieceAtSquare(moveTo);
        }

        // Move pieces in piece lists
        if (movePieceType == Piece.KING) {
            KingSquare[ColorToMoveIndex] = moveTo;
            newCastleState &= (WhiteToMove) ? whiteCastleMask : blackCastleMask;
        } else {
            GetPieceList(movePieceType, ColorToMoveIndex).MovePiece(moveFrom, moveTo);
        }

        int pieceOnTargetSquare = movePiece;

        // Handle promotion
        if (isPromotion) {
            int promoteType = 0;
            switch (moveFlag) {
                case Move.Flag.PROMOTE_TO_QUEEN -> {
                    promoteType = Piece.QUEEN;
                    queens[ColorToMoveIndex].AddPieceAtSquare(moveTo);
                }
                case Move.Flag.PROMOTE_TO_ROOK -> {
                    promoteType = Piece.ROOK;
                    rooks[ColorToMoveIndex].AddPieceAtSquare(moveTo);
                }
                case Move.Flag.PROMOTE_TO_BISHOP -> {
                    promoteType = Piece.BISHOP;
                    bishops[ColorToMoveIndex].AddPieceAtSquare(moveTo);
                }
                case Move.Flag.PROMOTE_TO_KNIGHT -> {
                    promoteType = Piece.KNIGHT;
                    knights[ColorToMoveIndex].AddPieceAtSquare(moveTo);
                }
            }

            pieceOnTargetSquare = promoteType | ColorToMove;
            pawns[ColorToMoveIndex].RemovePieceAtSquare(moveTo);
        } else {
            // Handle other special moves (en-passant, and castling)
            switch (moveFlag) {
                case Move.Flag.EN_PASSANT_CAPTURE -> {
                    int epPawnSquare = moveTo + ((ColorToMove == Piece.WHITE) ? -8 : 8);
                    currentGameState |= (short) (Square[epPawnSquare] << 8); // add pawn as capture type
                    Square[epPawnSquare] = 0; // clear ep capture square
                    pawns[opponentColorIndex].RemovePieceAtSquare(epPawnSquare);
                    ZobristKey ^= Zobrist.piecesArray[Piece.PAWN][opponentColorIndex][epPawnSquare];
                }
                case Move.Flag.CASTLING -> {
                    boolean kingside = moveTo == BoardRepresentation.G1 || moveTo == BoardRepresentation.G8;
                    int castlingRookFromIndex = (kingside) ? moveTo + 1 : moveTo - 2;
                    int castlingRookToIndex = (kingside) ? moveTo - 1 : moveTo + 1;
                    // make move on the board
                    Square[castlingRookFromIndex] = Piece.NONE;
                    Square[castlingRookToIndex] = Piece.ROOK | ColorToMove;
                    // make move in the Bitboard
                    rooks[ColorToMoveIndex].MovePiece(castlingRookFromIndex, castlingRookToIndex);
                    // update the zobrist key
                    ZobristKey ^= Zobrist.piecesArray[Piece.ROOK][ColorToMoveIndex][castlingRookFromIndex];
                    ZobristKey ^= Zobrist.piecesArray[Piece.ROOK][ColorToMoveIndex][castlingRookToIndex];
                }
            }
        }

        // Update the board representation:
        Square[moveTo] = pieceOnTargetSquare;
        Square[moveFrom] = 0;

        // PAWN has moves two forwards, mark file with en-passant flag
        if (moveFlag == Move.Flag.PAWN_TWO_FORWARD) {
            int file = BoardRepresentation.FileIndex(moveFrom) + 1;
            currentGameState |= (short) (file << 4);
            ZobristKey ^= Zobrist.enPassantFile[file];
        }

        // Piece moving to/from rook square removes castling right for that side
        if (originalCastleState != 0) {
            if (moveTo == BoardRepresentation.H1 || moveFrom == BoardRepresentation.H1) {
                newCastleState &= whiteCastleKingsideMask;
            } else if (moveTo == BoardRepresentation.A1 || moveFrom == BoardRepresentation.A1) {
                newCastleState &= whiteCastleQueensideMask;
            }
            if (moveTo == BoardRepresentation.H8 || moveFrom == BoardRepresentation.H8) {
                newCastleState &= blackCastleKingsideMask;
            } else if (moveTo == BoardRepresentation.A8 || moveFrom == BoardRepresentation.A8) {
                newCastleState &= blackCastleQueensideMask;
            }
        }

        // Update zobrist key with new piece position and side to move
        ZobristKey ^= Zobrist.sideToMove;
        ZobristKey ^= Zobrist.piecesArray[movePieceType][ColorToMoveIndex][moveFrom];
        ZobristKey ^= Zobrist.piecesArray[Piece.PieceType(pieceOnTargetSquare)][ColorToMoveIndex][moveTo];

        if (oldEnPassantFile != 0) {
            ZobristKey ^= Zobrist.enPassantFile[oldEnPassantFile];
        }

        if (newCastleState != originalCastleState) {
            ZobristKey ^= Zobrist.castlingRights[originalCastleState]; // remove old castling rights state
            ZobristKey ^= Zobrist.castlingRights[newCastleState]; // add new castling rights state
        }

        currentGameState |= newCastleState;
        currentGameState |= (int) fiftyMoveCounter << 14;
        gameStateHistory.push(currentGameState);

        // Change side to move
        WhiteToMove = !WhiteToMove;
        ColorToMove = (WhiteToMove) ? Piece.WHITE : Piece.BLACK;
        OpponentColor = (WhiteToMove) ? Piece.BLACK : Piece.WHITE;
        ColorToMoveIndex = 1 - ColorToMoveIndex;
        plyCount++;
        fiftyMoveCounter++;

        if (!inSearch) {
            if (movePieceType == Piece.PAWN || capturedPieceType != Piece.NONE) {
                RepetitionPositionHistory.clear();
                fiftyMoveCounter = 0;
            } else {
                RepetitionPositionHistory.push(ZobristKey);
            }
        }

    }

    // Undo a move previously made on the board
    public void UnmakeMove(Move move, boolean inSearch) {

        int opponentColorIndex = ColorToMoveIndex;
        boolean undoingWhiteMove = OpponentColor == Piece.WHITE;
        ColorToMove = OpponentColor; // side who made the move we are undoing 
        OpponentColor = (undoingWhiteMove) ? Piece.BLACK : Piece.WHITE;
        ColorToMoveIndex = 1 - ColorToMoveIndex;
        WhiteToMove = !WhiteToMove;

        int originalCastleState = currentGameState & 0b1111;

        int capturedPieceType = ((int) currentGameState >> 8) & 63;
        int capturedPiece = (capturedPieceType == 0) ? 0 : capturedPieceType | OpponentColor;

        int movedFrom = move.StartSquare;
        int movedTo = move.TargetSquare;
        int moveFlags = move.MoveFlag;
        boolean isEnPassant = moveFlags == Move.Flag.EN_PASSANT_CAPTURE;
        boolean isPromotion = move.Promotion;

        int toSquarePieceType = Piece.PieceType(Square[movedTo]);
        int movedPieceType = (isPromotion) ? Piece.PAWN : toSquarePieceType;

        // Update zobrist key with new piece position and side to move
        ZobristKey ^= Zobrist.sideToMove;
        ZobristKey ^= Zobrist.piecesArray[movedPieceType][ColorToMoveIndex][movedFrom]; // add piece back to square it moved from
        ZobristKey ^= Zobrist.piecesArray[toSquarePieceType][ColorToMoveIndex][movedTo]; // remove piece from square it moved to

        int oldEnPassantFile = (currentGameState >> 4) & 15;
        if (oldEnPassantFile != 0) {
            ZobristKey ^= Zobrist.enPassantFile[oldEnPassantFile];
        }

        // ignore ep captures, handled later
        if (capturedPieceType != 0 && !isEnPassant) {
            ZobristKey ^= Zobrist.piecesArray[capturedPieceType][opponentColorIndex][movedTo];
            GetPieceList(capturedPieceType, opponentColorIndex).AddPieceAtSquare(movedTo);
        }

        // Update king index
        if (movedPieceType == Piece.KING) {
            KingSquare[ColorToMoveIndex] = movedFrom;
        } else if (!isPromotion) {
            GetPieceList(movedPieceType, ColorToMoveIndex).MovePiece(movedTo, movedFrom);
        }

        // put back moved piece
        Square[movedFrom] = movedPieceType | ColorToMove; // note that if move was a pawn promotion, this will put the promoted piece back instead of the pawn. Handled in special move switch
        Square[movedTo] = capturedPiece; // will be 0 if no piece was captured

        if (isPromotion) {
            pawns[ColorToMoveIndex].AddPieceAtSquare(movedFrom);
            switch (moveFlags) {
                case Move.Flag.PROMOTE_TO_QUEEN -> queens[ColorToMoveIndex].RemovePieceAtSquare(movedTo);
                case Move.Flag.PROMOTE_TO_KNIGHT -> knights[ColorToMoveIndex].RemovePieceAtSquare(movedTo);
                case Move.Flag.PROMOTE_TO_ROOK -> rooks[ColorToMoveIndex].RemovePieceAtSquare(movedTo);
                case Move.Flag.PROMOTE_TO_BISHOP -> bishops[ColorToMoveIndex].RemovePieceAtSquare(movedTo);
            }
        } else if (isEnPassant) { // ep cature: put captured pawn back on right square
            int epIndex = movedTo + ((ColorToMove == Piece.WHITE) ? -8 : 8);
            Square[movedTo] = 0;
            Square[epIndex] = (int) capturedPiece;
            pawns[opponentColorIndex].AddPieceAtSquare(epIndex);
            ZobristKey ^= Zobrist.piecesArray[Piece.PAWN][opponentColorIndex][epIndex];
        } else if (moveFlags == Move.Flag.CASTLING) { // castles: move rook back to starting square

            boolean kingside = movedTo == 6 || movedTo == 62;
            int castlingRookFromIndex = (kingside) ? movedTo + 1 : movedTo - 2;
            int castlingRookToIndex = (kingside) ? movedTo - 1 : movedTo + 1;
            
            Square[castlingRookToIndex] = 0;
            Square[castlingRookFromIndex] = Piece.ROOK | ColorToMove;

            rooks[ColorToMoveIndex].MovePiece(castlingRookToIndex, castlingRookFromIndex);
            ZobristKey ^= Zobrist.piecesArray[Piece.ROOK][ColorToMoveIndex][castlingRookFromIndex];
            ZobristKey ^= Zobrist.piecesArray[Piece.ROOK][ColorToMoveIndex][castlingRookToIndex];

        }

        gameStateHistory.pop(); // removes current state from history
        currentGameState = (int) gameStateHistory.peek(); // sets current state to previous state in history

        fiftyMoveCounter = (int) (currentGameState & 429495091) >> 14;
        int newEnPassantFile = (int) (currentGameState >> 4) & 15;
        if (newEnPassantFile != 0) {
            ZobristKey ^= Zobrist.enPassantFile[newEnPassantFile];
        }

        int newCastleState = currentGameState & 0b1111;
        if (newCastleState != originalCastleState) {
            ZobristKey ^= Zobrist.castlingRights[originalCastleState]; // remove old castling rights state
            ZobristKey ^= Zobrist.castlingRights[newCastleState]; // add new castling rights state
        }

        plyCount--;

        if (!inSearch && !RepetitionPositionHistory.isEmpty()) {
            RepetitionPositionHistory.pop();
        }

    }

    // Load the starting position
    public void LoadStartPosition() {
        LoadPosition(FenUtility.startFen);
    }

    // Load custom position from fen string
    public void LoadPosition(String fen) {
        Initialize();
        FenUtility.LoadedPositionInfo loadedPosition = FenUtility.PositionFromFen(fen);

        // Load pieces into board array and piece lists
        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            int piece = loadedPosition.squares[squareIndex];
            Square[squareIndex] = piece;

            if (piece != Piece.NONE) {
                int pieceType = Piece.PieceType(piece);
                int pieceColourIndex = (Piece.IsColor(piece, Piece.WHITE)) ? WHITE_INDEX : BLACK_INDEX;
                if (Piece.IsSlidingPiece(piece)) {
                    switch (pieceType) {
                        case Piece.QUEEN -> queens[pieceColourIndex].AddPieceAtSquare(squareIndex);
                        case Piece.ROOK -> rooks[pieceColourIndex].AddPieceAtSquare(squareIndex);
                        case Piece.BISHOP -> bishops[pieceColourIndex].AddPieceAtSquare(squareIndex);
                        default -> {
                        }
                    }
                } else if (pieceType == Piece.KNIGHT) {
                    knights[pieceColourIndex].AddPieceAtSquare(squareIndex);
                } else if (pieceType == Piece.PAWN) {
                    pawns[pieceColourIndex].AddPieceAtSquare(squareIndex);
                } else if (pieceType == Piece.KING) {
                    KingSquare[pieceColourIndex] = squareIndex;
                }
            }
        }

        // Side to move
        WhiteToMove = loadedPosition.whiteToMove;
        ColorToMove = (WhiteToMove) ? Piece.WHITE : Piece.BLACK;
        OpponentColor = (WhiteToMove) ? Piece.BLACK : Piece.WHITE;
        ColorToMoveIndex = (WhiteToMove) ? 0 : 1;

        // Create gamestate
        int whiteCastle = ((loadedPosition.whiteCastleKingside) ? 1 : 0) | ((loadedPosition.whiteCastleQueenside) ? 1 << 1 : 0);
        int blackCastle = ((loadedPosition.blackCastleKingside) ? 1 << 2 : 0) | ((loadedPosition.blackCastleQueenside) ? 1 << 3 : 0);
        int epState = loadedPosition.epFile << 4;
        short initialGameState = (short) (whiteCastle | blackCastle | epState);
        gameStateHistory.push(initialGameState);
        currentGameState = initialGameState;
        plyCount = loadedPosition.plyCount;

        // Initialize zobrist key
        ZobristKey = Zobrist.CalculateZobristKey(this);
    }

    void Initialize() {
        Square = new int[64];
        KingSquare = new int[2];

        gameStateHistory = new Stack<>();
        ZobristKey = 0;
        RepetitionPositionHistory = new Stack<>();
        plyCount = 0;
        fiftyMoveCounter = 0;

        knights = new PieceList[]{new PieceList(10), new PieceList(10)};
        pawns = new PieceList[]{new PieceList(8), new PieceList(8)};
        rooks = new PieceList[]{new PieceList(10), new PieceList(10)};
        bishops = new PieceList[]{new PieceList(10), new PieceList(10)};
        queens = new PieceList[]{new PieceList(9), new PieceList(9)};
        PieceList emptyList = new PieceList(0);
        allPieceLists = new PieceList[]{
            emptyList,
            emptyList,
            pawns[WHITE_INDEX],
            knights[WHITE_INDEX],
            emptyList,
            bishops[WHITE_INDEX],
            rooks[WHITE_INDEX],
            queens[WHITE_INDEX],
            emptyList,
            emptyList,
            pawns[BLACK_INDEX],
            knights[BLACK_INDEX],
            emptyList,
            bishops[BLACK_INDEX],
            rooks[BLACK_INDEX],
            queens[BLACK_INDEX],};
    }
}
