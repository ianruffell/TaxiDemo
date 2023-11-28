package org.gridgain.demo.model;

import java.sql.Timestamp;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class TripQueue {

	@QuerySqlField
	private String registration;
	@QuerySqlField(index = true)
	private Integer PickUpLocationId;
	@QuerySqlField
	private Integer dropOffLocationId;
	@QuerySqlField
	private Timestamp pickUpTime;
	@QuerySqlField(index = true)
	private String tripId;

	public TripQueue(String registration, Integer pickUpLocationId, Integer dropOffLocationId, Timestamp pickUpTime,
			String tripId) {
		this.registration = registration;
		PickUpLocationId = pickUpLocationId;
		this.dropOffLocationId = dropOffLocationId;
		this.pickUpTime = pickUpTime;
		this.tripId = tripId;
	}

	public String getRegistration() {
		return registration;
	}

	public void setRegistration(String registration) {
		this.registration = registration;
	}

	public Integer getPickUpLocationId() {
		return PickUpLocationId;
	}

	public void setPickUpLocationId(Integer pickUpLocationId) {
		PickUpLocationId = pickUpLocationId;
	}

	public Integer getDropOffLocationId() {
		return dropOffLocationId;
	}

	public void setDropOffLocationId(Integer dropOffLocationId) {
		this.dropOffLocationId = dropOffLocationId;
	}

	public Timestamp getPickUpTime() {
		return pickUpTime;
	}

	public void setPickUpTime(Timestamp pickUpTime) {
		this.pickUpTime = pickUpTime;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	@Override
	public String toString() {
		return "TripQueue [registration=" + registration + ", PickUpLocationId=" + PickUpLocationId
				+ ", dropOffLocationId=" + dropOffLocationId + ", pickUpTime=" + pickUpTime + ", tripId=" + tripId
				+ "]";
	}

}
