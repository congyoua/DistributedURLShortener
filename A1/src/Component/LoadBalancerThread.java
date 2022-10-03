package Component;

import java.io.*;
import java.net.Socket;

/**
 * A thread object for LoadBalancer.
 * Distributes client requests to servers.
 */
public class LoadBalancerThread implements Runnable {

    static final int MAX_BUF_SIZE = 8192; // Maximum char buffer size
    LoadBalancerInfo loadBalancerInfo;
    Socket client, server;

    /**
     * Creates an instance for the load balancer that executed by a thread.
     *
     * @param loadBalancerInfo the load balancer information
     * @param client           a socket to the client
     */
    public LoadBalancerThread(LoadBalancerInfo loadBalancerInfo, Socket client) {
        this.loadBalancerInfo = loadBalancerInfo;
        this.client = client;
        this.server = null;
    }

    /**
     * Selects a server to distribute a request to if there are any available.
     * If the load balancer cannot connect to a server, then it is removed
     * from its server list.
     */
    private void selectServer() {
        boolean isSelecting = true;
        while (isSelecting) {
            ServerInfo serverInfo = this.loadBalancerInfo.selectServer();
            if (serverInfo == null) {
                System.err.println("Could not distribute request. No servers are available.");
                isSelecting = false;
            } else {
                String host = serverInfo.getHost();
                int port = serverInfo.getPort();
                try {
                    this.server = new Socket(host, port);
                    System.out.println("Sending request to " + host + ":" + port);
                    isSelecting = false;
                } catch (IOException e) {
                    System.err.println("Could not connect to " + host + ":" + port
                            + ", sending request to another server.");
                    System.out.println("Removing " + host + ":" + port + " from server list");
                    this.loadBalancerInfo.removeServer(host, port);
                }
            }
        }
    }

    /**
     * Closes the sockets to the client and server.
     */
    private void closeSockets() {
        try {
            this.client.close();
            if (this.server != null) {
                this.server.close();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Establishes a connection between the client and an appropriate server.
     */
    @Override
    public void run() {
        this.selectServer();
        if (this.server != null) {
            try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
                 BufferedWriter clientOut = new BufferedWriter(new OutputStreamWriter(this.client.getOutputStream()));
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
                 BufferedWriter serverOut = new BufferedWriter(new OutputStreamWriter(server.getOutputStream())))
            {
                char[] buf = new char[MAX_BUF_SIZE];
                int numCharRead = 0;

                // Receive request from the client and send it to the server
                numCharRead = clientIn.read(buf, 0, MAX_BUF_SIZE);
                serverOut.write(buf, 0, numCharRead);
                serverOut.flush();

                // Receive response from the server and send it to the client
                numCharRead = serverIn.read(buf, 0, MAX_BUF_SIZE);
                clientOut.write(buf, 0, numCharRead);
                clientOut.flush();
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        this.closeSockets();
    }
}
