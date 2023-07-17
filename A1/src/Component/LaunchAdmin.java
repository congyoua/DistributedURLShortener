package Component;

/**
 * Performs the initial configuration and then starts the admin tool.
 */
public class LaunchAdmin {
    public static void main(String[] args) {
        Admin admin = new Admin();
        System.out.println("Admin started at port 8800");
        admin.start();
    }
}
