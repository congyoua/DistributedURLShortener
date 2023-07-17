package API;

import Component.Address;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An abstract api class with multithreaded socket connection support
 * and useful functions & apis for servers and clients
 */
public abstract class Link {

	protected ServerSocket serverSocket;
	protected ExecutorService executor;
	public int port;

	public static final boolean verbose = false;

	/**
	 * Server initialization
	 * @param port
	 */
	public Link(int port) {
		this.port = port;
		this.executor = Executors.newFixedThreadPool(8);
	}

	/**
	 * Start the server
	 */
	public void start() {
		try {
			this.serverSocket = new ServerSocket(this.port);
			if(verbose)System.out.println("Server started at " + this.port);

			while (true) {
				this.executor.execute(handler(this.serverSocket.accept()));
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	/**
	 * Multithreaded handler
	 * @param socket
	 * @return
	 */
	public Runnable handler(Socket socket){
		return new Runnable(){
			@Override
			public void run() {
				handle(socket);
			}
		};
	}

	/**
	 * Main handle function
	 * @param socket client's socket
	 */
	public void handle(Socket socket){
		try {
			if(verbose)System.out.println("Receive connection");
			socket.close();
			if(verbose)System.out.println("Socket closed");
		} catch (IOException e) {
			System.err.println("Read write error : " + e.getMessage());
		}
	}

	/**
	 * Client API, send a message to specific socket
	 * @return Server's response
	 */
	public static String sendMsg(Socket socket, String code, String content) {
		String res = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

			out.println(code+"\n"+content);
			if(verbose)System.out.println("Sent: "+code);
			code = in.readLine();
			res = in.readLine();
			if(verbose)System.out.println("Received Code: "+code);
			if(verbose)System.out.println("Received String: "+res);
			in.close();
			out.close();
		} catch (IOException e) {
			System.err.println("Socket error : " + e.getMessage());
		}
		return res;
	}

	/**
	 * Client API, send a message to specific socket using ip/port
	 * @return Server's response
	 */
	public static String sendMsg(String ip, int port, String code, String content) {
		String res = "";
		try {
			Socket clientSocket = new Socket(ip, port);

			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

			out.println(code+"\n"+content);
			if(verbose)System.out.println("Sent: "+code);
			code = in.readLine();
			res = in.readLine();
			if(verbose)System.out.println("Received Code: "+code);
			if(verbose)System.out.println("Received String: "+res);

			in.close();
			out.close();
			return res;
		} catch (IOException e) {
			System.out.println("Cannot Reach out to: "+ip+":"+port);
		}
		return res;
	}

	/**
	 * Client API, send a static webpage
	 */
	public static void sendPage(Socket connect, File file) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			PrintWriter out = new PrintWriter(connect.getOutputStream(), true);
			BufferedOutputStream dataOut = new BufferedOutputStream(connect.getOutputStream());

			sendHTML(file,"HTTP/1.1 200 OK",out,dataOut);

			in.close();
			out.close();
			connect.close(); // we close socket connection
		} catch (IOException e) {
			System.err.println("Socket error : " + e.getMessage());
		}
	}

	/**
	 * Client API, send HTML file with different status
	 */
	public static void sendHTML(File file, String status, PrintWriter out, BufferedOutputStream dataOut) throws IOException {

		int fileLength = (int) file.length();
		String contentMimeType = "text/html";
		//read content to return to client
		byte[] fileData = Utils.readFileData(file, fileLength);

		out.println(status);
		out.println("Server: Java HTTP Server/Shortener : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + contentMimeType);
		out.println("Content-length: " + fileLength);
		out.println();
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
	}

	/**
	 * Client API, send HTML file for redirect
	 */
	public static void sendHTML(File file, String status, String redirect, PrintWriter out, BufferedOutputStream dataOut) throws IOException {

		int fileLength = (int) file.length();
		String contentMimeType = "text/html";
		//read content to return to client
		byte[] fileData = Utils.readFileData(file, fileLength);

		out.println(status);
		out.println("Location: " + redirect);
		out.println("Server: Java HTTP Server/Shortener : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + contentMimeType);
		out.println("Content-length: " + fileLength);
		out.println();
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
	}

	/**
	 * Client API, send latest info to monitoring page
	 */
	public static void sendDiv(PrintWriter out, BufferedOutputStream dataOut, ArrayList<Address> AllList, ArrayList<Address> LBList, ArrayList<Address> DBList, ArrayList<Address> NodeList) throws IOException {
		StringWriter str = new StringWriter();
		PrintWriter writer = new PrintWriter(str);
		writer.println("<h4>Servers:</h4>");
		for(Address add:AllList){
			if(add.type().equals("LB"))writer.println("<pre>"+add.host()+":"+add.port()+"    Type: Load Balancer"+"    Status: "+(LBList.contains(add)?"Alive":"No Response")+"</pre>");
			if(add.type().equals("NODE"))writer.println("<pre>"+add.host()+":"+add.port()+"    Type: URL Shortener"+"    Status: "+(NodeList.contains(add)?"Alive":"No Response")+"</pre>");
			if(add.type().equals("DB"))writer.println("<pre>"+add.host()+":"+add.port()+"    Type: Database"+"    Status: "+(DBList.contains(add)?"Alive":"No Response")+"</pre>");
		}

		String results = str.toString();
		int length = results.length();
		byte[] data = results.getBytes();

		out.println("HTTP/1.1 200 OK");
		out.println("Server: Java HTTP Server/Shortener : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: text/html");
		out.println("Content-length: " + length);
		out.println();
		out.flush();

		dataOut.write(data,0,length);
		dataOut.flush();
	}


	/**
	 * Shut down the server
	 */
	public void shutdown() {
		try {
			this.serverSocket.close();
			System.out.println("Server Closed");
		} catch (IOException e) {
			System.err.println("Socket fail to shutdown : " + e.getMessage());
		}
	}
}
