package Component;

/**
 * Stores the socket address of a server.
 * @param host the host name
 * @param port the port number
 */
public record NodeAddress(String host, int port) {}
