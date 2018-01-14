import java.util.*;
/**
 * Write a description of class Node here.
 * 
 * @author (Mayank Chander) 
 * @version (a version number or a date)
 */
public class Node
{
    // instance variables - replace the example below with your own
    protected int row;
    protected int col;
    protected String direction;
    Node parent; // pointer to the parent node, will be usefull to traverse back to the root and find the shortest path

    /**
     * Constructor for objects of class Node
     */
    public Node(int x, int y, Node parent, String dir)
    {
        this.row = x;
        this.col = y;
        this.parent = parent;
        this.direction = dir;
    }

    protected Node getParent() {
        return parent;
    }
    

    public String toString() {
        return "row = "+row+" col ="+col;
    }
}
