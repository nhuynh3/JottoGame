package ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import model.JottoModel;

/**
 * A GUI that allows the user to play
 * a Jotto Game through two types
 * of actions. 
 * 
 * Changing Puzzles:
 * Updates the puzzle number based on text in 
 * the puzzle text field
 * -Generates a random puzzle if the input is <=0 or
 * contains characters that aren't digits
 * -Otherwise, updates the puzzle to the given puzzle
 * number
 * 
 * Making guesses:
 * Updates the table based on guesses from the client 
 * and responses from the server. Each item is updated
 * as it is received
 * The GUI will respond in 3 ways:
 * -Reports an invalid string error if the guess
 *  contains numbers, of the incorrect length, or
 *  a nondictionary word
 * -Reports an invalid format if the guess is empty
 * -Reports the number of letter and position matches
 *  if the guess is valid
 * 
 * Threadsafe:
 * This GUI is threadsafe and responsive because it
 * makes use of AtomicIntegers and threadsafe SwingWorkers.
 * 
 * The workers ensure that the GUI is responsive and can
 * process multiple guesses from the user. 
 * A new worker is created for each guess to ensure that the row
 * that specific worker reports back to only ever gets assigned 
 * to that worker
 * Additionally, an atomic integer is used to represent the row of
 * the next guess. It is only updated after a guess is added to
 * the table or when the puzzle is reset. Its atomic nature ensures 
 * its thread-safe property
 *      
 */
public class JottoGUI extends JFrame {

    private static final int DEFAULT_TEXT_FIELD_SIZE = 80;
    private static final long serialVersionUID = 1L; // required by Serializable
    private static final int DEFAULT_PUZZLE_ID = 11;
    private static final int DEFAULT_LABEL_SIZE = 120;
    private static final int DEFAULT_ROWS = 10; 
    private static final int DEFAULT_COLUMNS= 3; 

    private static final int LETTER_MATCH_COLUMN = 1;
    private static final int POSITION_MATCH_COLUMN = 2;
    private static final int SCROLL_PANE_WIDTH = 300;
    private static final int SCROLL_PANE_HEIGHT = 200;

    private AtomicInteger row = new AtomicInteger();
    private List<SwingWorker<String, String>> guessWorkerList= new LinkedList<SwingWorker<String, String>>();


    // components to use in the GUI
    private final JButton newPuzzleButton;
    private final JTextField newPuzzleNumber;
    private final JLabel puzzleNumber;
    private final JTextField guess;
    private final JTable guessTable;
    private final JScrollPane scrollPane;



    private int currentPuzzleID;
    private DefaultTableModel model;

    /**
     * A mutable, threadsafe GUI that models a Jotto game.
     * 
     */
    public JottoGUI() {
        // components must be named with "setName" as specified in the problem set
        // remember to use these objects in your GUI!
        newPuzzleButton = new JButton();
        newPuzzleButton.setName("newPuzzleButton");
        newPuzzleNumber = new JTextField();
        newPuzzleNumber.setName("newPuzzleNumber");
        puzzleNumber = new JLabel();
        puzzleNumber.setName("puzzleNumber");
        guess = new JTextField();
        guess.setName("guess");

        model = new DefaultTableModel(DEFAULT_ROWS, DEFAULT_COLUMNS);
        guessTable = new JTable(model);
        guessTable.setName("guessTable");
        model.setColumnIdentifiers(new String[] {"Guess", "Letters", "Position"});

        scrollPane = new JScrollPane(guessTable);
        scrollPane.setPreferredSize(new Dimension(SCROLL_PANE_WIDTH, SCROLL_PANE_HEIGHT));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane);

