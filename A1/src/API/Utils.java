package API;

public class Utils {
    public int hashChecker(String msg, int num){
        return (msg.hashCode() % num);
    }
}
