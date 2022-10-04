package Component;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Provides shared information for a load balancer.
 */
public class LoadBalancerInfo {

    ArrayList<NodeAddress> serverArray;
    int numServers, serverIndex;
    HashMap<String, CharArray> cache;

    /**
     * Creates an object that manages the shared information for a load
     * balancer.
     *
     * @param serverArray an array of server information
     * @param numServers  the number of servers
     */
    public LoadBalancerInfo(ArrayList<NodeAddress> serverArray, int numServers) {
        this.serverArray = serverArray;
        this.numServers = numServers;
        serverIndex = 0;
        cache = new HashMap<>();
    }

    /**
     * Returns the index of the server in the server array with the given host
     * name and port number.
     * Returns -1 if such server does not exist.
     *
     * @param host the host name of the server
     * @param port the port number of the server
     * @return     the index of the server
     */
    synchronized private int searchServer(String host, int port) {
        for (int i = 0; i < numServers; i++) {
            NodeAddress server = serverArray.get(i);
            if (server.host().equals(host) && server.port() == port) {
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
            serverArray.add(new NodeAddress(host, port));
            numServers++;
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
            serverArray.remove(removeIndex);
            numServers--;
            if (numServers == 0) {
                serverIndex = 0;
            } else if (serverIndex > removeIndex) { // Adjust index for the removed server
                serverIndex--;
            }
        }
    }

    /**
     * Selects a server to distribute a request to.
     * Returns null if there are no servers available.
     * Load balancing method: round-robin
     *
     * @return the socket address of the selected server
     */
    synchronized public NodeAddress selectServer() {
        if (numServers == 0) {
            return null;
        }
        NodeAddress server = serverArray.get(serverIndex);
        serverIndex = (serverIndex + 1) % numServers;
        return server;
    }

    /**
     * Returns whether the given short URL is cached.
     *
     * @param shortURL a short URL
     * @return         whether the short URL is cached
     */
    synchronized public boolean isCached(String shortURL) {
        return cache.containsKey(shortURL);
    }

    /**
     * Stores the given short URL with the given data.
     *
     * @param shortURL  a short URL
     * @param charArray the data to cache
     */
    synchronized public void storeToCache(String shortURL, CharArray charArray) {
        cache.put(shortURL, charArray);
    }

    /**
     * Returns the data corresponding to the given short URL.
     *
     * @param shortURL a short URL
     * @return         the cached data
     */
    synchronized public CharArray fetchFromCache(String shortURL) {
        return cache.get(shortURL);
    }
}
