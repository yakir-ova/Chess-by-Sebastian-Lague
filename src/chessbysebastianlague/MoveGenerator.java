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
public class MoveGenerator {

    public enum PromotionMode {
        All, QueenOnly, QueenAndKnight
    }

    public PromotionMode promotionsToGenerate = PromotionMode.All;

    // ---- Instance variables ----
    ArrayList<Move> moves;
    boolean isWhiteToMove;
    int friendlyColour;
    int opponentColour;
    int friendlyKingSquare;
    int friendlyColourIndex;
    int opponentColourIndex;

    boolean inCheck;
    boolean inDoubleCheck;
    boolean pinsExistInPosition;
    long checkRayBitmask;
    long pinRayBitmask;
    long opponentKnightAttacks;
    long opponentAttackMapNoPawns;
    public long opponentAttackMap;
    public long opponentPawnAttackMap;
    long opponentSlidingAttackMap;

    boolean genQuiets;
    Board board;

    // Generates list of legal moves in current position.
    // Quiet moves (non captures) can optionally be excluded. This is used in quiescence search.
    public ArrayList<Move> GenerateMoves(Board board, boolean includeQuietMoves) {
        // applaying the given board with its setting the class's global board variable
        this.board = board;
        // deciding if we want to include quiet moves
        genQuiets = includeQuietMoves;
        // see function description
        Init();

        // see function description
        CalculateAttackData();
        // after we calculated the attack data we can generate the king moves
        // see function description
        GenerateKingMoves();

        // since the calculateAttackData function changed the inDoubleCheck variable we can look if we are in fact in a double check
        // Only king moves are valid in a double check position, so can return early.
        if (inDoubleCheck) {
            return moves;
        }
        
        // reaching here means we are not in a double check so just generate the other possible moves
        // see function description
        GenerateSlidingMoves();
        // see function description
        GenerateKnightMoves();
        // see function description
        GeneratePawnMoves();

        // after calculating all the moves return the list
        return moves;
    }

    // Note, this will only return correct value after GenerateMoves() has been called in the current position
    public boolean InCheck() {
        return inCheck;
    }

    void Init() {
        // giving values to all the undefined variables at the start of this class.
        moves = new ArrayList<>(64);
        inCheck = false;
        inDoubleCheck = false;
        pinsExistInPosition = false;
        checkRayBitmask = 0;
        pinRayBitmask = 0;

        isWhiteToMove = board.ColorToMove == Piece.WHITE;
        friendlyColour = board.ColorToMove;
        opponentColour = board.OpponentColor;
        friendlyKingSquare = board.KingSquare[board.ColorToMoveIndex];
        friendlyColourIndex = (board.WhiteToMove) ? Board.WHITE_INDEX : Board.BLACK_INDEX;
        opponentColourIndex = 1 - friendlyColourIndex;
    }

