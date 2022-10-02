package Component;

import API.Link;
import API.Utils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node extends Link {
    static final File WEB_ROOT = new File("Resources");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String BAD_REQUEST= "400.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    static final String REDIRECT_RECORDED = "redirect_recorded.html";
    static final String REDIRECT = "redirect.html";
    static final String NOT_FOUND = "notfound.html";
    static final String DATABASE = "database.txt";
    static final int DBPort = 7777;
    private List<String> DBList = new ArrayList<String>();
    private int numDB = 0;
    // verbose mode
    static final boolean verbose = true;

    public Node(){
        super(8888);
    }

    @Override
    public void handle(Socket connect) {
        BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
        try {
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            out = new PrintWriter(connect.getOutputStream());
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            String input = in.readLine();

            if(verbose)System.out.println("first line: "+input);
            Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mput = pput.matcher(input);
            if(mput.matches()){
                if(verbose)System.out.println("PUT");
                String shortResource=mput.group(1);
                String longResource=mput.group(2);
                String httpVersion=mput.group(3);
                if(save(shortResource, longResource)){
                    File file = new File(WEB_ROOT, REDIRECT_RECORDED);
                    sendHTML(file,"HTTP/1.1 200 OK",out,dataOut);
                }else{
                    File file = new File(WEB_ROOT, BAD_REQUEST);
                    sendHTML(file,"HTTP/1.1 1 400 Bad Request",out,dataOut);
                }
            } else {
                if(verbose)System.out.println("GET");
                Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
                Matcher mget = pget.matcher(input);
                if(mget.matches() && !input.contains("favicon.ico")){
                    String method=mget.group(1);
                    String shortResource=mget.group(2);
                    String httpVersion=mget.group(3);

                    String longResource = find(shortResource);
                    if(longResource!=null){
                        File file = new File(WEB_ROOT, REDIRECT);
                        sendHTML(file,"HTTP/1.1 307 Temporary Redirect",longResource,out,dataOut);
                    } else {
                        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
                        sendHTML(file,"HTTP/1.1 404 File Not Found",out,dataOut);
                    }
                }else{
                    File file = new File(WEB_ROOT, DEFAULT_FILE);
                    sendHTML(file,"HTTP/1.1 200 OK",out,dataOut);
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
            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }
    }

    private String find(String shortURL){
        String content = shortURL;
        //String res = sendMsg(DBList.get(Utils.hashChecker(shortURL,this.numDB)),DBPort,"READ",content);
        return sendMsg("localhost",7777,"READ",content);
    }

    private boolean save(String shortURL, String longURL){

        String content = shortURL+"\n"+longURL;
        //String res = sendMsg(DBList.get(Utils.hashChecker(shortURL,this.numDB)),DBPort,"WRITE",content);
        String res = sendMsg("localhost",7777,"WRITE",content);

        return res.equals("Stored");
    }


    private static byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }
        return fileData;
    }

}
