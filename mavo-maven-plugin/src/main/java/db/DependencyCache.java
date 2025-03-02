package db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import common.Constants;
import common.Utils;

public class DependencyCache {

	
	public static int createVulnerabilityCache(String originalPomPath)
	{
		int status = 0;

		String DB_DIRECTORY_VUL = originalPomPath;		
		String DB_URL_VUL = Utils.get_DB_URL(originalPomPath);
		
		try
		{
			File dbDir = new File(DB_DIRECTORY_VUL);
			if (!dbDir.exists()) {
				dbDir.mkdirs();
				System.out.println("Cache Directory created: " + dbDir.getAbsolutePath());
			}
			else {
				System.out.println("Cache Directory already exist");
			}
			
			Connection connection = DriverManager.getConnection(DB_URL_VUL);

			String createTableSQL = "CREATE TABLE IF NOT EXISTS "+Constants.VulnerabilityCache+" ("
					+ "key TEXT PRIMARY KEY,"
					+ "value TEXT NOT NULL)";
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createTableSQL);
			}catch (Exception e) {
				status = 1;
				System.out.println("Exception while creating Vulnerability Cache - "+e);
			}
			connection.close();	
			System.out.println("Vulnerability Cache table created successfully");
			
		}catch(Exception e) {
			System.out.println(e);
		}
		return status;
	}
	
	public static boolean isVulnerabilityKeyPresent(Connection connection, String key) throws SQLException {
		String checkSQL = "SELECT 1 FROM "+Constants.VulnerabilityCache+" WHERE key = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
			pstmt.setString(1, key);
			ResultSet rs = pstmt.executeQuery();
			return rs.next();  // Returns true if key exists
		}
	}
	
	public static void putVulnerabilityCache(Connection connection, String key, String value) throws SQLException {
		String upsertSQL = "INSERT INTO "+Constants.VulnerabilityCache+" (key, value) VALUES (?, ?) "
				+ "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
		try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
			pstmt.setString(1, key);
			pstmt.setString(2, value);  // Convert Map to JSON string
			pstmt.executeUpdate();
		}
	}

	public static String getVulnerabilityCache(Connection connection, String key) throws SQLException {
		String selectSQL = "SELECT value FROM "+Constants.VulnerabilityCache+" WHERE key = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
			pstmt.setString(1, key);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("value");  // Convert JSON string to Map
			}
		}
		return null; // Return null if not found
	}
	
	
	
	
	
	public static int createCombinationCache(String originalPomPath)
	{
		int status = 0;

		String DB_DIRECTORY_COMB = originalPomPath;		
		String DB_URL_COMB = Utils.get_DB_URL(originalPomPath);
		
		try
		{
			File dbDirComb = new File(DB_DIRECTORY_COMB);
			if (!dbDirComb.exists()) {
				dbDirComb.mkdirs();
				System.out.println("Cache Directory created: " + dbDirComb.getAbsolutePath());
			}
			else {
				System.out.println("Cache Directory already exist");
			}
			
			Connection connectionComb = DriverManager.getConnection(DB_URL_COMB);

			String createTableSQLComb = "CREATE TABLE IF NOT EXISTS "+Constants.CombinationsCache+" ("
					+ "key TEXT PRIMARY KEY,"
					+ "value TEXT NOT NULL)";
			try (Statement stmt = connectionComb.createStatement()) {
				stmt.execute(createTableSQLComb);
			}catch (Exception e) {
				status = 1;
				System.out.println("Exception while creating Combination Cache - "+e);
			}
			connectionComb.close();	
			System.out.println("Combinations Cache table created successfully");
		}catch(Exception e) {
			System.out.println(e);
		}
		return status;
	}

	public static boolean isCombinationKeyPresent(Connection connection, String key) throws SQLException {
		String checkSQL = "SELECT 1 FROM "+Constants.CombinationsCache+" WHERE key = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
			pstmt.setString(1, key);
			ResultSet rs = pstmt.executeQuery();
			return rs.next();  // Returns true if key exists
		}
	}
	
	public static void putCombinationCache(Connection connection, String key, String value) throws SQLException {
		String upsertSQL = "INSERT INTO "+Constants.CombinationsCache+" (key, value) VALUES (?, ?) "
				+ "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
		try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
			pstmt.setString(1, key);
			pstmt.setString(2, value);  // Convert Map to JSON string
			pstmt.executeUpdate();
		}
	}

	public static String getCombinationCache(Connection connection, String key) throws SQLException {
		String selectSQL = "SELECT value FROM "+Constants.CombinationsCache+" WHERE key = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
			pstmt.setString(1, key);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("value");  // Convert JSON string to Map
			}
		}
		return null; // Return null if not found
	}
	
}
