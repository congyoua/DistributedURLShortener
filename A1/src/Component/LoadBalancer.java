package Component;

import API.Link;
import API.Utils;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A multithreaded load balancer for the URL shortener.
 * Distributes client requests to nodes and caches recently accessed URLs.
 */
public class LoadBalancer extends Link {

    static final File ROOT = new File("resources");
    static final String SERVICE_UNAVAILABLE = "unavailable.html";
    static final String REDIRECT = "redirect.html";

    static final int PORT = 5555;        // Default port number
    static final int MAX_MSG_LEN = 8192; // Maximum characters per message
    static final int CLEAN_TIME = 60000; // Interval to clean the cache in milliseconds

    static boolean verbose = false;

    LoadBalancerData loadBalancerData;
    String nodeHost;
    int nodePort;

    /**
     * Creates a load balancer object.
     *
     * @param data the shared load balancer data
     */
    public LoadBalancer(LoadBalancerData data) {
        super(PORT);
        loadBalancerData = data;
    }

    /**
     * Starts threads to clean the cache and listen for connections.
     */
    @Override
    public void start() {
        executor.execute(this::clean);
        super.start();
    }

    /**
     * Selects a node to distribute a request to and returns a socket.
     * Returns null if there are no nodes available.
     *
     * @return socket to a node
     */
    private Socket selectNode() {
        Socket node = null;
        boolean isSelecting = true;
        int tries = 0;
        while (isSelecting && tries < 5) {
            Address nodeAddress = loadBalancerData.selectNode();
            if (nodeAddress == null) {
                isSelecting = false;
                if (verbose) System.out.println("LB: No nodes are available");
            } else {
                nodeHost = nodeAddress.host();
                nodePort = nodeAddress.port();
                try {
                    node = new Socket(nodeHost, nodePort);
                    isSelecting = false;
                } catch (IOException e) {
                    tries++;
                    System.err.println("LB error: Could not connect to " + nodeHost + ":" + nodePort
                            + ", sending request to another node");
                }
            }
        }
        return node;
    }

    /**
     * Passes a request from the client to a node.
     * Returns the response from the node.
     *
     * @param client                 socket to the client
     * @param clientIn               input stream the client's socket
     * @return                       node's response as a string
     * @throws IOException           if there is an error trying to read or write data
     */
    private String passData(Socket client, BufferedReader clientIn) throws IOException {
        char[] data = new char[MAX_MSG_LEN];
        int data_length = 0;
        try (Socket node = selectNode()) {
            if (node != null) {
                BufferedWriter clientOut = new BufferedWriter(
                        new OutputStreamWriter(client.getOutputStream()));
                BufferedReader nodeIn = new BufferedReader(
                        new InputStreamReader(node.getInputStream()));
                BufferedWriter nodeOut = new BufferedWriter(
                        new OutputStreamWriter(node.getOutputStream()));
                clientIn.reset();
                data_length = clientIn.read(data, 0, MAX_MSG_LEN);
                if (data_length > 0) {
                    nodeOut.write(data, 0, data_length);
                    nodeOut.flush();
                    if (verbose) System.out.println("LB: Sent request to " + nodeHost + ":" + nodePort);
                }
                data_length = nodeIn.read(data, 0, MAX_MSG_LEN);
                if (data_length > 0) {
                    clientOut.write(data, 0, data_length);
                    clientOut.flush();
                }
            }
        }
        return new String(data, 0, data_length);
    }

    /**
     * Sends an alive status.
     *
     * @param socket        socket to the client
     * @throws IOException if there is an error trying to write data
     */
    private void sendStatus(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.println("LB");
        out.println("LBALIVE");
        out.flush();
    }

