package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Immutable class responsible for interfacing
 * with the server
 */
public class JottoModel {
    private static final String PREFIX_URL = "http://courses.csail.mit.edu/6.005/jotto.py";
    
    /**
     * Takes in a five letter word guess and a puzzleID.
     * Returns the result of placing a guess on that puzzle,
     * an error message from the puzzle, or throws an error if
     * the html cannot be read
     * 
     * @param puzzleID > 0
     * @param guess - a string of length five
     * @return String representing the result of making that guess
     * on the puzzle
     * 
     * @throws IOException if the html cannot be read
     */
    
    public String makeGuess(String guess, Integer puzzleID) throws IOException {
        String guessURL = PREFIX_URL + "?puzzle=" + puzzleID.toString() + "&guess=" + guess;
        URL csail = new URL(guessURL);
        BufferedReader in = new BufferedReader(new InputStreamReader(csail.openStream()));
        
        String readLine = null;
        readLine = in.readLine();
        in.close();
        return readLine;
        
    }
}
