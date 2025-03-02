package common;

public class Utils {

	public static String get_DB_URL(String originalPomPath)
	{
		String DB_DIRECTORY_VUL = originalPomPath;
		String DB_URL_VUL = "jdbc:sqlite:" + DB_DIRECTORY_VUL + "\\" + Constants.DB_NAME;
		return DB_URL_VUL;
	}
}