    /**
     * Sends an HTTP 307 html.
     *
     * @param socket       socket to the client
     * @param longURL      redirect URL
     * @throws IOException if there is an error trying to write data
     */
    private void sendRedirect(Socket socket, String longURL) throws IOException {
        File file = new File(ROOT, REDIRECT);
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream());
        sendHTML(file,"HTTP/1.1 307 Temporary Redirect", longURL, out, dataOut);
    }

    /**
     * Sends an HTTP 504 html.
     *
     * @param socket       socket to the client
     * @throws IOException if there is an error trying to write data
     */
    private void sendUnavailable(Socket socket) throws IOException {
        File file = new File(ROOT, SERVICE_UNAVAILABLE);
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream());
        sendHTML(file, "HTTP/1.1 503 Service Unavailable", out, dataOut);
    }

    /**
     * Returns true if the status code of the HTTP response is 201 Created,
     * otherwise false.
     *
     * @param response response HTML
     * @return         true if the status code is 201, otherwise false
     */
    private boolean isHttpSuccess(String response) {
        Pattern pattern = Pattern.compile("(?s)^HTTP\\S+\\s+201\\s+Created.*");
        Matcher matcher = pattern.matcher(response);
        return matcher.matches();
    }

    /**
     * Extracts the redirect URL from the HTTP response.
     * Returns the empty string if there is no redirect URL.
     *
     * @param response response html
     * @return         redirect URL
     */
    private String extractLongURL(String response) {
        Pattern pattern = Pattern.compile("(?s).*Location:\\s+(\\S+).*");
        Matcher matcher = pattern.matcher(response);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Handles distributing the client's request (sending/receiving data).
     * Caches redirects.
     */
    @Override
    public void handle(Socket client) {
        try {
            BufferedReader clientIn = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
            clientIn.mark(MAX_MSG_LEN);
            String firstLine = clientIn.readLine();

            if (firstLine != null ) {
                Pattern putPattern = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+\\S+$");
                Pattern getPattern = Pattern.compile("^GET\\s+/(\\S+)\\s+\\S+$");
                Matcher putMatcher = putPattern.matcher(firstLine);
                Matcher getMatcher = getPattern.matcher(firstLine);

                if (firstLine.equals("STATUS")) {
                    sendStatus(client);
                } else if (firstLine.equals("UPDATE")) {
                    loadBalancerData.updateNodeList(Utils.parseList(clientIn.readLine(), "NODE"));
                } else if (loadBalancerData.getNumNode() == 0) {
                    sendUnavailable(client);
                } else if (putMatcher.matches()) { // PUT request
                    String response = passData(client, clientIn);
                    if (isHttpSuccess(response)) {
                        String shortURL = putMatcher.group(1);
                        String longURL = putMatcher.group(2);
                        loadBalancerData.storeToCache(shortURL, longURL);
                        if (verbose) System.out.println("LB: Stored long to cache");
                    }
                } else if (getMatcher.matches() && !firstLine.contains("favicon.ico")) { // GET request
                    String shortURL = getMatcher.group(1);
                    if (loadBalancerData.isCached(shortURL)) { // Cached
                        String longURL = loadBalancerData.fetchFromCache(shortURL);
                        sendRedirect(client, longURL);
                        if (verbose) System.out.println("LB: Loaded long from cache");
                    } else { // Not cached
                        String redirect = passData(client, clientIn);
                        String longURL = extractLongURL(redirect);
                        if (!longURL.equals("")) {
                            loadBalancerData.storeToCache(shortURL, longURL);
                            if (verbose) System.out.println("LB: Stored long to cache");
                        }
                    }
                } else {
                    passData(client, clientIn);
                }
            }
            client.close();
        } catch (IOException e) {
            System.err.println("LB error: " + e.getMessage());
        }
    }

    /**
     * Cleans the cache at an interval.
     */
    public void clean() {
        while (true) {
            loadBalancerData.clean();
            try {
                Thread.sleep(CLEAN_TIME);
            } catch (InterruptedException e) {
                System.err.println("LB error: " + e.getMessage());
            }
        }
    }
}
