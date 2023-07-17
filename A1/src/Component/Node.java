package Component;

import API.Link;
import API.Utils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A multithreaded URL shortener.
 * Distributes client requests to different database node and returns webpages.
 */
public class Node extends Link {
    static final File WEB_ROOT = new File("resources");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String BAD_REQUEST= "400.html";
    static final String SERVER_ERROR= "500.html";
    static final String REDIRECT_RECORDED = "redirect_recorded.html";
    static final String REDIRECT = "redirect.html";
    static final String CONFIG = "config";
    private ArrayList<Address> DBList = new ArrayList<Address>();

    /**
     * Creates a URL shortener object.
     */
    public Node(){
        super(8888);
        try {
            loadNodeConfig(CONFIG);
        } catch (IOException e) {
            System.err.println("Error loading DB Config : " + e.getMessage());
        }
    }

    /**
     * Handles the client's request (GET/PUT).
     *
     * @param connect client's socket
     */
    @Override
    public void handle(Socket connect) {
        BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();

            // message from monitor
            if(input.equals("STATUS")) {
                out.println("NODE");
                out.println("NODEALIVE");
                out.flush();
            // message from admin
            }else if(input.equals("UPDATE")){
                ArrayList<Address> newList= Utils.parseList(in.readLine(),"DB");
                replaceDBList(newList);
                out.println("NODE");
                out.println("Updated");
                out.flush();
            // message from client
            }else{
                // handle PUT requests
                Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+\\S+$");
                Matcher mput = pput.matcher(input);
                if(mput.matches()){
                    String shortResource=mput.group(1);
                    String longResource=mput.group(2);
                    if(Utils.isValidLong(longResource)&&Utils.isValidShort(shortResource)){
                        // saved
                        if(save(shortResource, longResource)){
                            File file = new File(WEB_ROOT, REDIRECT_RECORDED);
                            sendHTML(file,"HTTP/1.1 201 Created",out,dataOut);
                        // failed to save to the target database
                        }else{
                            File file = new File(WEB_ROOT, SERVER_ERROR);
                            sendHTML(file,"HTTP/1.1 500 Internal Server Error",out,dataOut);
                        }
                        // save to neighbours
                        backup(shortResource, longResource);
                    }else{
                        // invalid input
                        File file = new File(WEB_ROOT, BAD_REQUEST);
                        sendHTML(file,"HTTP/1.1 400 Bad Request",out,dataOut);
                    }
                // handle GET requests
                } else {
                    Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
                    Matcher mget = pget.matcher(input);
                    // ignore get webicon
                    if(mget.matches() && !input.contains("favicon.ico")){
                        String method=mget.group(1);
                        String shortResource=mget.group(2);
                        String httpVersion=mget.group(3);

                        String longResource = find(shortResource);
                        // found
                        if(longResource!=null&&longResource!=""){
                            File file = new File(WEB_ROOT, REDIRECT);
                            sendHTML(file,"HTTP/1.1 307 Temporary Redirect",longResource,out,dataOut);
                        // not found
                        } else {
                            File file = new File(WEB_ROOT, FILE_NOT_FOUND);
                            sendHTML(file,"HTTP/1.1 404 File Not Found",out,dataOut);
                        }
                    }else{
                        // default web page
                        File file = new File(WEB_ROOT, DEFAULT_FILE);
                        sendHTML(file,"HTTP/1.1 200 OK",out,dataOut);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Server error "+e.getMessage());
        } finally {
            try {
                in.close();
                out.close();
                connect.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }
        }
    }

    /**
     * Find corrresponding long URL from the designated database nodes.
     * @param shortURL short URL
     * @return long URL
     */
    private String find(String shortURL){
        String content = shortURL;
        ArrayList<Address> list = getDBList();

        int num = list.size();
        if(num>=1){
            int id = Utils.hashChecker(shortURL,num);
            Address address = list.get(id);
            String res = sendMsg(address.host(), address.port(), "READ",content);
            if(res=="" ||res.equals("null")){
                address = list.get(((id+1)>=num)? 0:id+1 );
                res = sendMsg(address.host(), address.port(), "READ",content);
                if ((res == "" ||res.equals("null")) && num>3){
                    address = list.get(((id-1)<0)? num-1:id-1);
                    res = sendMsg(address.host(), address.port(), "READ",content);
                }
            }
            if(res.equals("null"))return "";
            return res;
        }
        return "";

    }

    /**
     * Save a URL key-value pair to the designated database
     * @param shortURL
     * @param longURL
     * @return
     */
    private boolean save(String shortURL, String longURL){
        String content = shortURL+"\n"+longURL;
        ArrayList<Address> list = getDBList();
        Address address =  getDBList().get(Utils.hashChecker(shortURL,list.size()));
        String res = sendMsg(address.host(), address.port(), "WRITE",content);
        return res.contains("Stored");
    }

    /**
     * Save a key-value pair replica to designated database neighbours
     * @param shortURL
     * @param longURL
     */
    private void backup(String shortURL, String longURL){
        ArrayList<Address> list = getDBList();
        int num = list.size();
        if(num>1){
            String content = shortURL+"\n"+longURL;
            int id = Utils.hashChecker(shortURL,num);
            Address address = list.get(((id+1)>=num)? 0:id+1);
            sendMsg(address.host(), address.port(), "WRITE",content);
            if (num>=3) {
                address = list.get(((id-1)<0)? num-1:id-1);
                sendMsg(address.host(), address.port(), "WRITE",content);
            }
        }
    }

    /**
     * Update the internal list of active databases
     * @param newList latest list of active databases
     */
    private synchronized void replaceDBList(ArrayList<Address> newList){
        this.DBList = newList;
    }

    /**
     * Get the current list of active databases
     * @return current list of active databases
     */
    private synchronized ArrayList<Address> getDBList(){
        return this.DBList;
    }

    /**
     * Load the config file to get initial list of available databases
     * @param path the path of config file
     * @throws IOException
     */
    private void loadNodeConfig(String path) throws IOException {
        String content = Utils.loadConfig(path,2);
        Scanner scanner = new Scanner(content);
        scanner.useDelimiter("[:\n]"); // Split host:port pairs
        while (scanner.hasNext()) {
            String h = scanner.next().strip();
            int p = Integer.parseInt(scanner.next().strip());
            DBList.add(new Address("DB", h, p));
        }
    }

}
