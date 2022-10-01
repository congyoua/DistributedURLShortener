package API;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class Link {
	
	ServerSocket serverSocket;
	public int port;
		
	public Link(int port) {
		this.port = port;
	}
	
	// Start the server
	public void start() {
		try {
			this.serverSocket = new ServerSocket(this.port);
			System.out.println("Server started at " + this.port);
			
			while (true) {
				new Handler(this.serverSocket.accept()).start();
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
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
	
	// Shutdown server
	public void shutdown() {
		try {
			this.serverSocket.close();
			System.out.println("Server Closed");
		} catch (IOException e) {
			System.err.println("Socket fail to shutdown : " + e.getMessage());
		}
	}
	
	// Thread class for handling income command
	class Handler extends Thread {
		private Socket client;
		private PrintWriter out;
		private BufferedReader in;

		public Handler(Socket socket)
	    {
	        this.client = socket;
	    }

		public void run() {
			try {
				System.out.println("Receive connection");
				out = new PrintWriter(this.client.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
				
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
		        this.client.close();
		        System.out.println("Socket closed");
			} catch (IOException e) {
				System.err.println("Read write error : " + e.getMessage());
			}
			
		}
		
	}
}
