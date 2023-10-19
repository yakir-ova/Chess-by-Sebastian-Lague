/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package chessbysebastianlague;

/**
 *
 * @author yakir
 */
public class ChessBySebastianLague {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Board board = new Board();
        AISettings aiSettings = new AISettings();
        Search search = new Search(board, aiSettings);
        PrecomputedMoveData PMD = new PrecomputedMoveData();
        board.LoadStartPosition();
        // search.StartSearch();
    }
    
}
