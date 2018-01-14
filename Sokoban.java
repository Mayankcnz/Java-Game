// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP103 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP103 - 2017T2, Assignment 2
 * Name: Mayank Chander
 * Username:
 * ID: 300384272
 */

import ecs100.*;
import java.util.*;
import java.io.*;

/** 
 * Sokoban
 */

public class Sokoban {

    private Square[][] squares;  // the array representing the warehouse
    private int rows;                  // the height of the warehouse
    private int cols;                   // the width of the warehouse

    private Coord workerPosition;     // the position of the worker
    private String workerDirection = "left"; // the direction the worker is facing
    private ActionRecord history;

    private final int maxLevels = 4; // maximum number of levels defined
    private int level = 0;                // current level 

    private Map<Character, String> fileCharacterToSquareType;  // character in level file -> square object
    private Map<String, String> directionToWorkerImage;        // worker direction ->  image of worker
    private Map<String, String> keyToAction;                  // key string -> action to perform
    private static final int leftMargin = 40;
    private static final int topMargin = 40;
    private static final int squareSize = 25;
    private int selectedRow;
    private int selectedCol;  // HAH

    private Deque<Node> path;
    private Stack<ActionRecord> undo;
    private Stack<ActionRecord>redo;
    // Constructors
    /** 
     *  Constructs a new Sokoban object
     *  and set up the GUI.
     */
    public Sokoban() {

        undo = new Stack<ActionRecord>();
        redo = new Stack<ActionRecord>();
        path = new LinkedList<Node>();

        UI.setMouseMotionListener(this::doMouse);
        UI.addButton("New Level", () -> {level = (level+1)%maxLevels; load(level);});
        UI.addButton("Restart", () -> {load(level);});
        UI.addButton("left", () -> doAction("left"));
        UI.addButton("up", () -> doAction("up"));
        UI.addButton("down", () -> doAction("down"));
        UI.addButton("right", () -> doAction("right"));
        UI.addButton("Undo", () -> pullBox());
        UI.addButton("Redo", () -> redoUndo());

        UI.println("Push the boxes\n to their target postions.");
        UI.println("You may use keys (wasd or ijkl and u)");
        UI.setKeyListener(this::doKey);

        initialiseMappings();
        load(0); // start with level zero
    }

    public void doMouse(String action, double x, double y) {

        if(action.equals("released")){

            if((y >= topMargin && y <= topMargin + (squareSize * squares.length)) &&
            (x >= leftMargin && x <= leftMargin + (squareSize * squares[0].length))) {

                this.selectedCol = (int) ((x-leftMargin) / squareSize);
                this.selectedRow = (int) ((y - topMargin) / squareSize);

                if(squares[this.selectedRow][this.selectedCol].isFree()){
                    if(doPaths()){UI.println("Path found!");}
                }else {
                    UI.println("Cannot move to that position");
                }         
            }
        }
    }

    private boolean doPaths() {  // BFS IMPLEMENTATION FOR SOKOBAN

        path.clear();


        path.add(new Node(workerPosition.row,workerPosition.col, null,workerDirection));   // add the first Node to the queue, it has no parent

        squares[this.selectedRow][this.selectedCol].setGoal(true);   // the goal node

        while(!path.isEmpty()){ 

            Node node = path.remove();
            if((squares[node.row][node.col].isGoal())){  // if the goal is founded , set the goal to false and call the doqeue method where we traverse back to the parent to find the shortest path
                squares[this.selectedRow][this.selectedCol].setGoal(false);
                doQueue(node);
                return true;
            }

            if(check(node.row+1,node.col)){ //  check the down neightbour, if have one

                squares[node.row][node.col].setVisited();
                Node newNode =  new Node(node.row+1,node.col,node,"down"); // create a new Node and assingn the current Node as its parent
                path.add(newNode);

            }

            if(check(node.row-1,node.col)){
                squares[node.row][node.col].setVisited();
                Node newNode = new Node(node.row-1,node.col,node,"up"); // create a new Node and assingn the current Node as its parent
                path.add(newNode);  

            }

            if(check(node.row,node.col+1)){
                squares[node.row][node.col].setVisited();

                Node newNode = new Node(node.row,node.col+1,node,"right"); // create a new Node and assingn the current Node as its parent
                path.add(newNode);
            }
            if(check(node.row,node.col-1)){
                squares[node.row][node.col].setVisited();

                Node newNode = new Node(node.row,node.col-1,node,"left");  // create a new Node and assingn the current Node as its parent
                path.add(newNode);
            }
        }
        
        return false;
    }

