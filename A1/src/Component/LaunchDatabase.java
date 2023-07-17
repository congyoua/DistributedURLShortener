package Component;

/**
 * Database launcher
 */
public class LaunchDatabase {
    public static void main(String[] args){
        Database db = new Database();
        System.out.println("Database started at port 7777");
        db.start();
    }
}