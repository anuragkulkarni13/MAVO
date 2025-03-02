package db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;

import common.Constants;
import common.dto.DependencyDTO;
import common.dto.VulnerabilityDTO;

public class DependencyDB {

	public static String get_DB_URL(String originalPomPath)
	{
		String DB_DIRECTORY = originalPomPath;
		String DB_URL = "jdbc:sqlite:" + DB_DIRECTORY + "\\" + Constants.DB_NAME;
		return DB_URL;
	}

	public static int createDependencyTable(String originalPomPath)
	{
		int status = 0;
		
		String DB_DIRECTORY_DEP = originalPomPath;		
		String DB_URL_DEP = get_DB_URL(originalPomPath);

		try
		{
			File dbDir = new File(DB_DIRECTORY_DEP);
			if (!dbDir.exists()) {
				dbDir.mkdirs();
				System.out.println("Dependency Table Directory created: " + dbDir.getAbsolutePath());
			}
			else {
				System.out.println("Dependency Table Directory already exist");
			}

			Connection connection = DriverManager.getConnection(DB_URL_DEP);

			String createTableSQL = "CREATE TABLE IF NOT EXISTS "+Constants.DependencyTable+" ("
					+ "    groupId TEXT,"
					+ "    artifactId TEXT,"
					+ "    version TEXT,"
					+ "    resolvedVersion TEXT,"
					+ "    recommendedVersion TEXT,"
					+ "    module TEXT,"
					+ "    dependencyType TEXT,"
					+ "    parentDependency TEXT,"
					+ "    dependencyNature TEXT,"
					+ "    exclude BOOLEAN,"
					+ "    vulnerabilities TEXT,"
					+ "    afterResolvedVulnerabilities TEXT,"
					+ "    PRIMARY KEY (groupId, artifactId, version, module)"
					+ ");";
			try (Statement stmt = connection.createStatement()) {
				stmt.execute(createTableSQL);
			}catch (Exception e) {
				status = 1;
				System.out.println("Exception while creating Dependency Table - "+e);
			}
			connection.close();	

		}catch(Exception e) {
			System.out.println(e);
		}
		
		return status;
	}

