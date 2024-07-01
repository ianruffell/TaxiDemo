package org.gridgain.gg9test;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;

public class IgniteClientTest {

	public static void main(String[] args) throws Exception {
		try (IgniteClient client = IgniteClient.builder().addresses("127.0.0.1:10800").build()) {
			ResultSet<SqlRow> rs = client.sql().execute(null, "SELECT * FROM CITY", null);
			if (rs.hasNext()) {
				SqlRow row = rs.next();
				for (int i = 0; i < row.columnCount(); i++) {
					System.out.print(row.columnName(i) + "\t");
				}
				System.out.println();

			}
			while (rs.hasNext()) {
				SqlRow row = rs.next();

				for (int i = 0; i < row.columnCount(); i++) {
					System.out.print(row.value(i) + "\t");
				}
				System.out.println();
			}
		}
	}

}
