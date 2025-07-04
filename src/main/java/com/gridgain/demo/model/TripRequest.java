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
public class TripRequest {

	@QuerySqlField
	private String trip_id;
	@QuerySqlField
	private Timestamp request_datetime;
	@QuerySqlField
	private Timestamp pickup_datetime;
	@QuerySqlField
	private Timestamp dropoff_datetime;
	@QuerySqlField
	private Integer PULocationID;
	@QuerySqlField
	private Integer DOLocationID;
	
}
