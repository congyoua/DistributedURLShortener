package Component;

import API.Link;
import API.Utils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A multithreaded admin tool for the URL shortener.
 */
public class Admin extends Link {

    static final File ROOT = new File("resources");
    static final String DEFAULT = "admin_index.html";
    static final String MONITOR = "admin_monitor.html";
    static final String CONFIG = "config";

    static final int PORT = 8800; // Port number

    private boolean isMonitoring = false;
    private final ArrayList<Address> knownServerList = new ArrayList<Address>();
    private final ArrayList<Address> activeLBList = new ArrayList<Address>();
    private final ArrayList<Address> activeDBList = new ArrayList<Address>();
    private final ArrayList<Address> activeNodeList = new ArrayList<Address>();

    /**
     * Creates an admin tool object
     */
    public Admin() {
        super(PORT);
        try {
            loadConfig();
        } catch (IOException e) {
            System.err.println("Error loading DB Config : " + e.getMessage());
        }
    }

    /**
     * A Runnable task to be used with Executor.
     * Monitors and manages the health of the system.
     */
    private void monitor() {
        ExecutorService exe = Executors.newFixedThreadPool(8);
        while (isMonitoring) {
            for (Address address : knownServerList) {
                exe.execute(new Runnable(){
                    @Override
                    public void run() {
                        healthCheck(address);
                    }
                });
            }
            recoverProcesses();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Send a status check code to one node and update the active list accordingly
     *
     * @param address address of a node
     */
    private void healthCheck(Address address) {
    	// Send code "STATUS" to the node
        String res = sendMsg(address.host(), address.port(), "STATUS","");
        // If received a proper response, the active list will be updated, otherwise removed from the list
        if (res != null && (res.equals("LBALIVE") || res.equals("DBALIVE") || res.equals("NODEALIVE"))) {
            updateList(address, "update");
        } else {
            updateList(address, "remove");
        }
    }
    
    /**
     *  Update the active list according to the address type and command received
     *
     * @param address address of a node
     * 		  com "update" or "remove" a node from the active list
     */
    private synchronized void updateList(Address address, String com) {
        if (address.type().equals("NODE")) {
            if (!activeNodeList.contains(address) && com.equals("update")) {
                activeNodeList.add(address);
                notifyLB();
            } else if (activeNodeList.contains(address) && com.equals("remove")) {
                activeNodeList.remove(address);
                notifyLB();
            }
        } else if (address.type().equals("DB")){
            if (!activeDBList.contains(address) && com.equals("update")) {
                activeDBList.add(address);
                notifyNode();
            } else if (activeDBList.contains(address) && com.equals("remove")) {
                activeDBList.remove(address);
                notifyNode();
            }
        } else if (address.type().equals("LB")){
            if (!activeLBList.contains(address) && com.equals("update")) {
                activeLBList.add(address);
            } else if (activeLBList.contains(address) && com.equals("remove")) {
                activeLBList.remove(address);
            }
        }
    }
    
    /**
     *	Send the new active database list to all the active coordinator
     */
    private synchronized void notifyNode() {
    	String addressList = addressListToString(activeDBList);
        String res = null;
        for (Address address : activeNodeList) {
            res = sendMsg(address.host(), address.port(), "UPDATE", addressList);
            if (res == null) sendMsg(address.host(), address.port(), "UPDATE", addressList);
        }
    }
    
    /**
     *	Send the new active coordinator list to all the active load balancer
     */
    private synchronized void notifyLB() {
    	String addressList = addressListToString(activeNodeList);
        String res = null;
        for (Address address : activeLBList) {
            res = sendMsg(address.host(), address.port(), "UPDATE", addressList);
            if (res == null) sendMsg(address.host(), address.port(), "UPDATE", addressList);
        }
    }

    /**
     * Attempts to recover a process.
     *
     * @param address address of the process
     */
    private void launchProcess(Address address) {
        System.out.println("Recovering " + address.type() + " on " + address.host() + ":" + address.port());
        String currPath = System.getProperty("user.dir");
        String classPath = currPath + "/out/production/A1:" + currPath + "/src/Database/sqlite-jdbc-3.39.3.0.jar";

        String sshCmd = "cd " + currPath + "; nohup java -cp " + classPath;
        if (address.type().equals("LB")) {
            sshCmd += " Component.LaunchLoadBalancer";
        } else if (address.type().equals("NODE")) {
            sshCmd += " Component.LaunchNode";
        } else {
            sshCmd += " Component.LaunchDatabase";
        }

        String[] cmdArray = {"ssh", address.host(), sshCmd};
        try {
            Runtime.getRuntime().exec(cmdArray);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Attempts to recover unresponsive processes.
     */
    private synchronized void recoverProcesses() {
        for (Address address : knownServerList) {
            if (!activeLBList.contains(address)
                    && !activeNodeList.contains(address)
                    && !activeDBList.contains(address)) {
                launchProcess(address);
            }
        }
    }

    /**
     * Handles connections to the web server.
     *
     * @param socket socket to the client
     */
    public void handle(Socket socket) {
        System.out.println("Admin: Web connection received");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream());
             BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream()))
        {
            String input = in.readLine();
            Pattern startPattern = Pattern.compile("^GET\\s+/start\\s+\\S+$");
            Pattern stopPattern = Pattern.compile("^GET\\s+/stop\\s+\\S+$");
            Pattern refreshPattern = Pattern.compile("^GET\\s+/refresh\\s+\\S+$");
            Pattern addressPattern = Pattern.compile("^PUT\\s+/\\?code=(\\S+)&ip=(\\S+)&type=(\\S+)\\s+\\S+$");
            Matcher addressGet = addressPattern.matcher(input);
            if (startPattern.matcher(input).matches()) {
                if (!isMonitoring) {
                    isMonitoring = true;
                    executor.execute(this::monitor);
                }
                File file = new File(ROOT, MONITOR);
                sendHTML(file, "HTTP/1.1 200 OK", out, dataOut);
            } else if (stopPattern.matcher(input).matches()) {
                isMonitoring = false;
                File file = new File(ROOT, DEFAULT);
                sendHTML(file, "HTTP/1.1 200 OK", out, dataOut);
            } else if (refreshPattern.matcher(input).matches()) {
                sendDiv(out, dataOut, knownServerList, activeLBList, activeDBList, activeNodeList);
            } else if (addressGet.matches()) {
                String message = scale(addressGet.group(1), addressGet.group(2), addressGet.group(3));
                System.out.println(message);
            } else {
                File file = new File(ROOT, DEFAULT);
                sendHTML(file, "HTTP/1.1 200 OK", out, dataOut);
            }
        } catch (IOException e) {
            System.out.println("Server error " + e.getMessage());
        }
    }
    
    /**
     *	Add or remove a node from the active list and server list u
     */
    private synchronized String scale(String code, String ip, String type) {
    	Pattern ipPattern = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    	if (!ipPattern.matcher(ip).matches() && !ip.equals("localhost")) return "Invalid ip address";

    	Address address = null;
    	int port;
    	if (type.equals("DB")) {
    		port = 7777;
			address = new Address("DB", ip, port);
		} else if (type.equals("Node")){
			port = 8888;
			address = new Address("NODE", ip, port);
		} else if (type.equals("LB")){
			port=8080;
            address = new Address("LB", ip, port);
        } else {
        	return "Invalid type";
        }
    	launchProcess(address);
    	String res = sendMsg(ip, port, "STATUS","");
    	
    	if (code.equals("add") && res != null && !res.equals("")) {
    		if (knownServerList.contains(address)) return "Already exists";
            knownServerList.add(address);
            updateList(address, "update");
            return "Added";
        } else if (code.equals("add") && (res == null || res.equals(""))){
        	return "Not running";
        } else if (code.equals("remove")){
        	knownServerList.remove(address);
        	updateList(address, "remove");
    		return "Removed";
        }
    	return "Invalid command";
    }
    
    /**
     *	Parse the active list to string for sending through socket
     */
    private String addressListToString(ArrayList<Address> addressList) {
    	String addressStringList = "";
    	for (Address address : addressList) {
    		addressStringList = addressStringList+address.host()+"/"+address.port()+",";
    	}
    	if (addressStringList.isEmpty()) return "";
    	return addressStringList.substring(0, addressStringList.length() - 1);
    }
    
    /**
     *	Load all the nodes to the big server list and each active lists
     *	Assumed they are all alive at the beginning
     */
    private void loadConfig() throws IOException {
        String h;
        int p;
        String lbContent = Utils.loadConfig(CONFIG,0);
        String nodeContent = Utils.loadConfig(CONFIG,1);
        String dbContent = Utils.loadConfig(CONFIG,2);

        Scanner scanner = new Scanner(lbContent);
        scanner.useDelimiter("[:\n]"); // Split host:port pairs
        while (scanner.hasNext()) {
            h = scanner.next().strip();
            p = Integer.parseInt(scanner.next().strip());
            knownServerList.add(new Address("LB", h, p));
            activeLBList.add(new Address("LB", h, p));
        }

        scanner = new Scanner(nodeContent);
        scanner.useDelimiter("[:\n]"); // Split host:port pairs
        while (scanner.hasNext()) {
            h = scanner.next().strip();
            p = Integer.parseInt(scanner.next().strip());
            knownServerList.add(new Address("NODE", h, p));
            activeNodeList.add(new Address("NODE", h, p));
        }

        scanner = new Scanner(dbContent);
        scanner.useDelimiter("[:\n]");
        while (scanner.hasNext()) {
            h = scanner.next().strip();
            p = Integer.parseInt(scanner.next().strip());
            knownServerList.add(new Address("DB", h, p));
            activeDBList.add(new Address("DB", h, p));
        }
    }
}
