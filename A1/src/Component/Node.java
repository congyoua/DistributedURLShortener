package Component;

import API.Link;

import java.io.File;

public class Node extends Link {
    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    static final String REDIRECT_RECORDED = "redirect_recorded.html";
    static final String REDIRECT = "redirect.html";
    static final String NOT_FOUND = "notfound.html";
    static final String DATABASE = "database.txt";


    public Node(){
        super(2222);
        System.out.println("Hi! I'm a Node! Port:"+this.port);
    }


    
    

}
