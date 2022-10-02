package API;

public class Utils {
    public static int hashChecker(String msg, int num){
        return (msg.hashCode() % num);
    }
}
