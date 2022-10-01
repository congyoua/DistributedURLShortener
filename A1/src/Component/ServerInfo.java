package Component;

/**
 * Provides information for a server.
 */
public class ServerInfo {

    private String host;
    private int port;

    public ServerInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Returns the host name of the server.
     * @return the host name of the server
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Returns the port number of the server.
     * @return the port number of the server
     */
    public int getPort() {
        return this.port;
    }
}
