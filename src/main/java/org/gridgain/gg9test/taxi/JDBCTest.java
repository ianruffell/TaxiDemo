package org.gridgain.gg9test.taxi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JDBCTest {

	public static void main(String[] args) throws Exception {
		new JDBCTest();
	}

	public JDBCTest() throws Exception {
		String sql = "SELECT * FROM Person;";
		//DriverManager.registerDriver(new org.apache.ignite.jdbc.IgniteJdbcDriver());
		try (
				Connection conn = DriverManager.getConnection("jdbc:ignite:thin://127.0.0.1:10800");
				Statement stmt = conn.createStatement();) {
			ResultSet rset = stmt.executeQuery(sql);
			while (rset.next()) {
				System.out.printf("| %3d | %30s |\n", rset.getInt("ID"), rset.getString("NAME"), rset.getString("CITY"),
						rset.getInt("AGE"));
			}
		}
	}

}
