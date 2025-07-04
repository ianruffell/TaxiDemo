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
public class TripQueue {

	@QuerySqlField
	private String registration;
	@QuerySqlField(index = true)
	private Integer PickUpLocationId;
	@QuerySqlField
	private Integer dropOffLocationId;
	@QuerySqlField(index = true)
	private Timestamp pickUpTime;
	@QuerySqlField
	private String tripId;

}