        setFormat();
        addListeners();

    }

    /**
     * Creates listeners for changing the puzzle number
     * and for making guesses
     */
    private void addListeners() {
        /**
         * Puzzle number change listener
         * waits for the newPuzzleBUtton to be hit
         * or for the Return key to be pressed
         * 
         */
        class PuzzleNumListener implements ActionListener{

            private static final double NUMBER_OF_PUZZLES = 10000;

            @Override
            public void actionPerformed(ActionEvent e) {
                int newPuzzleID = (int) (Math.random()*NUMBER_OF_PUZZLES);
                String newPuzzleText = newPuzzleNumber.getText();
                if (newPuzzleText.matches("[0-9]+")) {
                    int tempID = Integer.parseInt(newPuzzleText);
                    if (tempID > 0) { newPuzzleID = tempID;}   
                }
                setPuzzleNumber(newPuzzleID);
            }

        }
        ActionListener puzzleNumListener = new PuzzleNumListener();
        newPuzzleNumber.addActionListener(puzzleNumListener);
        newPuzzleButton.addActionListener(puzzleNumListener);
        row.set(0);

        /**
         * Listens for user input into the guess field
         * 
         * Updates GUI based on input
         * If the input is invalid or misformatted, 
         * the GUI will display an error message,
         * otherwise it will display a JOTTO game response
         *  
         * Deals with delays in the server by creating a new
         * thread once each guess is submitted
         *
         */
        class GuessListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) { 
                String guessText = guess.getText().toLowerCase();               
                model.setValueAt(guessText, row.get(), 0);
                guess.setText("");

                Integer currentRow = new Integer(row.incrementAndGet()-1);
                if (row.get() >= model.getRowCount()) {model.addRow(new String[DEFAULT_COLUMNS]);}

                GuessWorker guessWorker = new GuessWorker();
                guessWorkerList.add(guessWorker);
                guessWorker.setGuess(guessText);
                guessWorker.setRow(currentRow);

                try {
                    guessWorker.execute();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }



        }
        ActionListener guessListener = new GuessListener();
        guess.addActionListener(guessListener);
    }

    /**
     * A mutable worker that runs in the background to retrieve
     * messages from the server
     * 
     * Updates GUI based on response
     * If the input is invalid or misformatted, 
     * the GUI will display an error message,
     * otherwise it will display a JOTTO game response
     *  
     */
    class GuessWorker extends SwingWorker<String, String> {
        private String userGuess; 
        private Integer currentRow;

        @Override
        protected String doInBackground() throws Exception {
            JottoModel jottoModel = new JottoModel();
            String response = jottoModel.makeGuess(userGuess.toLowerCase(), currentPuzzleID);
            return response;
        }

        @Override
        protected void done() {
            String response;
            try {
                response = this.get();

                String[] responseArray = response.split(" ");
                String output = response;
                String letterRow = "";
                String positionRow = "";
                if (response.startsWith("guess")) {
                    if (response.equals("guess 5 5")) {
                        letterRow = "You win!";
                        output = "You win!!! The secret word was " + userGuess;
                    } else {
                        letterRow = responseArray[1];
                        positionRow = responseArray[2];
                    }
                } else if (response.startsWith("error 0:")) {
                    letterRow = "Incorrectly formatted request";
                } else if (response.startsWith("error 2:")) {
                    letterRow = "Invalid guess.";
                }

                model.setValueAt(letterRow, currentRow, LETTER_MATCH_COLUMN);
                model.setValueAt(positionRow, currentRow, POSITION_MATCH_COLUMN);
                System.out.println(output);
            } catch (CancellationException e) {
                System.out.println("Aborting all unresolved guesses");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } 

        /**
         * sets the guess to be guessText
         * @param guessText a user's guess
         */
        public void setGuess(String guessText) {
            userGuess = guessText;
        }

        /**
         * sets the row number that this worker paints
         * @param row an Integer representing the row
         *      painted when this worker's response is retrieved
         */
        public void setRow(Integer row) {
            currentRow = row;
        }
    }

    /**
     * Updates the puzzle given a puzzle number 
     * Refreshes the GUI 
     * @param newPuzzleID an integer identifying the puzzle to be played
     */
    public void setPuzzleNumber(int newPuzzleID) {
        for (SwingWorker<String, String> worker : guessWorkerList) {
            worker.cancel(true);
        }
        currentPuzzleID = newPuzzleID;
        puzzleNumber.setText("Puzzle #" + newPuzzleID);
        guess.setText("");
        model.setRowCount(DEFAULT_ROWS);
        for (int r = 0; r< model.getRowCount(); r++) {
            for (int c =0; c<model.getColumnCount(); c++) {
                model.setValueAt("", r, c);   
            }
        }
        row.set(0);

        newPuzzleNumber.setText("");

    }

    /**
     * creates the layout of the GUI elements
     */
    private void setFormat() {
        this.currentPuzzleID = DEFAULT_PUZZLE_ID;
        puzzleNumber.setText("Puzzle #" + currentPuzzleID);
        newPuzzleButton.setText("New Puzzle");
        JLabel guessLabel = new JLabel();
        guessLabel.setText("Type a guess here:");

        Container panel = getContentPane();
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(puzzleNumber)
                        .addComponent(newPuzzleButton)
                        .addComponent(newPuzzleNumber))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(guessLabel)
                                .addComponent(guess))
                                .addComponent(scrollPane)
                );

        layout.setHorizontalGroup(
                layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                        .addComponent(puzzleNumber, DEFAULT_LABEL_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        .addComponent(newPuzzleButton)
                        .addComponent(newPuzzleNumber, DEFAULT_TEXT_FIELD_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(guessLabel)
                                .addComponent(guess))
                                .addComponent(scrollPane)
                );

        setTitle("Jotto Game");
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    /**
     * Start the GUI Jotto client.
     * @param args unused
     */
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JottoGUI main = new JottoGUI();

                main.setVisible(true);
            }
        });
    }
}
