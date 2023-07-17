package API;

import Component.Address;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A set of function utilities
 */
public class Utils {

    /**
     * Main hash function to check responsibility
     * @param msg string to hash
     * @param num num of active databases
     * @return hashed number
     */
    public static int hashChecker(String msg, int num){
        return (Math.floorMod(msg.hashCode(),num));
    }

    public static String loadConfig(String path, int item) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));

        Pattern pget = Pattern.compile("^LoadBalancers:([\\S\\s]*)Nodes:([\\S\\s]*)Databases:([\\S\\s]*)$");
        Matcher mget = pget.matcher(content);
        if(mget.matches()){
            switch(item){
                case 0:
                    return mget.group(1).trim();
                case 1:
                    return mget.group(2).trim();
                case 2:
                    return mget.group(3).trim();
            }
        }
        return "";
    }

    /**
     * Read the file content as byte array
     * @param file
     * @param fileLength
     * @return file data byte array
     * @throws IOException
     */
    public static byte[] readFileData(File file, int fileLength) throws IOException {
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

    /**
     * Returns true if the short URL is valid, otherwise false.
     *
     * @return true if the short URL is valid, otherwise false
     */
    public static boolean isValidShort(String shortURL) {
        if (shortURL.length() > 100) {
            return false;
        }
        // Change the regex depending on what characters should be allowed
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9-_]+$");
        Matcher matcher = pattern.matcher(shortURL);
        return matcher.matches();
    }

    /**
     * Returns true if the long URL is valid, otherwise false
     *
     * @return true if the long URL is valid, otherwise false
     */
    public static boolean isValidLong(String longURL) {
        if (longURL.length() > 100) {
            return false;
        }
        // This regex is not perfect, but it should be good enough
        Pattern pattern = Pattern.compile("^https?://[a-zA-Z0-9-]+\\.\\S{2,}$");
        Matcher matcher = pattern.matcher(longURL.strip());

        return matcher.matches();
    }

    /**
     * Config file parser
     * @param addressString input string
     * @param type type of node
     * @return the addresses for given node type
     */
    public static ArrayList<Address> parseList(String addressString, String type) {
        ArrayList<Address> addressList = new ArrayList<Address>();
        if (addressString.isEmpty()) return addressList;
        String[] stringList = addressString.split(",");
        String[] addressInfo;
        for (String address : stringList) {
            addressInfo = address.split("/");
            addressList.add(new Address(type, addressInfo[0], Integer.parseInt(addressInfo[1])));
        }
        return addressList;
    }
}