	public static boolean isDependencyKeyPresent(Connection connection, String key) throws SQLException {
		String checkSQL = "SELECT 1 FROM "+Constants.DependencyTable+" WHERE key = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
			pstmt.setString(1, key);
			ResultSet rs = pstmt.executeQuery();
			return rs.next();  // Returns true if key exists
		}
	}
	
	public static void putDependencyTableEntry(Connection connection, String groupId, String artifactId, String version, String resolvedVersion, String recommendedVersion, 
			String module, String dependencyType, String parentDependency, String dependencyNature, Boolean exclude, String vulnerabilities, String afterResolvedVulnerabilities
			){
		String upsertSQL = "INSERT INTO "+Constants.DependencyTable+" ("
				+ "    groupId,"
				+ "    artifactId,"
				+ "    version,"
				+ "    resolvedVersion,"
				+ "    recommendedVersion,"
				+ "    module,"
				+ "    dependencyType,"
				+ "    parentDependency,"
				+ "    dependencyNature,"
				+ "    exclude,"
				+ "    vulnerabilities,"
				+ "    afterResolvedVulnerabilities"
				+ ")"
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
				+ "ON CONFLICT (groupId, artifactId, version, module) DO UPDATE SET "
				+ "    resolvedVersion = excluded.resolvedVersion,"
				+ "    recommendedVersion = excluded.recommendedVersion,"
				+ "    dependencyType = excluded.dependencyType,"
				+ "    parentDependency = excluded.parentDependency,"
				+ "    exclude = excluded.exclude,"
				+ "    vulnerabilities = excluded.vulnerabilities,"
				+ "    afterResolvedVulnerabilities = excluded.afterResolvedVulnerabilities;";
		try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
			pstmt.setString(1, groupId);
			pstmt.setString(2, artifactId);
			pstmt.setString(3, version);
			pstmt.setString(4, resolvedVersion);
			pstmt.setString(5, recommendedVersion);
			pstmt.setString(6, module);
			pstmt.setString(7, dependencyType);
			pstmt.setString(8, parentDependency);
			pstmt.setString(9, dependencyNature);
			pstmt.setBoolean(10, exclude);
			pstmt.setString(11, vulnerabilities);
			pstmt.setString(12, afterResolvedVulnerabilities);
			pstmt.executeUpdate();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void putDependenciesInDependencyTable(Connection connection, String pomPath, Map<String, List<Dependency>> segregatedDependencies, String moduleName) {

		try {
			
			for (Map.Entry<String, List<Dependency>> entry : segregatedDependencies.entrySet()) {
//			    System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue().size());
				
				String dependencyType = entry.getKey();
				List<Dependency> dependencyList = entry.getValue();
				for(Dependency dependency : dependencyList)
				{
					String groupId = dependency.getGroupId();
					String artifactId = dependency.getArtifactId();
					String version = dependency.getVersion() == null ? "NA" : dependency.getVersion();
					String resolvedVersion = resolveVersion(connection, groupId, artifactId, version);
//					System.out.println("resolvedVersion - "+resolvedVersion);
					String type = dependencyType;
					if(dependency.getType().equalsIgnoreCase(Constants.parentDependencytype))
					{
						type = dependency.getType();
					}
					DependencyDB.putDependencyTableEntry(connection, groupId, artifactId, version, resolvedVersion, resolvedVersion, moduleName, type, "", Constants.permanentDependencyNature, false, "", "");
				}
			}
			connection.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
		

	}
	
	public static void updateExcludeListInDependencyTable(Connection connection, String groupId, String artifactId)
	{
		String updateQuery = "update DependencyTable set exclude = ? where groupId = ? and artifactId = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
			pstmt.setBoolean(1, true);
			pstmt.setString(2, groupId);
			pstmt.setString(3, artifactId);
			pstmt.executeUpdate();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static List<DependencyDTO> getAllParentDependencies(Connection connection) {
		
		List<DependencyDTO> dependencyList = new ArrayList<>();
		String query = "SELECT groupId, artifactId, recommendedVersion FROM DependencyTable where "
				+ "dependencyType = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, Constants.pomDependencyType);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	String groupId = resultSet.getString("groupId");
	            	String artifactId = resultSet.getString("artifactId");
	            	String recommendedVersion = resultSet.getString("recommendedVersion");
	            	DependencyDTO dependencyDTO = new DependencyDTO();
	            	dependencyDTO.setGroupId(groupId);
	            	dependencyDTO.setArtifactId(artifactId);
	            	dependencyDTO.setVersion(recommendedVersion);
	            	
	            	dependencyList.add(dependencyDTO);
	            }
            }catch (Exception e) {
				System.out.println(e);
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependencyList;
	}

	public static void updateRecommendedVersionForParentInDependencyTable(Connection connection, String groupId, String artifactId, String oldRecommendedVersion, 
			String newRecommendedVersion)
	{
        String updateQuery = "update DependencyTable set recommendedVersion = ? where groupId = ? and artifactId = ? "
        		+ "and recommendedVersion = ? and dependencyType = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
			pstmt.setString(1, newRecommendedVersion);
			pstmt.setString(2, groupId);
			pstmt.setString(3, artifactId);
			pstmt.setString(4, oldRecommendedVersion);
			pstmt.setString(5, Constants.pomDependencyType);
			pstmt.executeUpdate();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static void updateParentDependentDependencies(Connection connection, String groupId, String newRecommendedVersion)
	{
        String updateQuery = "update DependencyTable set recommendedVersion = ? where groupId = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
			pstmt.setString(1, newRecommendedVersion);
			pstmt.setString(2, groupId);
			pstmt.executeUpdate();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static List<DependencyDTO> getModulewiseDependenciesExceptParent(Connection connection, String moduleName)
	{
		List<DependencyDTO> dependencyList = new ArrayList<>();
		String query = "select * from DependencyTable where "
				+ "groupId not in (select groupId from DependencyTable where dependencyType in ('pom')) "
				+ "and dependencyType = 'normal' and module = ? and exclude = false;";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, moduleName);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	String groupId = resultSet.getString("groupId");
	            	String artifactId = resultSet.getString("artifactId");
	            	String recommendedVersion = resultSet.getString("recommendedVersion");
	            	DependencyDTO dependencyDTO = new DependencyDTO();
	            	dependencyDTO.setGroupId(groupId);
	            	dependencyDTO.setArtifactId(artifactId);
	            	dependencyDTO.setVersion(recommendedVersion);
	            	
	            	dependencyList.add(dependencyDTO);
	            }
            }catch (Exception e) {
				System.out.println(e);
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependencyList;
	}
	
	public static void updateRecommendedVersionToDependencyTable(Connection connection, String groupId, String artifactId, String recommendedVersion) {
        String updateQuery = "update DependencyTable set recommendedVersion = ? where groupId = ? and artifactId = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
			pstmt.setString(1, recommendedVersion);
			pstmt.setString(2, groupId);
			pstmt.setString(3, artifactId);
			pstmt.executeUpdate();
		}
		catch (Exception e) {
			System.out.println(e);
		}
//		try {
//			DependencyDB.putDependencyTableEntry(connection, groupId, artifactId, "NA", "NA", recommendedVersion, moduleName, Constants.normalDependencyType, "", Constants.temporaryDependencyType, false, vulString, "");
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
	}
	
	public static void updateVulnerabilityInDependencyTable(Connection connection, String groupId, String artifactId, String version, String vulString)
	{	
        String updateQuery = "update DependencyTable set vulnerabilities = ? where groupId = ? and artifactId = ? and resolvedVersion = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
			pstmt.setString(1, vulString);
			pstmt.setString(2, groupId);
			pstmt.setString(3, artifactId);
			pstmt.setString(4, version);
			pstmt.executeUpdate();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static boolean isDependencyPresent(Connection connection, String groupId, String artifactId) throws SQLException {
		String checkSQL = "SELECT 1 FROM "+Constants.DependencyTable+" WHERE groupId = ? and artifactId = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
			pstmt.setString(1, groupId);
			pstmt.setString(2, artifactId);
			ResultSet rs = pstmt.executeQuery();
			return rs.next();  // Returns true if key exists
		}
	}
	
	public static List<String> getGropuIds(Connection connection) throws SQLException {
		
		List<String> groupIds = new ArrayList<>();
		
		String checkSQL = "select distinct(groupId) from DependencyTable";
		try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
			ResultSet rs = pstmt.executeQuery();
			while(rs.next())
			{
				groupIds.add(rs.getString("groupId"));
			}
		}
		
		return groupIds;
	}
	
	public static List<DependencyDTO> getGropuIdVersions(Connection connection, String groupId) throws SQLException {
		
		List<DependencyDTO> dependencyList = new ArrayList<>();
		
		String checkSQL = "select artifactId, recommendedVersion from DependencyTable where groupId = '"+groupId+"'";
		try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
			ResultSet rs = pstmt.executeQuery();
			while(rs.next())
			{
				DependencyDTO dependency = new DependencyDTO();
				dependency.setGroupId(groupId);
				dependency.setArtifactId(rs.getString("artifactId"));
				dependency.setVersion(rs.getString("recommendedVersion"));
				dependencyList.add(dependency);
			}
		}
		
		return dependencyList;
	}
	
	public static void updateGroupIdVersions(Connection connection, String groupId, String version)
	{	
        String updateQuery = "update DependencyTable set recommendedVersion = ? where groupId = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
			pstmt.setString(1, version);
			pstmt.setString(2, groupId);
			pstmt.executeUpdate();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	public static List<DependencyDTO> getModulewiseTemporaryDependencies(Connection connection, String moduleName) {
		List<DependencyDTO> dependencyList = new ArrayList<>();
		String query = "select * from DependencyTable where module = ? and dependencyNature = ?;";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, moduleName);
            statement.setString(2, Constants.temporaryDependencyNature);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	String groupId = resultSet.getString("groupId");
	            	String artifactId = resultSet.getString("artifactId");
	            	String recommendedVersion = resultSet.getString("recommendedVersion");
	            	DependencyDTO dependencyDTO = new DependencyDTO();
	            	dependencyDTO.setGroupId(groupId);
	            	dependencyDTO.setArtifactId(artifactId);
	            	dependencyDTO.setVersion(recommendedVersion);
	            	dependencyList.add(dependencyDTO);
	            }
            }catch (Exception e) {
				System.out.println(e);
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependencyList;
	}
	
	public static List<DependencyDTO> getOriginalDependencies(Connection connection, String moduleName) {
		List<DependencyDTO> dependencyList = new ArrayList<>();
		String query = "select groupId, artifactId, resolvedVersion from DependencyTable where module = ? and dependencyNature = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, moduleName);
            statement.setString(2, Constants.permanentDependencyNature);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	String groupId = resultSet.getString("groupId");
	            	String artifactId = resultSet.getString("artifactId");
	            	String resolvedVersion = resultSet.getString("resolvedVersion");
	            	DependencyDTO dependencyDTO = new DependencyDTO();
	            	dependencyDTO.setGroupId(groupId);
	            	dependencyDTO.setArtifactId(artifactId);
	            	dependencyDTO.setVersion(resolvedVersion);
	            	dependencyList.add(dependencyDTO);
	            }
            }catch (Exception e) {
				System.out.println(e);
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependencyList;
	}
	
	public static List<DependencyDTO> getRecommendedDependencies(Connection connection, String moduleName) {
		List<DependencyDTO> dependencyList = new ArrayList<>();
		String query = "select groupId, artifactId, recommendedVersion from DependencyTable where module = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, moduleName);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	String groupId = resultSet.getString("groupId");
	            	String artifactId = resultSet.getString("artifactId");
	            	String recommendedVersion = resultSet.getString("recommendedVersion");
	            	DependencyDTO dependencyDTO = new DependencyDTO();
	            	dependencyDTO.setGroupId(groupId);
	            	dependencyDTO.setArtifactId(artifactId);
	            	dependencyDTO.setVersion(recommendedVersion);
	            	dependencyList.add(dependencyDTO);
	            }
            }catch (Exception e) {
				System.out.println(e);
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependencyList;
	}

	public static List<DependencyDTO> getReportDependencyTable(Connection connection, String moduleName) {
		
		List<DependencyDTO> dependencyList = new ArrayList<>();
		String query = "select groupId, artifactId, resolvedVersion, recommendedVersion from DependencyTable where dependencyType='normal' and resolvedVersion != recommendedVersion and module = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, moduleName);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	String groupId = resultSet.getString("groupId");
	            	String artifactId = resultSet.getString("artifactId");
	            	String resolvedVersion = resultSet.getString("resolvedVersion");
	            	String recommendedVersion = resultSet.getString("recommendedVersion");
	            	DependencyDTO dependencyDTO = new DependencyDTO();
	            	dependencyDTO.setGroupId(groupId);
	            	dependencyDTO.setArtifactId(artifactId);
	            	dependencyDTO.setVersion(resolvedVersion+","+recommendedVersion);
	            	dependencyList.add(dependencyDTO);
	            }
            }catch (Exception e) {
				System.out.println(e);
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dependencyList;
	}

	
	
	
	public static int createVersionTable(String originalPomPath)
	{
		int status = 0;

		String DB_DIRECTORY_VER = originalPomPath;		
		String DB_URL_VER = get_DB_URL(originalPomPath);

		try
		{
			File dbDirVer = new File(DB_DIRECTORY_VER);
			if (!dbDirVer.exists()) {
				dbDirVer.mkdirs();
				System.out.println("Version Table Directory created: " + dbDirVer.getAbsolutePath());
			}
			else {
				System.out.println("Version Table Directory already exist");
			}

			Connection connectionVer = DriverManager.getConnection(DB_URL_VER);

			String createTableSQLVer = "CREATE TABLE IF NOT EXISTS "+Constants.VersionTable+" ("
					+ "property TEXT PRIMARY KEY,"
					+ "version TEXT NOT NULL)";
			try (Statement stmt = connectionVer.createStatement()) {
				stmt.execute(createTableSQLVer);
			}catch (Exception e) {
				status = 1;
				System.out.println("Exception while creating Version Table - "+e);
			}
			connectionVer.close();	
			System.out.println("Version Table created successfully");
		}catch(Exception e) {
			System.out.println(e);
		}
		
		return status;
	}
	
	public static boolean isVersionKeyPresent(Connection connection, String key) throws SQLException {
		String checkSQL = "SELECT 1 FROM "+Constants.VersionTable+" WHERE key = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
			pstmt.setString(1, key);
			ResultSet rs = pstmt.executeQuery();
			return rs.next();  // Returns true if key exists
		}
	}
	
