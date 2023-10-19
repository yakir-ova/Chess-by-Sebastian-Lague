package chessbysebastianlague;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author yakir
 */
class onSearchComplete {

    static void Invoke(Move bestMove, Board board) {
        board.MakeMove(bestMove, true);
    }

}
