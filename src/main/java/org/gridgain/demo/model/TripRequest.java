package org.gridgain.demo.model;

import java.sql.Timestamp;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

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

	public TripRequest(String trip_id, Timestamp request_datetime, Timestamp pickup_datetime,
			Timestamp dropoff_datetime,
			Integer pULocationID, Integer dOLocationID) {
		this.trip_id = trip_id;
		this.request_datetime = request_datetime;
		this.pickup_datetime = pickup_datetime;
		this.dropoff_datetime = dropoff_datetime;
		this.PULocationID = pULocationID;
		this.DOLocationID = dOLocationID;
	}

	public String getTrip_id() {
		return trip_id;
	}

	public void setTrip_id(String trip_id) {
		this.trip_id = trip_id;
	}

	public Timestamp getRequest_datetime() {
		return request_datetime;
	}

	public void setRequest_datetime(Timestamp request_datetime) {
		this.request_datetime = request_datetime;
	}

	public Integer getPULocationID() {
		return PULocationID;
	}

	public void setPULocationID(Integer pULocationID) {
		PULocationID = pULocationID;
	}

	public Integer getDOLocationID() {
		return DOLocationID;
	}

	public void setDOLocationID(Integer dOLocationID) {
		DOLocationID = dOLocationID;
	}

	public Timestamp getPickup_datetime() {
		return pickup_datetime;
	}

	public void setPickup_datetime(Timestamp pickup_datetime) {
		this.pickup_datetime = pickup_datetime;
	}

	public Timestamp getDropoff_datetime() {
		return dropoff_datetime;
	}

	public void setDropoff_datetime(Timestamp dropoff_datetime) {
		this.dropoff_datetime = dropoff_datetime;
	}

	@Override
	public String toString() {
		return "TripRequest [trip_id=" + trip_id + ", request_datetime=" + request_datetime + ", pickup_datetime="
				+ pickup_datetime + ", dropoff_datetime=" + dropoff_datetime + ", PULocationID=" + PULocationID
				+ ", DOLocationID=" + DOLocationID + "]";
	}
	
}
