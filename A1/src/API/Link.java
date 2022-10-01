package API;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Link {
	
	protected ServerSocket serverSocket;
	protected ExecutorService executor;
	public int port;
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	static final String REDIRECT_RECORDED = "redirect_recorded.html";
	static final String REDIRECT = "redirect.html";
	static final String NOT_FOUND = "notfound.html";
	static final String DATABASE = "database.txt";
		
	public Link(int port) {
		this.port = port;
		this.executor = Executors.newCachedThreadPool();
	}
	
	// Start the server
	public void start() {
		try {
			this.serverSocket = new ServerSocket(this.port);
			System.out.println("Server started at " + this.port);
			
			while (true) {
				this.executor.execute(handler(this.serverSocket.accept()));
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}
	
	public Runnable handler(Socket socket){
		return new Runnable(){
			@Override
			public void run() {	
				try {
					System.out.println("Receive connection");
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					
					String message = in.readLine();
					// Store everything in a string list for now
					String[] command = message.split(" ");
					System.out.println(command[0]);
					
					switch (command[0]) {
						case "SHUTDOWN":
							shutdown();
							break;
						default:
							System.out.println("Others");
					}
					
					out.println("Complete");
					
					in.close();
			        out.close();
			        socket.close();
			        System.out.println("Socket closed");
				} catch (IOException e) {
					System.err.println("Read write error : " + e.getMessage());
				}
			}
		};
	}
	
	public static void sendToAnother(Socket socket, String code, String content) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			
			out.println(code+" "+content);
			String res = in.readLine();
			System.out.println("Response: "+res);
			in.close();
	        out.close();
		} catch (IOException e) {
			System.err.println("Socket error : " + e.getMessage());
		}
	}
	
	
	// Send something to another socket and receive response
	public static void sendToAnother(String ip, int port, String code, String content) {
		try {
			Socket clientSocket = new Socket(ip, port);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			
			out.println(code+" "+content);
			String res = in.readLine();
			System.out.println("Response: "+res);
			in.close();
	        out.close();
		} catch (IOException e) {
			System.err.println("Socket error : " + e.getMessage());
		}
		
	}
	
	public static void sendFile(Socket connect, String name) {
		try {			
			BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			PrintWriter out = new PrintWriter(connect.getOutputStream(), true);
			BufferedOutputStream dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			File file = new File(name);
			int fileLength = (int) file.length();
			String contentMimeType = "text/html";
			//read content to return to client
			byte[] fileData = readFileData(file, fileLength);
				
			out.println("HTTP/1.1 200 OK");
			out.println("Server: Java HTTP Server/Shortner : 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: " + contentMimeType);
			out.println("Content-length: " + fileLength);
			out.println(); 
			out.flush(); 

			dataOut.write(fileData, 0, fileLength);
			dataOut.flush();
			
			in.close();
			out.close();
			connect.close(); // we close socket connection
		} catch (IOException e) {
			System.err.println("Socket error : " + e.getMessage());
		}
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
	
	// Shutdown server
	public void shutdown() {
		try {
			this.serverSocket.close();
			System.out.println("Server Closed");
		} catch (IOException e) {
			System.err.println("Socket fail to shutdown : " + e.getMessage());
		}
	}
}
