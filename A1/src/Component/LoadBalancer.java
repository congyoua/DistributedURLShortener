package Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * A load balancer for the URL shortener application.
 * Listens for client connections and creates threads to handle them.
 */
public class LoadBalancer {

    static final int PORT = 8080;       // Default port number
    static final String FILE = "hosts"; // Default hosts file path

    public static void main(String[] args) {
        int port = PORT;
        String file = FILE;

        if (args == null || (args.length != 0 && args.length != 2)) {
            System.out.println("Usage: java LoadBalancer");
            System.out.println("       java LoadBalancer <port> <file>");
            System.exit(0);
        } else if (args.length == 2) {
            port = Integer.parseInt(args[0]);
            file = args[1];
        }

        ArrayList<ServerInfo> serverArray = new ArrayList<>();
        int numServers = 0;

        try (Scanner scanner = new Scanner(new File(file))) {
            scanner.useDelimiter("[:\n]"); // Split host:port pairs
            while (scanner.hasNext()) {
                String h = scanner.next();
                int p = Integer.parseInt(scanner.next());
                serverArray.add(new ServerInfo(h, p));
                numServers++;
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not open: " + e.getMessage());
            System.exit(1);
        }

        LoadBalancerInfo loadBalancerInfo = new LoadBalancerInfo(serverArray, numServers);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Load balancer started.\n"
                               + "Listening for connections on port " + port + "...\n");
            while (true) {
                Socket client = serverSocket.accept();
                Thread thread = new Thread(new LoadBalancerThread(loadBalancerInfo, client));
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
