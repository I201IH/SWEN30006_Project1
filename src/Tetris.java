package src;// Tetris.java

import ch.aplu.jgamegrid.*;
import src.difficulty.Difficulty;
import src.difficulty.Easy;
import src.difficulty.Madness;
import src.difficulty.Medium;
import src.factory.RandomFactory;
import src.factory.TetrisPieceFactory;
import src.piece.*;

import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.*;
import javax.swing.*;

/**
 * Workshop 4 Friday 9:00, Team 12
 * Yi Wei 1166107
 * Thanh Nguyen Pham 1166068
 * Ian Han 1180762
 */

public class Tetris extends JFrame implements GGActListener {

    private Actor currentBlock;  // Currently active block
    private Actor blockPreview = null;   // Block in preview window
    private int score = 0;
    private int slowDown = 5;
    private Random random = new Random(0);
    private LinkedHashMap<String, Integer> numBlocks = new LinkedHashMap<>(); // Number of blocks in a game
    protected Difficulty diff = new Easy();
    private final int EASY_BOUND = 7;
    private final int DIFF_BOUND = 10;

    private TetrisGameCallback gameCallback;

    /**
     * Factories to create tetris pieces for game
     * difficulty change based on the bound
     */
    private RandomFactory factory = new RandomFactory(new TetrisPieceFactory[]{
            new I(this, diff.getCanRotate()),
            new J(this, diff.getCanRotate()),
            new L(this, diff.getCanRotate()),
            new O(this, diff.getCanRotate()),
            new S(this, diff.getCanRotate()),
            new T(this, diff.getCanRotate()),
            new Z(this, diff.getCanRotate()),
            new P(this, diff.getCanRotate()),
            new Q(this, diff.getCanRotate()),
            new Plus(this, diff.getCanRotate())
    });

    private RandomFactory factory2 = new RandomFactory(new TetrisPieceFactory[]{
            new I(this, diff.getCanRotate()),
            new J(this, diff.getCanRotate()),
            new L(this, diff.getCanRotate()),
            new O(this, diff.getCanRotate()),
            new S(this, diff.getCanRotate()),
            new T(this, diff.getCanRotate()),
            new Z(this, diff.getCanRotate()),
            new P(this, diff.getCanRotate()),
            new Q(this, diff.getCanRotate()),
            new Plus(this, diff.getCanRotate())
    });

    private boolean isAuto = false;

    private String difficulty;
    private Statistics statistics;

    private int seed = 30006;
    // For testing mode, the block will be moved automatically based on the blockActions.
    // L is for Left, R is for Right, T is for turning (rotating), and D for down
    private String [] blockActions = new String[10];
    private int blockActionIndex = 0;

    public Difficulty selectDiff(String difficulty){
        switch (difficulty){
            case ("easy"):
                diff = new Easy();
                break;
            case("medium"):
                diff = new Medium();
                break;
            case("madness"):
                diff = new Madness();
                break;
        }
        return diff;
    }

    /**
     * Initialise object
     * @param properties
     */
    private void initWithProperties(Properties properties) {
        this.seed = Integer.parseInt(properties.getProperty("seed", "30006"));
        random = new Random(seed);
        isAuto = Boolean.parseBoolean(properties.getProperty("isAuto"));
        String blockActionProperty = properties.getProperty("autoBlockActions", "");
        blockActions = blockActionProperty.split(",");
        difficulty = properties.getProperty("difficulty", "easy");
    }

    /**
     * Initialize a game
     * @param gameCallback
     * @param properties
     */
    public Tetris(TetrisGameCallback gameCallback, Properties properties) {
        // set score to 0, slowdown to 5 and a new storage to compute number of blocks
        reset();

        // Initialise value
        initWithProperties(properties);
        this.gameCallback = gameCallback;
        blockActionIndex = 0;
        // Set up the UI components. No need to modify the UI Components
        tetrisComponents = new TetrisComponents();
        tetrisComponents.initComponents(this);
        gameGrid1.addActListener(this);
        gameGrid1.setSimulationPeriod(getSimulationTime());

        // Add the first block to start
        currentBlock = createRandomTetrisBlock();
        gameGrid1.addActor(currentBlock, new Location(6, 0));
        gameGrid1.doRun();
        calculateNumBlocks((TetrisPiece) currentBlock);

        // Do not lose keyboard focus when clicking this window
        gameGrid2.setFocusable(false);
        setTitle("SWEN30006 Tetris Madness");

        showScore(score);

        statistics =  new Statistics(difficulty);
        diff = selectDiff(difficulty);

    }

    /**
     * Create a block and assign to a preview mode
     * @return current block
     */
    public Actor createRandomTetrisBlock() {
        if (blockPreview != null)
            blockPreview.removeSelf();

        // If the game is in auto test mode, then the block will be moved according to the blockActions
        String currentBlockMove = "";
        if (blockActions.length > blockActionIndex) {
            currentBlockMove = blockActions[blockActionIndex];
        }

        blockActionIndex++;

        RandomFactory current;
        RandomFactory current2;
        int bound = 7;

        current = factory;
        current2 = factory2;
        if (difficulty.equals("easy")){
            bound = EASY_BOUND;
        }
        else{
            bound = DIFF_BOUND;
        }

        Actor currentPiece = current.create(bound);
        if (isAuto) {
            ((TetrisPiece)currentPiece).setAutoBlockMove(currentBlockMove);
        }

         // To confirm get the same "random" pieces with test2
         // use another factory called current2
        TetrisPiece preview = current2.create(bound);
        while (!preview.getClass().getName().equals(currentPiece.getClass().getName())){
            TetrisPiece test2 = current2.create(bound);
            preview = test2;
        }

        preview.display(gameGrid2, new Location(2, 1));
        blockPreview = preview;

        //Show preview tetrisBlock
        currentPiece.setSlowDown(slowDown);
        return currentPiece;

    }

