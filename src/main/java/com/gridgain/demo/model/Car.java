package com.gridgain.demo.model;

import java.sql.Timestamp;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Car {

	@QuerySqlField
	private String registration;
	@QuerySqlField
	private String driver;
	@QuerySqlField(index = true)
	private Integer locationId;
	@QuerySqlField
	private Integer dropOffLocationId;
	@QuerySqlField
	private Timestamp lastUpdate;
	@QuerySqlField
	private Timestamp startTime;
	@QuerySqlField
	private Timestamp dropOffTime;
	@QuerySqlField(index = true)
	private String tripId;
	@QuerySqlField(index = true)
	private String queuedTripId;

}
