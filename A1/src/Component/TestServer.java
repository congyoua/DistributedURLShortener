package Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple server for testing purposes.
 * When a client connects, the server reads data from them and replies with
 * an acknowledgment, then the connection is closed.
 */
public class TestServer {

    static final int PORT = 3000; // Default port number

    public static void main(String[] args) {
        int port = PORT;

        if (args == null || (args.length != 0 && args.length != 1)) {
            System.out.println("Usage: java URLShortener");
            System.out.println("       java URLShortener <port>");
            System.exit(0);
        } else if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started.\n"
                               + "Listening for connections on port " + port + "...\n");
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Connection accepted.");
                try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                     PrintWriter out = new PrintWriter(client.getOutputStream())
                ) {
                    String input = in.readLine();
                    System.out.println("Received: " + input);
                    out.println("Server has received: " + input);
                    out.flush();
                }
                client.close();
                System.out.println("Connection closed.");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