//	public static void putVersionTableEntry(Connection connection, String property, String version) throws SQLException {
//		String upsertSQL = "INSERT INTO "+Constants.VersionTable+" (property, version) VALUES (?, ?) "
//				+ "ON CONFLICT(property) DO UPDATE SET version = excluded.version";
//		try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
//			pstmt.setString(1, property);
//			pstmt.setString(2, version);  // Convert Map to JSON string
//			pstmt.executeUpdate();
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	public static void putPOMPropertiesInVersionTable(Connection connection, String pomPath, Map<String, String> propertiesMap) {
		
		try {
			for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
//			    System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue().size());
//				DependencyDB.putVersionTableEntry(connection, entry.getKey(), entry.getValue());
				
				String property = entry.getKey();
				String version = entry.getValue();
				String upsertSQL = "INSERT INTO "+Constants.VersionTable+" (property, version) VALUES (?, ?) "
						+ "ON CONFLICT(property) DO UPDATE SET version = excluded.version";
				try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
					pstmt.setString(1, property);
					pstmt.setString(2, version);  // Convert Map to JSON string
					pstmt.executeUpdate();
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String resolveVersion(Connection connection, String groupId, String artifactId, String propertyVersion){

		if(propertyVersion.startsWith("${") && propertyVersion.endsWith("}"))
		{
			propertyVersion = propertyVersion.substring(2, propertyVersion.length() - 1);
		}
		else if(propertyVersion.equals(""))
		{
			return propertyVersion;
		}
		else if(propertyVersion.equals("NA"))
		{
			String query = "SELECT resolvedVersion FROM DependencyTable where "
					+ "groupId = ? and artifactId = ? and dependencyType = ?";
	        try (PreparedStatement statement = connection.prepareStatement(query)) {
	            statement.setString(1, groupId);
	            statement.setString(2, artifactId);
	            statement.setString(3, Constants.dependencyManagementType);
	            try (ResultSet resultSet = statement.executeQuery()) {
		            while (resultSet.next()) {
		            	propertyVersion = resultSet.getString("resolvedVersion");
		            }
	            }catch (Exception e) {
					System.out.println(e);
				}
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
		}
		
        String query = "SELECT version FROM VersionTable where property = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, propertyVersion);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	propertyVersion = resultSet.getString("version");
	            }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return propertyVersion;
	}

	public static String getVersionOfDependency(Connection connection, String groupId, String artifactId, String moduleName) {
		String query = "select recommendedVersion from DependencyTable where module = ? and groupId = ? and artifactId = ?;";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, moduleName);
            statement.setString(2, groupId);
            statement.setString(3, artifactId);
            try (ResultSet resultSet = statement.executeQuery()) {
	            while (resultSet.next()) {
	            	String recommendedVersion = resultSet.getString("recommendedVersion");
	            	return recommendedVersion;
	            }
            }catch (Exception e) {
				System.out.println(e);
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "NA";
	}

}
