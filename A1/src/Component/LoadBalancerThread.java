package Component;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A thread object for LoadBalancer.
 * Distributes client requests to servers.
 */
public class LoadBalancerThread implements Runnable {

    static final int MAX_DATA_LEN = 8192; // Maximum char

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
        server = null;
    }

    /**
     * Selects a server to distribute a request to if there are any available.
     * If the load balancer cannot connect to a server, then it is removed
     * from its server list.
     */
    private void selectServer() {
        while (true) {
            NodeAddress nodeAddress = loadBalancerInfo.selectServer();
            if (nodeAddress == null) {
                System.err.println("Could not distribute request. No servers are available.");
                break;
            } else {
                String host = nodeAddress.host();
                int port = nodeAddress.port();
                try {
                    server = new Socket(host, port);
                    System.out.println("Sending request to " + host + ":" + port);
                    break;
                } catch (IOException e) {
                    System.err.println("Could not connect to " + host + ":" + port
                                       + ", sending request to another server.");
                    System.out.println("Removing " + host + ":" + port + " from server list");
                    loadBalancerInfo.removeServer(host, port);
                }
            }
        }
    }

    /**
     * Closes the sockets to the client and server.
     */
    private void closeSockets() throws IOException {
        client.close();
        if (server != null) {
            server.close();
        }
    }

    /**
     * Returns the first line from the given character array as a string.
     * If a line is not found, then a string containing the specified number
     * of characters from the array is returned.
     * A line is terminated with a '\n' or '\r'.
     *
     * @param data   the data to parse
     * @param length the length of the data
     * @return       the first line
     */
    private String extractFirstLine(char[] data, int length) {
        for (int i = 0; i < length; i++) {
            if (data[i] == '\n' || data[i] == '\r') {
                return String.valueOf(data, 0, i);
            }
        }
        return String.valueOf(data, 0, length);
    }

    /**
     * Returns whether the given string contains an illegal combination of
     * characters.
     *
     * @param str the string to check
     * @return    whether the string contains illegal combinations
     */
    private boolean containsIgnore(String str) {
        return (str.contains("favicon.ico") || str.contains("short=") || str.contains("long="));
    }

    /**
     * Sends data to the client.
     *
     * @param data         the data to send
     * @param length       the length of the data
     * @throws IOException if sending data to the client fails
     */
    private void sendToClient(char[] data, int length) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
        out.write(data, 0, length);
        out.flush();
    }

    /**
     * Receives data from the client.
     *
     * @return             the data from the client and its length
     * @throws IOException if receiving data from the client fails
     */
    private CharArray receiveFromClient() throws IOException {
        char[] data = new char[MAX_DATA_LEN];
        int length;
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        length = in.read(data, 0, MAX_DATA_LEN);
        return new CharArray(data, length);
    }

    /**
     * Sends data to the server.
     *
     * @param data         the data to send
     * @param length       the length of the data
     * @throws IOException if sending data to the server fails
     */
    private void sendToServer(char[] data, int length) throws IOException {
        if (server == null) {
            throw new IOException("Server socket is not set");
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
        out.write(data, 0, length);
        out.flush();
    }

    /**
     * Receives data from the server.
     *
     * @return             the data from the server and its length
     * @throws IOException if receiving data from the server fails
     */
    private CharArray receiveFromServer() throws IOException {
        if (server == null) {
            throw new IOException("Server socket is not set");
        }
        char[] data = new char[MAX_DATA_LEN];
        int length;
        BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
        length = in.read(data, 0, MAX_DATA_LEN);
        return new CharArray(data, length);
    }

    /**
     * Handles distributing the client's request, sending the client/servers
     * data, and receiving the client/servers data.
     * Caches redirect.
     */
    @Override
    public void run() {
        try {
            CharArray input = receiveFromClient();
            String firstLine = extractFirstLine(input.array(), input.length());

            Pattern pattern = Pattern.compile("^PUT\\s+/\\?short=\\S+&long=\\S+\\s+\\S+$");
            Matcher matcher = pattern.matcher(firstLine);

            if (matcher.matches()) { // PUT request
                selectServer();
                sendToServer(input.array(), input.length());
                CharArray output = receiveFromServer();
                sendToClient(output.array(), output.length());
            } else {
                pattern = Pattern.compile("^\\S+\\s+/(\\S+)\\s+\\S+$");
                matcher = pattern.matcher(firstLine);
                if (matcher.matches() && !containsIgnore(firstLine)) { // GET request
                    String shortURL = matcher.group(1);
                    if (loadBalancerInfo.isCached(shortURL)) { // Cached
                        System.out.println("Loading response from cache");
                        CharArray output = loadBalancerInfo.fetchFromCache(shortURL);
                        sendToClient(output.array(), output.length());
                    } else { // Not cached
                        System.out.println("Storing response to cache");
                        selectServer();
                        sendToServer(input.array(), input.length());
                        CharArray output = receiveFromServer();
                        sendToClient(output.array(), output.length());
                        loadBalancerInfo.storeToCache(shortURL, output);
                    }
                } else {
                    selectServer();
                    sendToServer(input.array(), input.length());
                    CharArray output = receiveFromServer();
                    sendToClient(output.array(), output.length());
                }
            }
            this.closeSockets();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