    void GenerateKingMoves() {
        // a bit of background. We have another class called PrecomputedMoveData where we calculate and store all the king knight and pawn possible moves
        // on any position on the board and use those to save time
        
        // since we have every possible move of the king on any square. We want to loop over every square that our king can. To explain this better
        // lets give the variables values. imagine that friendlyKingSquare is 0, that means that our king can go to the three adjacent squares,
        // assuming they are not being threated by enemy piece. So, we'll take the length of that array which if the calculation on the other class
        // is correct should also be 3. Now, we'll go through those places and give the target square that position, and take that number to get the
        // piece that is on the target square.
        for (int i = 0; i < PrecomputedMoveData.kingMoves[friendlyKingSquare].length; i++) {
            int targetSquare = PrecomputedMoveData.kingMoves[friendlyKingSquare][i];
            int pieceOnTargetSquare = board.Square[targetSquare];

            // Skip squares occupied by friendly pieces
            if (Piece.IsColor(pieceOnTargetSquare, friendlyColour)) {
                continue;
            }

            boolean isCapture = Piece.IsColor(pieceOnTargetSquare, opponentColour);
            if (!isCapture) {
                // King can't move to square marked as under enemy control, unless he is capturing that piece
                // Also skip if not generating quiet moves
                
                // more on SquareIsInCheckRay function on function description
                if (!genQuiets || SquareIsInCheckRay(targetSquare)) {
                    continue;
                }
            }

            // Safe for king to move to this square
            if (!SquareIsAttacked(targetSquare)) {
                moves.add(new Move(friendlyKingSquare, targetSquare));

                // Castling:
                // if the king is in check we cant castle as well as if the square that the king wants castle to is occupied by enemy piece
                if (!inCheck && !isCapture) {
                    // Castle kingside
                    // we are checking if the square that we want to castle to is the legal square and if our king has any limitations to go there
                    // more on hasKingsideCastleRight function on function description
                    if ((targetSquare == BoardRepresentation.F1 || targetSquare == BoardRepresentation.F8) && hasKingsideCastleRight()) {
                        // saving the square that we want to castle to which is the targetSquare + 1
                        int castleKingsideSquare = targetSquare + 1;
                        // checking that the spot is empty
                        if (board.Square[castleKingsideSquare] == Piece.NONE) {
                            // more on function description
                            if (!SquareIsAttacked(castleKingsideSquare)) {
                                moves.add(new Move(friendlyKingSquare, castleKingsideSquare, Move.Flag.CASTLING));
                            }
                        }
                    } // Castle queenside
                    // the same rules for the king side apply on the queen side with their respected values
                    else if ((targetSquare == BoardRepresentation.D1 || targetSquare == BoardRepresentation.D8) && HasQueensideCastleRight()) {
                        int castleQueensideSquare = targetSquare - 1;
                        if (board.Square[castleQueensideSquare] == Piece.NONE && board.Square[castleQueensideSquare - 1] == Piece.NONE) {
                            if (!SquareIsAttacked(castleQueensideSquare)) {
                                moves.add(new Move(friendlyKingSquare, castleQueensideSquare, Move.Flag.CASTLING));
                            }
                        }
                    }
                }
            }
        }
    }

    void GenerateSlidingMoves() {
        PieceList rooks = board.rooks[friendlyColourIndex];
        for (int i = 0; i < rooks.Count; i++) {
            GenerateSlidingPieceMoves(rooks.getStartSquare(i), 0, 4);
        }

        PieceList bishops = board.bishops[friendlyColourIndex];
        for (int i = 0; i < bishops.Count; i++) {
            GenerateSlidingPieceMoves(bishops.getStartSquare(i), 4, 8);
        }

        PieceList queens = board.queens[friendlyColourIndex];
        for (int i = 0; i < queens.Count; i++) {
            GenerateSlidingPieceMoves(queens.getStartSquare(i), 0, 8);
        }
    }

    void GenerateSlidingPieceMoves(int startSquare, int startDirIndex, int endDirIndex) {
        boolean isPinned = IsPinned(startSquare);

        // If this piece is pinned, and the king is in check, this piece cannot move
        if (inCheck && isPinned) {
            return;
        }

        for (int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            int currentDirOffset = PrecomputedMoveData.directionOffsets[directionIndex];

            // If pinned, this piece can only move along the ray towards/away from the friendly king, so skip other directions
            if (isPinned && !IsMovingAlongRay(currentDirOffset, friendlyKingSquare, startSquare)) {
                continue;
            }

            for (int n = 0; n < PrecomputedMoveData.numSquaresToEdge[startSquare][directionIndex]; n++) {
                int targetSquare = startSquare + currentDirOffset * (n + 1);
                int targetSquarePiece = board.Square[targetSquare];

                // Blocked by friendly piece, so stop looking in this direction
                if (Piece.IsColor(targetSquarePiece, friendlyColour)) {
                    break;
                }
                boolean isCapture = targetSquarePiece != Piece.NONE;

                boolean movePreventsCheck = SquareIsInCheckRay(targetSquare);
                if (movePreventsCheck || !inCheck) {
                    if (genQuiets || isCapture) {
                        moves.add(new Move(startSquare, targetSquare));
                    }
                }
                // If square not empty, can't move any further in this direction
                // Also, if this move blocked a check, further moves won't block the check
                if (isCapture || movePreventsCheck) {
                    break;
                }
            }
        }
    }

