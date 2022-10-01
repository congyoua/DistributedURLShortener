package Component;

import java.io.*;
import java.net.Socket;

/**
 * A thread object for LoadBalancer.
 * Forwards a request to the appropriate server.
 */
public class LoadBalancerThread implements Runnable {

    LoadBalancerInfo loadBalancerInfo;
    Socket client;

    /**
     * Creates an instance for the load balancer that executed by a thread.
     *
     * @param loadBalancerInfo the load balancer information
     * @param client           a socket to the client
     */
    public LoadBalancerThread(LoadBalancerInfo loadBalancerInfo, Socket client) {
        this.loadBalancerInfo = loadBalancerInfo;
        this.client = client;
    }

    /**
     * Establishes a connection between the client and an appropriate server.
     */
    @Override
    public void run() {
        ServerInfo serverInfo = this.loadBalancerInfo.chooseServer();
        String host = serverInfo.getHost();
        int port = serverInfo.getPort();

        try (Socket server = new Socket(host, port)) {
            System.out.println("Request forwarded to " + host + ":" + port);
            try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
                 PrintWriter clientOut = new PrintWriter(this.client.getOutputStream(), true);
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
                 PrintWriter serverOut = new PrintWriter(server.getOutputStream(), true)) {

                String clientInput, serverInput;
                while ((clientInput = clientIn.readLine()) != null) {
                    serverOut.println(clientInput);
                }
                while ((serverInput = serverIn.readLine()) != null) {
                    clientOut.println(serverInput);
                }
            }
            this.client.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
