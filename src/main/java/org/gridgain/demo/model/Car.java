package org.gridgain.demo.model;

import java.sql.Timestamp;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

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
	@QuerySqlField
	private Boolean isCommitted;
	@QuerySqlField
	private Boolean isWorking;
	@QuerySqlField
	private Boolean isQueued;
	@QuerySqlField(index = true)
	private String tripId;

	public Car(String registration, String driver, Integer locationId, Integer dropOffLocationId, Timestamp lastUpdate,
			Timestamp startTime, Timestamp dropOffTime, Boolean isCommitted, Boolean isWorking, Boolean isQueued,
			String tripId) {
		this.registration = registration;
		this.driver = driver;
		this.locationId = locationId;
		this.dropOffLocationId = dropOffLocationId;
		this.lastUpdate = lastUpdate;
		this.startTime = startTime;
		this.dropOffTime = dropOffTime;
		this.isCommitted = isCommitted;
		this.isWorking = isWorking;
		this.isQueued = isQueued;
		this.tripId = tripId;
	}

	public String getRegistration() {
		return registration;
	}

	public void setRegistration(String registration) {
		this.registration = registration;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public Integer getLocationId() {
		return locationId;
	}

	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	public Timestamp getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Timestamp lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	public Boolean getIsCommitted() {
		return isCommitted;
	}

	public void setIsCommitted(Boolean isCommitted) {
		this.isCommitted = isCommitted;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public Boolean getIsWorking() {
		return isWorking;
	}

	public void setIsWorking(Boolean isWorking) {
		this.isWorking = isWorking;
	}

	public Timestamp getDropOffTime() {
		return dropOffTime;
	}

	public void setDropOffTime(Timestamp dropOffTime) {
		this.dropOffTime = dropOffTime;
	}

	public Integer getDropOffLocationId() {
		return dropOffLocationId;
	}

	public void setDropOffLocationId(Integer dropOffLocationId) {
		this.dropOffLocationId = dropOffLocationId;
	}

	public Boolean getIsQueued() {
		return isQueued;
	}

	public void setIsQueued(Boolean isQueued) {
		this.isQueued = isQueued;
	}

	@Override
	public String toString() {
		return "Car [registration=" + registration + ", driver=" + driver + ", locationId=" + locationId
				+ ", lastUpdate=" + lastUpdate + ", startTime=" + startTime + ", isCommitted=" + isCommitted
				+ ", isWorking=" + isWorking + ", tripId=" + tripId + "]";
	}

}