    void GenerateKnightMoves() {
        PieceList myKnights = board.knights[friendlyColourIndex];

        for (int i = 0; i < myKnights.Count; i++) {
            int startSquare = myKnights.getStartSquare(i);

            // Knight cannot move if it is pinned
            if (IsPinned(startSquare)) {
                continue;
            }

            for (int knightMoveIndex = 0; knightMoveIndex < PrecomputedMoveData.knightMoves[startSquare].length; knightMoveIndex++) {
                int targetSquare = PrecomputedMoveData.knightMoves[startSquare][knightMoveIndex];
                int targetSquarePiece = board.Square[targetSquare];
                boolean isCapture = Piece.IsColor(targetSquarePiece, opponentColour);
                if (genQuiets || isCapture) {
                    // Skip if square contains friendly piece, or if in check and knight is not interposing/capturing checking piece
                    if (Piece.IsColor(targetSquarePiece, friendlyColour) || (inCheck && !SquareIsInCheckRay(targetSquare))) {
                        continue;
                    }
                    moves.add(new Move(startSquare, targetSquare));
                }
            }
        }
    }

    void GeneratePawnMoves() {
        PieceList myPawns = board.pawns[friendlyColourIndex];
        int pawnOffset = (friendlyColour == Piece.WHITE) ? 8 : -8;
        int startRank = (board.WhiteToMove) ? 1 : 6;
        int finalRankBeforePromotion = (board.WhiteToMove) ? 6 : 1;

        int enPassantFile = ((int) (board.currentGameState >> 4) & 15) - 1;
        int enPassantSquare = -1;
        if (enPassantFile != -1) {
            enPassantSquare = 8 * ((board.WhiteToMove) ? 5 : 2) + enPassantFile;
        }

        for (int i = 0; i < myPawns.Count; i++) {
            int startSquare = myPawns.getStartSquare(i);
            int rank = BoardRepresentation.RankIndex(startSquare);
            boolean oneStepFromPromotion = rank == finalRankBeforePromotion;

            if (genQuiets) {

                int squareOneForward = startSquare + pawnOffset;

                // Square ahead of pawn is empty: forward moves
                if (board.Square[squareOneForward] == Piece.NONE) {
                    // Pawn not pinned, or is moving along line of pin
                    if (!IsPinned(startSquare) || IsMovingAlongRay(pawnOffset, startSquare, friendlyKingSquare)) {
                        // Not in check, or pawn is interposing checking piece
                        if (!inCheck || SquareIsInCheckRay(squareOneForward)) {
                            if (oneStepFromPromotion) {
                                MakePromotionMoves(startSquare, squareOneForward);
                            } else {
                                moves.add(new Move(startSquare, squareOneForward));
                            }
                        }

                        // Is on starting square (so can move two forward if not blocked)
                        if (rank == startRank) {
                            int squareTwoForward = squareOneForward + pawnOffset;
                            if (board.Square[squareTwoForward] == Piece.NONE) {
                                // Not in check, or pawn is interposing checking piece
                                if (!inCheck || SquareIsInCheckRay(squareTwoForward)) {
                                    moves.add(new Move(startSquare, squareTwoForward, Move.Flag.PAWN_TWO_FORWARD));
                                }
                            }
                        }
                    }
                }
            }

            // Pawn captures.
            for (int j = 0; j < 2; j++) {
                // Check if square exists diagonal to pawn
                if (PrecomputedMoveData.numSquaresToEdge[startSquare][PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][j]] > 0) {
                    // move in direction friendly pawns attack to get square from which enemy pawn would attack
                    int pawnCaptureDir = PrecomputedMoveData.directionOffsets[PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][j]];
                    int targetSquare = startSquare + pawnCaptureDir;
                    int targetPiece = board.Square[targetSquare];

                    // If piece is pinned, and the square it wants to move to is not on same line as the pin, then skip this direction
                    if (IsPinned(startSquare) && !IsMovingAlongRay(pawnCaptureDir, friendlyKingSquare, startSquare)) {
                        continue;
                    }

                    // Regular capture
                    if (Piece.IsColor(targetPiece, opponentColour)) {
                        // If in check, and piece is not capturing/interposing the checking piece, then skip to next square
                        if (inCheck && !SquareIsInCheckRay(targetSquare)) {
                            continue;
                        }
                        if (oneStepFromPromotion) {
                            MakePromotionMoves(startSquare, targetSquare);
                        } else {
                            moves.add(new Move(startSquare, targetSquare));
                        }
                    }

                    // Capture en-passant
                    if (targetSquare == enPassantSquare) {
                        int epCapturedPawnSquare = targetSquare + ((board.WhiteToMove) ? -8 : 8);
                        if (!InCheckAfterEnPassant(startSquare, targetSquare, epCapturedPawnSquare)) {
                            moves.add(new Move(startSquare, targetSquare, Move.Flag.EN_PASSANT_CAPTURE));
                        }
                    }
                }
            }
        }
    }

    void MakePromotionMoves(int fromSquare, int toSquare) {
        moves.add(new Move(fromSquare, toSquare, Move.Flag.PROMOTE_TO_QUEEN));
        if (promotionsToGenerate == PromotionMode.All) {
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PROMOTE_TO_KNIGHT));
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PROMOTE_TO_ROOK));
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PROMOTE_TO_BISHOP));
        } else if (promotionsToGenerate == PromotionMode.QueenAndKnight) {
            moves.add(new Move(fromSquare, toSquare, Move.Flag.PROMOTE_TO_KNIGHT));
        }
    }

    boolean IsMovingAlongRay(int rayDir, int startSquare, int targetSquare) {
        int moveDir = PrecomputedMoveData.directionLookup[targetSquare - startSquare + 63];
        return (rayDir == moveDir || -rayDir == moveDir);
    }

    boolean IsPinned(int square) {
        return pinsExistInPosition && ((pinRayBitmask >> square) & 1) != 0;
    }

    boolean SquareIsInCheckRay(int square) {
        return inCheck && ((checkRayBitmask >> square) & 1) != 0;
    }

    boolean hasKingsideCastleRight() {
        int mask = (board.WhiteToMove) ? 1 : 4;
        return (board.currentGameState & mask) != 0;
    }

    boolean HasQueensideCastleRight() {
        int mask = (board.WhiteToMove) ? 2 : 8;
        return (board.currentGameState & mask) != 0;
    }

    void GenSlidingAttackMap() {
        opponentSlidingAttackMap = 0;

        PieceList enemyRooks = board.rooks[opponentColourIndex];
        for (int i = 0; i < enemyRooks.Count; i++) {
            UpdateSlidingAttackPiece(enemyRooks.getStartSquare(i), 0, 4);
        }

        PieceList enemyQueens = board.queens[opponentColourIndex];
        for (int i = 0; i < enemyQueens.Count; i++) {
            UpdateSlidingAttackPiece(enemyQueens.getStartSquare(i), 0, 8);
        }

        PieceList enemyBishops = board.bishops[opponentColourIndex];
        for (int i = 0; i < enemyBishops.Count; i++) {
            UpdateSlidingAttackPiece(enemyBishops.getStartSquare(i), 4, 8);
        }
    }

    void UpdateSlidingAttackPiece(int startSquare, int startDirIndex, int endDirIndex) {

        for (int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            int currentDirOffset = PrecomputedMoveData.directionOffsets[directionIndex];
            for (int n = 0; n < PrecomputedMoveData.numSquaresToEdge[startSquare][directionIndex]; n++) {
                int targetSquare = startSquare + currentDirOffset * (n + 1);
                int targetSquarePiece = board.Square[targetSquare];
                opponentSlidingAttackMap |= 1L << targetSquare;
                if (targetSquare != friendlyKingSquare) {
                    if (targetSquarePiece != Piece.NONE) {
                        break;
                    }
                }
            }
        }
    }

    void CalculateAttackData() {
        // this function is resposible for giving the different variables such as inCheck or PinRayBitMask their actual values after some calculations
        // and just updating their status.
        
        
        // getting the opponent sliding attack map
        GenSlidingAttackMap();
        // Search squares in all directions around friendly king for checks/pins by enemy sliding pieces (queen, rook, bishop)
        // 0 and 8 by default which later be redefined if there are no queens on board
        int startDirIndex = 0;
        int endDirIndex = 8;

        if (board.queens[opponentColourIndex].Count == 0) {
            // changing the start and end direction indexes if there aren't any rooks or bishops on the board
            // since the start and end direction indexes are for the 8 different directions the sliding pieces can go to 
            // so, for a rook the directions will start at 0 and end on 4 and the bishop will start at 4 and end at 8 and 
            // the queen will go from 0 to 8
            startDirIndex = (board.rooks[opponentColourIndex].Count > 0) ? 0 : 4;
            endDirIndex = (board.bishops[opponentColourIndex].Count > 0) ? 8 : 4;
        }
        // looping through the direction as we need 
        for (int dir = startDirIndex; dir < endDirIndex; dir++) {
            // if the direction is greater then 3 that means we are looking at the diagonals lines.
            boolean isDiagonal = dir > 3;

            // the numSquaresToEdge is a 2 dimentional array which counts how many square are there from a given place to the edge 
            // of the board if we were to look at a certain direction.
            // so, we save the amount of squares from our place to the edge of the board
            int n = PrecomputedMoveData.numSquaresToEdge[friendlyKingSquare][dir];
            // directionOffset is something we add to our square so that we only go in one direction
            int directionOffset = PrecomputedMoveData.directionOffsets[dir];
            // defining 2 new variables to help us later
            boolean isFriendlyPieceAlongRay = false;
            long rayMask = 0;

            // Here we are looping through the squares that we want to go to 
            for (int i = 0; i < n; i++) {
                // and saving the square position number
                int squareIndex = friendlyKingSquare + directionOffset * (i + 1);
                // by using an arithmetic operation we set a bit at the rayMask to let us know that we are looking at that general area
                // by doing this a few times we get the full ray that we are looking at.
                rayMask |= 1L << squareIndex;
                // we also saving the piece that is on that square for later use
                int piece = board.Square[squareIndex];

                // This square contains a piece
                if (piece != Piece.NONE) {
                    // This piece is a Friendly piece
                    if (Piece.IsColor(piece, friendlyColour)) {
                        // First friendly piece we have come across in this direction, so it might be pinned
                        // We've initialized this variable as False so this will be False only once until we leave the loop
                        if (!isFriendlyPieceAlongRay) {
                            isFriendlyPieceAlongRay = true;
                        } // This is the second friendly piece we've found in this direction, therefore pin is not possible
                        else {
                            break;
                        }
                    } // This square contains an enemy piece
                    else {
                        // saving the piece type for later use
                        int pieceType = Piece.PieceType(piece);

                        // Check if piece is in bitmask of pieces able to move in current direction
                        if (isDiagonal && Piece.IsBishopOrQueen(pieceType) || !isDiagonal && Piece.IsRookOrQueen(pieceType)) {
                            // Friendly piece blocks the check, so this is a pin
                            if (isFriendlyPieceAlongRay) {
                                pinsExistInPosition = true;
                                // adding this mask to our global pinRay mask
                                pinRayBitmask |= rayMask;
                            } // No friendly piece blocking the attack, so this is a check
                            else {
                                // adding this rayMask to our global checkRay mask
                                checkRayBitmask |= rayMask;
                                inDoubleCheck = inCheck; // if already in check, then this is double check
                                inCheck = true;
                            }
                            break;
                        } else {
                            // This enemy piece is not able to move in the current direction, and so is blocking any checks/pins
                            break;
                        }
                    }
                }
            }
            // Stop searching for pins if in double check, as the king is the only piece able to move in that case anyway
            if (inDoubleCheck) {
                break;
            }

        }

        // Knight attacks
        // PieceList is another utility class. It's basically a Bitboard class
        // we are saving the opponents knights positions
        PieceList opponentKnights = board.knights[opponentColourIndex];
        // reseting the knights attack map
        opponentKnightAttacks = 0;
        // creating a boolean to see if we have a knight-king check
        boolean isKnightCheck = false;

        // same as we did for the king but this time we can have more than one knight so we first loop through all the knight and then applying 
        // the same logic as the king to the knights excluding the direction options but rather using the actual fixed target square, since this piece
        // can only go to a fixed square unlike the sliding pieces
        for (int knightIndex = 0; knightIndex < opponentKnights.Count; knightIndex++) {
            int startSquare = opponentKnights.getStartSquare(knightIndex);
            opponentKnightAttacks |= PrecomputedMoveData.knightAttackBitboards[startSquare];

            // This knight is attacking the king 
            if (!isKnightCheck && BitBoardUtility.ContainsSquare(opponentKnightAttacks, friendlyKingSquare)) {
                isKnightCheck = true;
                inDoubleCheck = inCheck; // if already in check, then this is double check
                inCheck = true;
                // adding this square to our global check ray mask
                checkRayBitmask |= 1L << startSquare;
            }
        }

        // Pawn attacks
        // the same as the knight but for pawns
        PieceList opponentPawns = board.pawns[opponentColourIndex];
        opponentPawnAttackMap = 0;
        boolean isPawnCheck = false;

        for (int pawnIndex = 0; pawnIndex < opponentPawns.Count; pawnIndex++) {
            int pawnSquare = opponentPawns.getStartSquare(pawnIndex);
            long pawnAttacks = PrecomputedMoveData.pawnAttackBitboards[pawnSquare][opponentColourIndex];
            opponentPawnAttackMap |= pawnAttacks;

            if (!isPawnCheck && BitBoardUtility.ContainsSquare(pawnAttacks, friendlyKingSquare)) {
                isPawnCheck = true;
                inDoubleCheck = inCheck; // if already in check, then this is double check
                inCheck = true;
                checkRayBitmask |= 1L << pawnSquare;
            }
        }

        int enemyKingSquare = board.KingSquare[opponentColourIndex];

        // lastly we update the global variables of the attack map and the attack map without the pawns
        opponentAttackMapNoPawns = opponentSlidingAttackMap | opponentKnightAttacks | PrecomputedMoveData.kingAttackBitboards[enemyKingSquare];
        opponentAttackMap = opponentAttackMapNoPawns | opponentPawnAttackMap;
    }

    boolean SquareIsAttacked(int square) {
        return BitBoardUtility.ContainsSquare(opponentAttackMap, square);
    }

    boolean InCheckAfterEnPassant(int startSquare, int targetSquare, int epCapturedPawnSquare) {
        // Update board to reflect en-passant capture
        board.Square[targetSquare] = board.Square[startSquare];
        board.Square[startSquare] = Piece.NONE;
        board.Square[epCapturedPawnSquare] = Piece.NONE;

        boolean inCheckAfterEpCapture = false;
        if (SquareAttackedAfterEPCapture(epCapturedPawnSquare, startSquare)) {
            inCheckAfterEpCapture = true;
        }

        // Undo change to board
        board.Square[targetSquare] = Piece.NONE;
        board.Square[startSquare] = Piece.PAWN | friendlyColour;
        board.Square[epCapturedPawnSquare] = Piece.PAWN | opponentColour;
        return inCheckAfterEpCapture;
    }

    boolean SquareAttackedAfterEPCapture(int epCaptureSquare, int capturingPawnStartSquare) {
        if (BitBoardUtility.ContainsSquare(opponentAttackMapNoPawns, friendlyKingSquare)) {
            return true;
        }

        // Loop through the horizontal direction towards ep capture to see if any enemy piece now attacks king
        int dirIndex = (epCaptureSquare < friendlyKingSquare) ? 2 : 3;
        for (int i = 0; i < PrecomputedMoveData.numSquaresToEdge[friendlyKingSquare][dirIndex]; i++) {
            int squareIndex = friendlyKingSquare + PrecomputedMoveData.directionOffsets[dirIndex] * (i + 1);
            int piece = board.Square[squareIndex];
            if (piece != Piece.NONE) {
                // Friendly piece is blocking view of this square from the enemy.
                if (Piece.IsColor(piece, friendlyColour)) {
                    break;
                } // This square contains an enemy piece
                else {
                    if (Piece.IsRookOrQueen(piece)) {
                        return true;
                    } else {
                        // This piece is not able to move in the current direction, and is therefore blocking any checks along this line
                        break;
                    }
                }
            }
        }

        // check if enemy pawn is controlling this square (can't use pawn attack bitboard, because pawn has been captured)
        for (int i = 0; i < 2; i++) {
            // Check if square exists diagonal to friendly king from which enemy pawn could be attacking it
            if (PrecomputedMoveData.numSquaresToEdge[friendlyKingSquare][PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][i]] > 0) {
                // move in direction friendly pawns attack to get square from which enemy pawn would attack
                int piece = board.Square[friendlyKingSquare + PrecomputedMoveData.directionOffsets[PrecomputedMoveData.pawnAttackDirections[friendlyColourIndex][i]]];
                if (piece == (Piece.PAWN | opponentColour)) // is enemy pawn
                {
                    return true;
                }
            }
        }

        return false;
    }

}
