package Database;

import java.io.BufferedReader;
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


public class Database extends Link {
	
	public Database() {
		super(7777);
		initialize();
	}
	
	private Connection connect() {
		Connection c = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:./src/Database/test.db");
			System.out.println("Connected to database");
		} catch (Exception e) {
			System.out.println("Database error: " + e);
		}
		return c;
	}
	
	public void initialize() {
		try {
			Statement table = null;
			Connection c = connect();
			table = c.createStatement();
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
	
	@Override
	public void handle(Socket socket){
		PrintWriter out = null; 
		BufferedReader in = null;
		try {
			System.out.println("Receive connection");
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			String code = in.readLine();
			String shortURL = in.readLine();
			String longURL = "";
			
			out.println("DB");
			switch (code) {
				case "READ":
					out.println(fetchURL(shortURL));
					break;
				case "WRITE":
					longURL = in.readLine();
					storeURL(shortURL, longURL);
					out.println("Stored");
					break;
				default:
					System.out.println("Invalid code");
			}

		} catch (IOException e) {
			System.err.println("Error : " + e.getMessage());
		} finally {
			try {
				in.close();
				out.close();
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Socket closed");
		}
	}
	
	public void storeURL(String shortURL, String longURL) {
		String sql = "INSERT INTO URL(shortURL,longURL) "
				+ "VALUES (\""+shortURL+"\",\""+longURL+"\");";
		try {
			Connection c = connect();
			PreparedStatement pstmt = c.prepareStatement(sql);
			pstmt.executeUpdate();
			pstmt.close();
			c.close();
		} catch (SQLException e) {
			System.out.println("Database insert error: " + e);
		}
		System.out.println("New URL stored");
	}
	
	public String fetchURL(String shortURL) {
		String sql = "SELECT * FROM URL WHERE shortURL = ?";
		String longURL = "";
		try {
			Connection c = connect();
			PreparedStatement pstmt = c.prepareStatement(sql);
			pstmt.setString(1, shortURL);
			ResultSet rs = pstmt.executeQuery();
			longURL = rs.getString("longURL");
			pstmt.close();
			c.close();
			System.out.println("URL is "+longURL);
		} catch (SQLException e) {
			System.out.println("Database read error: " + e);
		}
		return longURL;
	}

}