    /** Set current block
     * @param t current block
     */
    public void setCurrentTetrisBlock(Actor t) {
        gameCallback.changeOfBlock(currentBlock);
        currentBlock = t;
        TetrisPiece piece = (TetrisPiece) currentBlock;
        // calculate number of a specific block when it is played
        calculateNumBlocks(piece);
    }

    /** Calculate number of blocks in display
     * @param preview Block name
     */
    private void calculateNumBlocks(TetrisPiece preview) {
        String blockName = preview.getClass().getSimpleName();
        if (blockName.equals("Plus")) {
            blockName = "+";
        }
        if (!numBlocks.containsKey(blockName)) {
            numBlocks.put(blockName, 1);
        } else {
            numBlocks.put(blockName,
                    numBlocks.get(blockName) + 1);
        }
    }

    /**
     * Handle user input to move block. Arrow left to move left, Arrow right to move right, Arrow up to rotate and
     * Arrow down for going down
     * @param keyEvent Key that a user uses
     */
    private void moveBlock(int keyEvent) {
        switch (keyEvent) {
            case KeyEvent.VK_UP:
                if (diff.getCanRotate()){
                    ((TetrisPiece) currentBlock).rotate();
                }
                break;
            case KeyEvent.VK_LEFT:
                ((TetrisPiece) currentBlock).left();
                break;
            case KeyEvent.VK_RIGHT:
                ((TetrisPiece) currentBlock).right();
                break;
            case KeyEvent.VK_DOWN:
                ((TetrisPiece) currentBlock).drop();
                break;
            default:
                return;
        }
    }


    public void act() {
        removeFilledLine();
        moveBlock(gameGrid1.getKeyCode());
    }
    private void removeFilledLine() {
        for (int y = 0; y < gameGrid1.nbVertCells; y++) {
            boolean isLineComplete = true;
            TetroBlock[] blocks = new TetroBlock[gameGrid1.nbHorzCells];   // One line
            // Calculate if a line is complete
            for (int x = 0; x < gameGrid1.nbHorzCells; x++) {
                blocks[x] =
                        (TetroBlock) gameGrid1.getOneActorAt(new Location(x, y), TetroBlock.class);
                if (blocks[x] == null) {
                    isLineComplete = false;
                    break;
                }
            }
            if (isLineComplete) {
                // If a line is complete, we remove the component block of the shape that belongs to that line
                for (int x = 0; x < gameGrid1.nbHorzCells; x++)
                    gameGrid1.removeActor(blocks[x]);
                ArrayList<Actor> allBlocks = gameGrid1.getActors(TetroBlock.class);
                for (Actor a : allBlocks) {
                    int z = a.getY();
                    if (z < y)
                        a.setY(z + 1);
                }
                gameGrid1.refresh();
                score++;
                gameCallback.changeOfScore(score);
                showScore(score);
                slowDown = diff.setSpeed(score);
            }
        }
    }

    /**
     * Show score
     * @param score of a game
     */
    private void showScore(final int score) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                scoreText.setText(score + " points");
            }
        });
    }

    /**
     * When the game is over
     */
    public void gameOver() {
        gameGrid1.addActor(new Actor("sprites/gameover.gif"), new Location(5, 5));
        gameGrid1.doPause();
        statistics.printStatistics(score, numBlocks);
        if (isAuto) {
            System.exit(0);
        }
    }

    /**
     * Reset the game to restart
     */
    private void reset() {
        numBlocks = new LinkedHashMap<>();
        numBlocks.put("I", 0);
        numBlocks.put("J", 0);
        numBlocks.put("L", 0);
        numBlocks.put("O", 0);
        numBlocks.put("S", 0);
        numBlocks.put("T", 0);
        numBlocks.put("Z", 0);
        numBlocks.put("+", 0);
        numBlocks.put("P", 0);
        numBlocks.put("Q", 0);
        score = 0;
        slowDown = 5;
    }

    /**
     * Start a new game
     * @param evt
     */
    public void startBtnActionPerformed(java.awt.event.ActionEvent evt)
    {
        reset();
        gameGrid1.doPause();
        gameGrid1.removeAllActors();
        gameGrid2.removeAllActors();
        gameGrid1.refresh();
        gameGrid2.refresh();
        gameGrid2.delay(getDelayTime());
        blockActionIndex = 0;
        currentBlock = createRandomTetrisBlock();
        gameGrid1.addActor(currentBlock, new Location(6, 0));
        gameGrid1.doRun();
        gameGrid1.requestFocus();
        showScore(score);
    }

    /**
     * Different speed for manual and auto mode
     * @return speed
     */
    //
    private int getSimulationTime() {
        if (isAuto) {
            return 10;
        } else {
            return 100;
        }
    }

    private int getDelayTime() {
        if (isAuto) {
            return 200;
        } else {
            return 2000;
        }
    }

    // AUTO GENERATED - do not modify//GEN-BEGIN:variables
    public ch.aplu.jgamegrid.GameGrid gameGrid1;
    public ch.aplu.jgamegrid.GameGrid gameGrid2;
    public javax.swing.JPanel jPanel1;
    public javax.swing.JPanel jPanel2;
    public javax.swing.JPanel jPanel3;
    public javax.swing.JPanel jPanel4;
    public javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JTextArea jTextArea1;
    public javax.swing.JTextField scoreText;
    public javax.swing.JButton startBtn;
    private TetrisComponents tetrisComponents;
    // End of variables declaration//GEN-END:variables

}
