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
public class Search {

    static final int transpositionTableSize = 64000;
    static final int immediateMateScore = 100000;
    static final int positiveInfinity = 9999999;
    static final int negativeInfinity = -positiveInfinity;

    TranspositionTable tt;
    MoveGenerator moveGenerator;

    Move bestMoveThisIteration;
    int bestEvalThisIteration;
    Move bestMove;
    int bestEval;
    int currentIterativeSearchDepth;
    boolean abortSearch;

    Move invalidMove;
    MoveOrdering moveOrdering;
    AISettings settings;
    Board board;
    Evaluation evaluation;

    // Diagnostics
    public SearchDiagnostics searchDiagnostics;
    int numNodes;
    int numQNodes;
    int numCutoffs;
    int numTranspositions;

    public Search(Board board, AISettings settings) {
        this.board = board;
        this.settings = settings;
        evaluation = new Evaluation();
        moveGenerator = new MoveGenerator();
        tt = new TranspositionTable(board, transpositionTableSize);
//        moveOrdering = new MoveOrdering(moveGenerator, tt);
        invalidMove = Move.InvalidMove();
    }

    public void StartSearch() {
        InitDebugInfo();

        // Initialize search settings
        bestEvalThisIteration = bestEval = 0;
        bestMoveThisIteration = bestMove = Move.InvalidMove();
        tt.enabled = settings.useTranspositionTable;

        // Clearing the transposition table before each search seems to help
        // This makes no sense to me, I presume there is a bug somewhere but haven't been able to track it down yet
        if (settings.clearTTEachMove) {
            tt.Clear();
        }

        moveGenerator.promotionsToGenerate = settings.promotionsToSearch;
        currentIterativeSearchDepth = 0;
        abortSearch = false;
        searchDiagnostics = new SearchDiagnostics();

        // Iterative deepening. This means doing a full search with a depth of 1, then with a depth of 2, and so on.
        // This allows the search to be aborted at any time, while still yielding a useful result from the last search.
        if (settings.useIterativeDeepening) {
            int targetDepth = (settings.useFixedDepthSearch) ? settings.depth : Integer.MAX_VALUE;

            for (int searchDepth = 1; searchDepth <= targetDepth; searchDepth++) {
                SearchMoves(searchDepth, 0, negativeInfinity, positiveInfinity);
                if (abortSearch) {
                    break;
                } else {
                    currentIterativeSearchDepth = searchDepth;
                    bestMove = bestMoveThisIteration;
                    bestEval = bestEvalThisIteration;

                    // Update diagnostics
                    searchDiagnostics.lastCompletedDepth = searchDepth;
                    searchDiagnostics.move = bestMove.getName();
                    searchDiagnostics.eval = bestEval;
//                    searchDiagnostics.moveVal = Chess.PGNCreator.NotationFromMove(FenUtility.CurrentFen(board), bestMove);

                    // Exit search if found a mate
                    if (IsMateScore(bestEval) && !settings.endlessSearchMode) {
                        break;
                    }
                }
            }
        } else {
            SearchMoves(settings.depth, 0, negativeInfinity, positiveInfinity);
            bestMove = bestMoveThisIteration;
            bestEval = bestEvalThisIteration;
        }

        onSearchComplete.Invoke(bestMove, board);

        if (!settings.useThreading) {
            LogDebugInfo();
        }
    }

    public SearchResults GetSearchResult(Move move, int eval) {
        return new SearchResults(move, eval);
    }

    public void EndSearch() {
        abortSearch = true;
    }

