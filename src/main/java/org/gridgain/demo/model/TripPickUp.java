package org.gridgain.demo.model;

import java.sql.Timestamp;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class TripPickUp {

	@QuerySqlField
	private String trip_id;
	@QuerySqlField
	private Timestamp pickup_datetime;
	@QuerySqlField
	private Integer PULocationID;

	public TripPickUp(String trip_id, Timestamp pickup_datetime, Integer pULocationID) {
		super();
		this.trip_id = trip_id;
		this.setPickup_datetime(pickup_datetime);
		PULocationID = pULocationID;
	}

	public String getTrip_id() {
		return trip_id;
	}

	public void setTrip_id(String trip_id) {
		this.trip_id = trip_id;
	}

	public Timestamp getPickup_datetime() {
		return pickup_datetime;
	}

	public void setPickup_datetime(Timestamp pickup_datetime) {
		this.pickup_datetime = pickup_datetime;
	}

	public Integer getPULocationID() {
		return PULocationID;
	}

	public void setPULocationID(Integer pULocationID) {
		PULocationID = pULocationID;
	}

	@Override
	public String toString() {
		return "TripRequest [trip_id=" + trip_id + ", pickup_datetime=" + pickup_datetime + ", PULocationID="
				+ PULocationID + "]";
	}

}