    public void doQueue(Node node) {

        Stack<Node> traverseBackToRoot = new Stack<Node>();
        traverseBackToRoot.add(node);
        while (node.getParent() != null){ // traverse Back to the parent Node and put the objects to stack

            node = node.getParent();
            traverseBackToRoot.add(node);

        }

        traverseBackToRoot.pop();
        while(!traverseBackToRoot.isEmpty()){    
            Node n = traverseBackToRoot.pop();   // pop the root from start to parent and draw the worker
            move(n.direction);
            drawWorker();
            UI.sleep(80);
        }

        for(int row = 0; row < squares.length; row++) {
            for(int col = 0; col < squares[0].length;col++){

                if(squares[row][col].isVisited()){
                    squares[row][col].setUnvisited();   // set all the visited nodes to false 
                }

            }
        }

    }

    private boolean check(int row, int col){
        return((row >= 0 && row < squares.length) && (col >= 0 && col < squares[row].length) && (squares[row][col].isFree() || squares[row][col].isGoal()) && (squares[row][col].isVisited() == false));
    }

    /** Responds to key actions */
    public void doKey(String key) {
        doAction(keyToAction.get(key));
    }

    /** 
     *  Moves the worker in the specified direction, if possible.
     *  If there is box in front of the Worker and a space in front of the box,
     *  then push the box.
     *  Otherwise, if there is anything in front of the Worker, do nothing.
     * @param action the action to perform 
     */
    public void doAction(String action) {
        if (action==null) 
            return;

        workerDirection = action; // action can only be a move; record it.

        Coord nextP = workerPosition.next(workerDirection);  // where the worker would move to
        Coord nextNextP = nextP.next(workerDirection);       // using the nextP reference variable

        // is a box push possible?
        if (squares[nextP.row][nextP.col].hasBox() && squares[nextNextP.row][nextNextP.col].isFree()) {
            push(workerDirection);
            undo.add(new ActionRecord("push",oppositeDirection(workerDirection))); // if current diurection is up it will insert down to the sta

            if (isSolved()) {
                UI.println("\n*** YOU WIN! ***\n");

                // flicker with the boxes to indicate win
                for (int i=0; i<12; i++) {
                    for (int row=0; row<rows; row++)
                        for (int column=0; column<cols; column++) {
                            Square square=squares[row][column];

                            // toggle shelf squares
                            if (square.hasBox()) {square.moveBoxOff(); drawSquare(row, column);}
                            else if (square.isEmptyShelf()) {square.moveBoxOn(); drawSquare(row, column);}
                        }

                    UI.sleep(100);
                }
            }
        }
        else if ( squares[nextP.row][nextP.col].isFree()) { // can the worker move?
            move(workerDirection);
            undo.add(new ActionRecord("null",oppositeDirection(workerDirection)));

        }
    }

    /** Moves the worker into the new position (guaranteed to be empty) 
     * @param direction the direction the worker is heading
     */
    public void move(String direction) {

        drawSquare(workerPosition); // display square under worker
        workerPosition = workerPosition.next(direction); // new worker position
        //squares[workerPosition.row][workerPosition.col].setVisited();

        drawWorker();  // display worker at new position
        Trace.println("Move " + direction);
    }