    int SearchMoves(int depth, int plyFromRoot, int alpha, int beta) {
        if (abortSearch) {
            return 0;
        }

        if (plyFromRoot > 0) {
            // Detect draw by repetition.
            // Returns a draw score even if this position has only appeared once in the game history (for simplicity).
            if (board.RepetitionPositionHistory.contains(board.ZobristKey)) {
                return 0;
            }

            // Skip this position if a mating sequence has already been found earlier in
            // the search, which would be shorter than any mate we could find from here.
            // This is done by observing that alpha can't possibly be worse (and likewise
            // beta can't  possibly be better) than being mated in the current position.
            alpha = Integer.max(alpha, -immediateMateScore + plyFromRoot);
            beta = Integer.min(beta, immediateMateScore - plyFromRoot);
            if (alpha >= beta) {
                return alpha;
            }
        }

        // Try looking up the current position in the transposition table.
        // If the same position has already been searched to at least an equal depth
        // to the search we're doing now,we can just use the recorded evaluation.
        int ttVal = tt.LookupEvaluation(depth, plyFromRoot, alpha, beta);
        if (ttVal != TranspositionTable.lookupFailed) {
            numTranspositions++;
            if (plyFromRoot == 0) {
                bestMoveThisIteration = tt.GetStoredMove();
                bestEvalThisIteration = tt.entries[(int) tt.Index].value;
                //Debug.Log ("move retrieved " + bestMoveThisIteration.Name + " Node type: " + tt.entries[tt.Index].nodeType + " depth: " + tt.entries[tt.Index].depth);
            }
            return ttVal;
        }

        if (depth == 0) {
            return QuiescenceSearch(alpha, beta);
        }

        ArrayList<Move> moves = moveGenerator.GenerateMoves(board, true);
        moveOrdering.OrderMoves(board, moves, settings.useTranspositionTable);
        // Detect checkmate and stalemate when no legal moves are available
        if (moves.isEmpty()) {
            if (moveGenerator.InCheck()) {
                int mateScore = immediateMateScore - plyFromRoot;
                return -mateScore;
            } else {
                return 0;
            }
        }

        int evalType = TranspositionTable.UpperBound;
        Move bestMoveInThisPosition = invalidMove;

        for (int i = 0; i < moves.size(); i++) {
            board.MakeMove(moves.get(i), true);
            int eval = -SearchMoves(depth - 1, plyFromRoot + 1, -beta, -alpha);
            board.UnmakeMove(moves.get(i), true);
            numNodes++;

            // Move was *too* good, so opponent won't allow this position to be reached
            // (by choosing a different move earlier on). Skip remaining moves.
            if (eval >= beta) {
                tt.StoreEvaluation(depth, plyFromRoot, beta, TranspositionTable.LowerBound, moves.get(i));
                numCutoffs++;
                return beta;
            }

            // Found a new best move in this position
            if (eval > alpha) {
                evalType = TranspositionTable.Exact;
                bestMoveInThisPosition = moves.get(i);

                alpha = eval;
                if (plyFromRoot == 0) {
                    bestMoveThisIteration = moves.get(i);
                    bestEvalThisIteration = eval;
                }
            }
        }

        tt.StoreEvaluation(depth, plyFromRoot, alpha, evalType, bestMoveInThisPosition);

        return alpha;

    }

    // Search capture moves until a 'quiet' position is reached.
    int QuiescenceSearch(int alpha, int beta) {
        // A player isn't forced to make a capture (typically), so see what the evaluation is without capturing anything.
        // This prevents situations where a player only has bad captures available from being evaluated as bad,
        // when the player might have good non-capture moves available.
        int eval = evaluation.Evaluate(board);
        searchDiagnostics.numPositionsEvaluated++;
        if (eval >= beta) {
            return beta;
        }
        if (eval > alpha) {
            alpha = eval;
        }

        var moves = moveGenerator.GenerateMoves(board, false);
//        moveOrdering.OrderMoves(board, moves, false);
        for (int i = 0; i < moves.size(); i++) {
            board.MakeMove(moves.get(i), true);
            eval = -QuiescenceSearch(-beta, -alpha);
            board.UnmakeMove(moves.get(i), true);
            numQNodes++;

            if (eval >= beta) {
                numCutoffs++;
                return beta;
            }
            if (eval > alpha) {
                alpha = eval;
            }
        }

        return alpha;
    }

    public static boolean IsMateScore(int score) {
        final int maxMateDepth = 1000;
        return Math.abs(score) > immediateMateScore - maxMateDepth;
    }

    public static int NumPlyToMateFromScore(int score) {
        return immediateMateScore - Math.abs(score);

    }

    void LogDebugInfo() {
        AnnounceMate();
        System.out.println("Best move: " + bestMoveThisIteration.getName());
        System.out.println("Eval: " + bestEvalThisIteration);
        System.out.println("Num nodes: " + numNodes);
        System.out.println("num Qnodes: " + numQNodes);
        System.out.println("num cutoffs: " + numCutoffs);
        System.out.println("num TThits " + numTranspositions);
    }

    void AnnounceMate() {

        if (IsMateScore(bestEvalThisIteration)) {
            int numPlyToMate = NumPlyToMateFromScore(bestEvalThisIteration);
            //int numPlyToMateAfterThisMove = numPlyToMate - 1;

            int numMovesToMate = (int) Math.round(numPlyToMate / 2.0);

            String sideWithMate = (bestEvalThisIteration * ((board.WhiteToMove) ? 1 : -1) < 0) ? "Black" : "White";

            System.out.println(sideWithMate + " can mate in " + numMovesToMate + " move " + ((numMovesToMate > 1) ? "s" : ""));
        }
    }

    void InitDebugInfo() {
        numNodes = 0;
        numQNodes = 0;
        numCutoffs = 0;
        numTranspositions = 0;
    }

    public class SearchDiagnostics {

        public int lastCompletedDepth;
        public String moveVal;
        public String move;
        public int eval;
        public boolean isBook;
        public int numPositionsEvaluated;
    }

    public class SearchResults {

        Move bestMove;
        int Evaluation;

        public SearchResults(Move bestMove, int Evaluation) {
            this.bestMove = bestMove;
            this.Evaluation = Evaluation;
        }
    }

}
