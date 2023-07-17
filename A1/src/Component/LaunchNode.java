package Component;

/**
 * Node launcher
 */
public class LaunchNode {
    public static void main(String[] args){
        Node node = new Node();
        System.out.println("Node started at port 6666");
        node.start();
    }
}
