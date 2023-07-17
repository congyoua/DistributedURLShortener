package Component;

import API.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Performs the initial configuration and starts the load balancer.
 */
public class LaunchLoadBalancer {

    static final String FILE = "config"; // Default config file path

    /**
     * Parses a config file and returns the shared data for a load balancer.
     *
     * @return             shared data for a load balancer
     * @throws IOException if there is an error trying to read the file
     */
    private static LoadBalancerData loadConfig() throws IOException {
        ArrayList<Address> serverArray = new ArrayList<>();

        String nodes = Utils.loadConfig(FILE, 1);
        try (Scanner scanner = new Scanner(nodes)) {
            scanner.useDelimiter("[:\n]"); // Split host:port pairs
            while (scanner.hasNext()) {
                String h = scanner.next().strip();
                int p = Integer.parseInt(scanner.next().strip());
                serverArray.add(new Address("NODE", h, p));
            }
        }
        return new LoadBalancerData(serverArray);
    }

    public static void main(String[] args) {
        try {
            LoadBalancerData loadBalancerData = loadConfig();
            LoadBalancer loadBalancer = new LoadBalancer(loadBalancerData);
            System.out.println("Load Balancer started at port 8080");
            loadBalancer.start();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
