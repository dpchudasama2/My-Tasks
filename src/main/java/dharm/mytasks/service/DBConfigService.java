package dharm.mytasks.service;

import java.sql.Statement;

import dharm.mytasks.util.DatabaseUtil;

public class DBConfigService {
	
	public static void validateDBConnection() {
		try {
			DatabaseUtil.get("SELECT 1 FROM COMPONENT", null, 0);
		} catch (Exception ex) {
			resetDB();
			System.out.println("Re configured database");
		}
	}

	private static void resetDB() {
//		if(url==null) url = "jdbc:h2:./app-config;MVCC=FALSE";
//		final String schemaName = "MY_TASKS";
		String[] queries = new String[]{
			"DROP ALL OBJECTS",
//			"DROP SCHEMA IF EXISTS "+schemaName,
//			"CREATE SCHEMA "+schemaName,
//			"USE "+schemaName,

			"CREATE TABLE APPCONFIG ("
			+ " KEY VARCHAR(100) NOT NULL PRIMARY KEY,"
			+ " VAL VARCHAR(200)"
			+ " )",
			"INSERT INTO APPCONFIG (KEY, VAL) VALUES ('DEFAULT_HOST','localhost')",

			"CREATE TABLE PROJECT ("
			+ " PROJECTID IDENTITY NOT NULL PRIMARY KEY,"
			+ " TITLE VARCHAR(150) NOT NULL,"
			+ " PARENTPROJECTID INT,"
			+ " CHILDCOUNT INT DEFAULT 0,"
			+ " LASTUSEDON TIMESTAMP NOT NULL"
			+ ")",
			"INSERT INTO PROJECT (TITLE, PARENTPROJECTID, CHILDCOUNT, LASTUSEDON) VALUES('Root', NULL, 2, NOW())",
			//TODO insert Dummy proj data
			"INSERT INTO PROJECT (TITLE, PARENTPROJECTID, CHILDCOUNT, LASTUSEDON) VALUES('Test1', 1, 2, NOW())",
			"INSERT INTO PROJECT (TITLE, PARENTPROJECTID, CHILDCOUNT, LASTUSEDON) VALUES('Test2', 1, 1, NOW())",
			"INSERT INTO PROJECT (TITLE, PARENTPROJECTID, CHILDCOUNT, LASTUSEDON) VALUES('Test1_1', 2, 0, NOW())",
			"INSERT INTO PROJECT (TITLE, PARENTPROJECTID, CHILDCOUNT, LASTUSEDON) VALUES('Test1_2', 2, 0, NOW())",
			"INSERT INTO PROJECT (TITLE, PARENTPROJECTID, CHILDCOUNT, LASTUSEDON) VALUES('Test2_1', 3, 0, NOW())",

			
			"CREATE TABLE COMPONENTTYPE ("
			+ " COMPONENTTYPEID IDENTITY NOT NULL PRIMARY KEY,"
			+ " TYPENAME VARCHAR(50),"
			+ " COMMANDPREFIX VARCHAR(150) NOT NULL,"
			+ " COMMANDSUFFIX VARCHAR(150) NOT NULL,"
			+ " DESC VARCHAR(200)"
			+ ")",
			"INSERT INTO COMPONENTTYPE (TYPENAME, COMMANDPREFIX, COMMANDSUFFIX, DESC) VALUES('Default', '', '', 'Default type for all')",
			"INSERT INTO COMPONENTTYPE (TYPENAME, COMMANDPREFIX, COMMANDSUFFIX, DESC) VALUES('NewLine', '', '', 'Change the line')",
			"INSERT INTO COMPONENTTYPE (TYPENAME, COMMANDPREFIX, COMMANDSUFFIX, DESC) VALUES('Open', 'xdg-open|', '', 'Open any file with system opener, arg=fullFilePath')",
			"INSERT INTO COMPONENTTYPE (TYPENAME, COMMANDPREFIX, COMMANDSUFFIX, DESC) VALUES('Notify', 'notify-send|', '', 'display notification')",

			"CREATE TABLE COMPONENT ("
			+ " COMPONENTID IDENTITY NOT NULL PRIMARY KEY,"
			+ " LABEL VARCHAR(50) NOT NULL,"
			+ " COMMAND VARCHAR(100) NOT NULL,"
			+ " DESC VARCHAR(200),"
			+ " DISPORDER INT NOT NULL DEFAULT 0,"
			+ " COMPONENTTYPEID INT DEFAULT 1,"
			+ " PROJECTID INT"
			+ ")",
			"INSERT INTO COMPONENT (LABEL, COMMAND, DESC, DISPORDER, COMPONENTTYPEID, PROJECTID) VALUES('Notify Test 1', 'Test 1', 'Notification test', 1, 4, 4)",
			"INSERT INTO COMPONENT (LABEL, COMMAND, DESC, DISPORDER, COMPONENTTYPEID, PROJECTID) VALUES('Notify Test 2', 'Test 2', 'Notification test', 2, 4, 4)",
			"INSERT INTO COMPONENT (LABEL, COMMAND, DESC, DISPORDER, COMPONENTTYPEID, PROJECTID) VALUES('Notify Test 2', 'Test 2', 'Notification test', 3, 2, 4)",
			"INSERT INTO COMPONENT (LABEL, COMMAND, DESC, DISPORDER, COMPONENTTYPEID, PROJECTID) VALUES('Notify Test 3', 'Test 3', 'Notification test', 4, 4, 4)",
		};
		
		DatabaseUtil.execute(conn->{
			Statement stmt = conn.createStatement();
			for (String query : queries)
				stmt.execute(query);
			return null;
		});
		System.out.println("DB Configured");
	}

}
