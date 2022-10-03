package Component;

import java.util.ArrayList;

/**
 * Provides information for a load balancer.
 */
public class LoadBalancerInfo {

    ArrayList<ServerInfo> serverArray;
    int numServers, serverIndex;

    /**
     * Creates an object that manages information for a load balancer.
     *
     * @param serverArray an array of server information
     * @param numServers  the number of servers
     */
    public LoadBalancerInfo(ArrayList<ServerInfo> serverArray, int numServers) {
        this.serverArray = serverArray;
        this.numServers = numServers;
        this.serverIndex = 0;
    }

    /**
     * Returns the index of the server in the internal server array with the
     * given host name and port number.
     * Returns -1 if such server does not exist.
     *
     * @param host the host name of the server
     * @param port the port number of the server
     * @return     the index of the server
     */
    synchronized private int searchServer(String host, int port) {
        for (int i = 0; i < this.numServers; i++) {
            ServerInfo server = this.serverArray.get(i);
            if (server.getHost().equals(host) && server.getPort() == port) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds a server with the given host name and port number.
     * Does nothing if such server already exists.
     *
     * @param host the host name of the server
     * @param port the port number of the server
     */
    synchronized public void addServer(String host, int port) {
        if (searchServer(host, port) == -1) {
            this.serverArray.add(new ServerInfo(host, port));
            this.numServers++;
        }
    }

    /**
     * Removes the server with the given host name and port number.
     * Does nothing if such server does not exist.
     *
     * @param host the host name of the server
     * @param port the port number of the server
     */
    synchronized public void removeServer(String host, int port) {
        int removeIndex = searchServer(host, port);
        if (removeIndex != -1) {
            this.serverArray.remove(removeIndex);
            this.numServers--;
            if (this.numServers == 0) {
                this.serverIndex = 0;
            } else if (this.serverIndex > removeIndex) { // Adjust index for the removed server
                this.serverIndex--;
            }
        }
    }

    /**
     * Selects a server to distribute a request to.
     * Returns null if there are no servers available.
     * Load balancing method: round-robin
     *
     * @return the information of the selected server
     */
    synchronized public ServerInfo selectServer() {
        if (this.numServers == 0) {
            return null;
        }
        ServerInfo server = this.serverArray.get(this.serverIndex);
        this.serverIndex = (this.serverIndex + 1) % this.numServers;
        return server;
    }
}
