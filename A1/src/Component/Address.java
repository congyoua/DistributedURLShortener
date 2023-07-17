package Component;

import java.io.Serializable;

/**
 * Provides infomation of an address
 */
public record Address(String type, String host, int port) implements Serializable {}