package model;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

/**
 * Test Partition:
 * <, =, > 5 length words without star
 * 5 length wodr with star
 * 
 */
public class JottoModelTest {
    
    /**
     * guess of <,=,> 5 letters and nondictionary word
     * guess with delay
     * 
     */
    private JottoModel model;
     
    public static final String ERR2 = "error 2: Invalid guess. Length of guess != 5 or guess is not a dictionary word.";
    @Before
    public void setup() {
        model = new JottoModel();
    }
    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; 
    }
    
    @Test
    public void testEqualFive() throws IOException {
        assertEquals("guess 2 1", model.makeGuess("hello", 1)); 
    }

    @Test
    public void testEqualFiveNonDict() throws IOException {
        assertEquals(ERR2, model.makeGuess("hella", 1)); 
    }
    
    @Test
    public void testGreaterThanFive() throws IOException {
        assertEquals(ERR2, model.makeGuess("hellololol", 1)); 
    }
    
    @Test
    public void testLessThanFive() throws IOException {
        assertEquals(ERR2, model.makeGuess("lol", 1)); 
    }
    
    @Test
    public void testDelay() throws IOException {
        assertEquals("guess 1 0", model.makeGuess("*hell", 1));
    }

}