    /** Push: Moves the Worker, pushing the box one step 
     *  @param direction the direction the worker is heading
     */
    public void push(String direction) {
        drawSquare(workerPosition); // display square under worker

        workerPosition = workerPosition.next(direction); // new worker position

        Coord boxPosition = workerPosition.next(direction); // this is two steps from the original worker position

        squares[workerPosition.row][workerPosition.col].moveBoxOff(); // remove box from its current position
        drawSquare(workerPosition); // display square without the box
        drawWorker();  // display worker at new position

        squares[boxPosition.row][boxPosition.col].moveBoxOn(); // place box on its new position
        drawSquare(boxPosition);

        Trace.println("Push " + direction);
    }

    /** Pull: (useful for undoing a push in the opposite direction)
     *  move the Worker in direction from direction,
     *  pulling the box into the Worker's old position
     */
    public void pull(String direction, boolean action) {
        String oppositeDir = oppositeDirection(direction); // get the apposite direction. 

        Coord boxP = workerPosition.next(oppositeDir); 

        squares[boxP.row][boxP.col].moveBoxOff();
        squares[workerPosition.row][workerPosition.col].moveBoxOn();
        drawSquare(boxP);
        drawSquare(workerPosition);

        workerPosition = workerPosition.next(direction);
        workerDirection = oppositeDir;
        drawWorker();

        Trace.println("Pull " + direction);
    }

    public void pullBox() {
        if(undo.isEmpty())   // stack for undoing
            return;

        history = undo.pop();
        ActionRecord temp  = new ActionRecord(history.isPush() ? "push" : "null", oppositeDirection(history.direction()));// ATM REFERCING TO THE SAME OBBECT SO MAKING CHANGES TO IT // BASICALLY 2 VARIABLES REFERENCES TO SAME OBJECT       

        redo.add(temp);

        if(history.isMove())
            move(history.direction()); // will pass the apposite direction to Mmove Method
        else 
            pull(history.direction(), true);

    }

    public void redoUndo() {
        if(redo.isEmpty())  //stack for redoing
            return;

        history = redo.pop();

        ActionRecord temp = new ActionRecord(history.isPush() ? "push" : "null", oppositeDirection(history.direction()));
        undo.add (temp);

        if(history.isMove())Mmove(history.direction());
        else pull(history.direction(), true);

    }

    public void Mmove(String direction) {
        drawSquare(workerPosition); // display square under worker // pass the require worker Position
        workerPosition = workerPosition.next(direction); // new worker position
        drawWorkers();  // display worker at new position
        Trace.println("Move " + direction);

    }

    private void drawWorkers() {
        String dir = oppositeDirection(history.direction());
        UI.drawImage(directionToWorkerImage.get(dir),
            leftMargin+(squareSize* workerPosition.col),
            topMargin+(squareSize* workerPosition.row),
            squareSize, squareSize);
    }

    /** Load a grid of squares (and Worker position) from a file */
    public void load(int level) {

        undo.clear();
        redo.clear();

        File f = new File("warehouse" + level + ".txt"); // creates a new File object

        if (f.exists()) {
            List<String> lines = new ArrayList<String>();// create a new ArrayList of Type List

            try {
                Scanner sc = new Scanner(f);

                while (sc.hasNext())
                    lines.add(sc.nextLine()); // add each Line to the ArrayList

                sc.close();
            } catch(IOException e) {
                Trace.println("File error: " + e);
            }

            rows = lines.size();
            cols = lines.get(0).length(); // the length of the stored string

            squares = new Square[rows][cols]; // creates the new object of 2DArrays with specified elements

            for(int row = 0; row < rows; row++) {
                String line = lines.get(row); // for Each String Type object in the List, Get its Length();
                for(int col = 0; col < cols; col++) { // traverse n number of charcters in the first Line of the array

                    if (col>=line.length()) // if the string is larger than the string in 0th element
                        squares[row][col] = new Square("empty");
                    else {
                        char ch = line.charAt(col); 

                        if (fileCharacterToSquareType.containsKey(ch))
                            squares[row][col] = new Square(fileCharacterToSquareType.get(ch));
                        else {
                            squares[row][col] = new Square("empty");
                            UI.printf("Invalid char: (%d, %d) = %c \n", row, col, ch);
                        }

                        if (ch=='A') // its the worker
                            workerPosition = new Coord(row,col); // pass the specified row and col for that worker
                    }
                }
            }
            draw(); // lastly draw the things 

        }
    }

