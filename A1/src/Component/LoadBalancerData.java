package Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides shared information for a load balancer.
 */
public class LoadBalancerData {

    static final int MAX_CACHE_SIZE = 50;
    static final int CACHE_EXPIRE_TIME = 60;

    private ArrayList<Address> nodeList;
    private int nodeIndex = 0;
    private final LinkedHashMap<String, String> longCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private final LinkedHashMap<String, Long> timeCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private final ReentrantLock nodeLock = new ReentrantLock();
    private final ReentrantLock cacheLock = new ReentrantLock();

    /**
     * Creates an object that manages the shared data for a load balancer.
     *
     * @param list list of node addresses
     */
    public LoadBalancerData(ArrayList<Address> list) {
        nodeList = list;
    }

    /**
     * Replaces the current node list.
     *
     * @param newNodeList list of node addresses
     */
    public void updateNodeList(ArrayList<Address> newNodeList) {
        nodeLock.lock();
        try {
            nodeList = newNodeList;
            nodeIndex = 0;
        } finally {
            nodeLock.unlock();
        }
    }

    /**
     * Returns the number of nodes in the node list.
     *
     * @return number of nodes
     */
    int getNumNode() {
        int size;
        nodeLock.lock();
        try {
            size = nodeList.size();
        } finally {
            nodeLock.unlock();
        }
        return size;
    }

    /**
     * Selects a node to distribute a request to.
     * Returns null if there are no nodes available.
     * Load balancing method: round-robin
     *
     * @return socket address of the selected node
     */
    public Address selectNode() {
        Address nodeAddress = null;
        nodeLock.lock();
        try {
            if (nodeList.size() != 0) {
                nodeAddress = nodeList.get(nodeIndex);
                nodeIndex = (nodeIndex + 1) % nodeList.size();
            }
        } finally {
            nodeLock.unlock();
        }
        return nodeAddress;
    }

    /**
     * Returns whether the given short URL is cached or not.
     *
     * @param shortURL short URL
     * @return         true if the short URL is cached
     */
    public boolean isCached(String shortURL) {
        boolean b;
        cacheLock.lock();
        try {
            b = longCache.containsKey(shortURL);
        } finally {
            cacheLock.unlock();
        }
        return b;
    }

    /**
     * Stores the given short URL with the given long URL.
     *
     * @param shortURL short URL
     * @param longURL  long URL
     */
    public void storeToCache(String shortURL, String longURL) {
        cacheLock.lock();
        try {
            longCache.put(shortURL, longURL);
            timeCache.put(shortURL, System.nanoTime());
        } finally {
            cacheLock.unlock();
        }
    }

    /**
     * Returns the long URL corresponding to the given short URL.
     *
     * @param shortURL short URL
     * @return         long URL
     */
    public String fetchFromCache(String shortURL) {
        String s;
        cacheLock.lock();
        try {
            s = longCache.get(shortURL);
        } finally {
            cacheLock.unlock();
        }
        return s;
    }

    /**
     * Removes expired entries in the cache.
     */
    public void clean() {
        cacheLock.lock();
        try {
            longCache.entrySet().removeIf(
                    e -> (System.nanoTime() - timeCache.get(e.getKey())) > CACHE_EXPIRE_TIME
            );
        } finally {
            cacheLock.unlock();
        }
    }
}
