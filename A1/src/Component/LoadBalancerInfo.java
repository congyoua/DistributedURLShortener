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
        if (searchServer(host, port) != -1) {
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
        int index = searchServer(host, port);
        if (index != -1) {
            this.serverArray.remove(index);
            if (this.serverIndex >= index) { // Adjust index for the removed server
                this.serverIndex--;
            }
        }
    }

    /**
     * Chooses a server to distribute an incoming request to.
     * Load balancing method: round-robin
     *
     * @return the information of the chosen server
     */
    synchronized public ServerInfo chooseServer() {
        ServerInfo server = serverArray.get(serverIndex);
        serverIndex = (serverIndex + 1) % numServers;
        return server;
    }
}