    // Drawing 

    /** Draw the grid of squares on the screen, and the Worker */
    public void draw() {
        UI.clearGraphics();
        // draw squares
        for(int row = 0; row<rows; row++)
            for(int col = 0; col<cols; col++)
                drawSquare(row, col);

        drawWorker();
    }

    private void drawWorker() {
        UI.drawImage(directionToWorkerImage.get(workerDirection),
            leftMargin+(squareSize* workerPosition.col),
            topMargin+(squareSize* workerPosition.row),
            squareSize, squareSize);
    }

    private void drawSquare(Coord pos) {
        drawSquare(pos.row, pos.col);
    }

    private void drawSquare(int row, int col) {
        String imageName = squares[row][col].imageName();

        if (imageName != ".gif") // draws an empty square
            UI.drawImage(imageName,
                leftMargin+(squareSize* col),
                topMargin+(squareSize* row),
                squareSize, squareSize);
    }

    /** 
     *  @returns true, if the warehouse is solved, i.e.,  
     *  all the shelves have boxes on them 
     */
    public boolean isSolved() {
        for(int row = 0; row<rows; row++) {
            for(int col = 0; col<cols; col++)
                if(squares[row][col].isEmptyShelf())
                    return  false;
        }

        return true;
    }

    /** 
     * @return the direction that is opposite of the parameter 
     */
    public String oppositeDirection(String direction) {
        if ( direction.equalsIgnoreCase("right")) return "left";
        if ( direction.equalsIgnoreCase("left"))  return "right";
        if ( direction.equalsIgnoreCase("up"))    return "down";
        if ( direction.equalsIgnoreCase("down"))  return "up";
        return direction;
    }

    private void initialiseMappings() {
        // character in level file -> square type
        fileCharacterToSquareType = new HashMap<Character, String>();
        fileCharacterToSquareType.put('.',  "empty");
        fileCharacterToSquareType.put('A', "empty");  // initial position of worker is an empty square beneath
        fileCharacterToSquareType.put('#',  "wall");
        fileCharacterToSquareType.put('S', "emptyShelf");
        fileCharacterToSquareType.put('B',  "box");

        // worker direction ->  image of worker
        directionToWorkerImage = new HashMap<String, String>();
        directionToWorkerImage.put("up", "worker-up.gif");
        directionToWorkerImage.put("down", "worker-down.gif");
        directionToWorkerImage.put("left", "worker-left.gif");
        directionToWorkerImage.put("right", "worker-right.gif");

        // key string -> action to perform
        keyToAction = new HashMap<String,String>();
        keyToAction.put("i", "up");     keyToAction.put("I", "up");   
        keyToAction.put("k", "down");   keyToAction.put("K", "down"); 
        keyToAction.put("j", "left");   keyToAction.put("J", "left"); 
        keyToAction.put("l", "right");  keyToAction.put("L", "right");

        keyToAction.put("w", "up");     keyToAction.put("W", "up");   
        keyToAction.put("s", "down");   keyToAction.put("S", "down"); 
        keyToAction.put("a", "left");   keyToAction.put("A", "left"); 
        keyToAction.put("d", "right");  keyToAction.put("D", "right");
    }

    public static void main(String[] args) {
        new Sokoban();
    }
}
