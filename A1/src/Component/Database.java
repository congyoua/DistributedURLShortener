package Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import API.Link;

/**
 * A multithreaded database for storing urls
 */
public class Database extends Link {
	
	public Database() {
		super(7777);
	}
	
	/**
     * Connect to the local database file
     * 
     * If the file does not exist, a new one will be created.
     * If the file is corrupted, it will be removed and create a new one.
     */
	private Connection connect() {
		Connection c = null;
		try {
			// Connect to the existing database or create a new database
			c = DriverManager.getConnection("jdbc:sqlite:/virtual/" + System.getProperty("user.name") +"/url.db");
			if(verbose)System.out.println("Connected to database");
		} catch (Exception e) {
			// Remove the corrupted database file
			File DB = new File("/virtual/" + System.getProperty("user.name") +"/url.db"); 
			DB.delete();
			try {
				// Create a new database file
				c = DriverManager.getConnection("jdbc:sqlite:/virtual/" + System.getProperty("user.name") +"/url.db");
				// Set up the new table
				initialize(c);
				if(verbose)System.out.println("Connected to database");
			} catch (Exception ex) {
				System.out.println("Database connect error: " + ex);
				System.exit(1);
			}
		}
		return c;
	}
	
	/**
     * Set up a url table for the new database file
     *
     */
	public void initialize(Connection c) {
		try {
			Statement table = null;
			table = c.createStatement();
			// SQL query send to database
			String sql = "CREATE TABLE IF NOT EXISTS URL " +
                    "(shortURL TEXT PRIMARY KEY NOT NULL," +
                    " longURL TEXT NOT NULL)"; 
			table.executeUpdate(sql);
			table.close();
			c.close();
		} catch (Exception e) {
			System.out.println("Create table error: " + e);
		}
		System.out.println("Table created");
	}

	
	/**
     * Handles upcoming database request for storing and getting URL
     */
	@Override
	public void handle(Socket socket){
		PrintWriter out = null; 
		BufferedReader in = null;
		try {
			if(verbose)System.out.println("Receive connection");
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			// Read the code and the short URL from the client
			String code = in.readLine();
			String shortURL = in.readLine();
			String longURL = "";
			
			out.println("DB");
			// Perform different operations according to code type
			switch (code) {
				// Get the long URL from the database with short URL
				case "READ":
					out.println(read(shortURL));
					break;
				// Store the short and long URL pair to the databse
				case "WRITE":
					longURL = in.readLine();
					write(shortURL, longURL);
					out.println("Stored");
					break;
				// Receive status check from the admin tool
				case "STATUS":
					out.println("DBALIVE");
					break;
				// Invalid code received from coordinator
				default:
					if(verbose)System.out.println("Invalid code");
			}

		} catch (IOException e) {
			System.err.println("Error : " + e.getMessage());
		} finally {
			try {
				in.close();
				out.close();
				socket.close();
			} catch (IOException e) {
				System.err.println("Error: " + e.getMessage());
			}

			if(verbose)System.out.println("Socket closed");
		}
	}
	
	/**
     * Store the short and long URL pair to the database
     *
     * @param shortURL short URL
     * 		  longURL long URL
     */
	public void write(String shortURL, String longURL) {
		Connection c = null;
		try {
			// Connect to the database
			c = connect();
			String sql = """
				 	pragma journal_mode = WAL;
					pragma synchronous = normal;
				""";
			Statement stmt  = c.createStatement();
			stmt.executeUpdate(sql);
			
			// This query will update the old one if it exists or insert a new one
			String insertSQL = "REPLACE INTO url(shortURL,longURL) "
					+ "VALUES (\""+shortURL+"\",\""+longURL+"\");";
			PreparedStatement pstmt = c.prepareStatement(insertSQL);
			pstmt.execute();
			if(verbose)System.out.println("Insert success");
		} catch (SQLException e) {
			System.out.println("Database insert error: " + e);
		} finally {
			try {
        		if (c != null) c.close();
				if(verbose)System.out.println("New URL stored");
    		} catch (SQLException ex) {
        		System.out.println(ex.getMessage());
    		}
		}
	}
	
	/**
     * Retrieve the long URL using short URL from the database
     *
     * @param shortURL short URL
     */
	public String read(String shortURL) {
		Connection c = null;
		String sql = "SELECT * FROM url WHERE shortURL = ?";
		String longURL = "";
		try {
			c = connect();
			// Prepare SQL select query
			PreparedStatement pstmt = c.prepareStatement(sql);
			pstmt.setString(1, shortURL);
			ResultSet rs = pstmt.executeQuery();
			// Get the long URL from the query result
			longURL = rs.getString("longURL");
		} catch (SQLException e) {
			System.out.println("Database read error: " + e);
		} finally {
			try {
        		if (c != null) c.close();
				if(verbose)System.out.println("URL is "+longURL);
    		} catch (SQLException ex) {
        		System.out.println(ex.getMessage());
    		}
		}
		return longURL;
	}

}

