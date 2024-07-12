package org.gridgain.gg9test.taxi.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

@Table(
value = Car.TABLE_NAME,
zone = @Zone(value = "zone_test", storageProfiles = "default"),
indexes = {
		@Index(value = "locationId", columns = { @ColumnRef(value = "locationId") }),
		@Index(value = "tripId", columns = { @ColumnRef(value = "tripId") }),
		@Index(value = "queuedTripId", columns = { @ColumnRef(value = "queuedTripId") })
		})
public class Car {
	
	public final static String TABLE_NAME = "CAR";

	@Id
	@Column
	private String registration;
	@Column
	private String driver;
	@Column
	private Integer locationId;
	@Column
	private Integer dropOffLocationId;
	@Column
	private Long lastUpdate;
	@Column
	private Long startTime;
	@Column
	private Long dropOffTime;
	@Column
	private String tripId;
	@Column
	private String queuedTripId;
	
	public static Car forId(String reg) {
		Car car = new Car();
		car.setRegistration(reg);
		return car;
	}
	
	public Car() {
	}

	public Car(String registration, String driver, Integer locationId, Integer dropOffLocationId, Long lastUpdate,
			Long startTime, Long dropOffTime, String tripId, String queuedTripId) {
		this.registration = registration;
		this.driver = driver;
		this.locationId = locationId;
		this.dropOffLocationId = dropOffLocationId;
		this.lastUpdate = lastUpdate;
		this.startTime = startTime;
		this.dropOffTime = dropOffTime;
		this.tripId = tripId;
		this.queuedTripId = queuedTripId;
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

	public Long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public Long getDropOffTime() {
		return dropOffTime;
	}

	public void setDropOffTime(Long dropOffTime) {
		this.dropOffTime = dropOffTime;
	}

	public Integer getDropOffLocationId() {
		return dropOffLocationId;
	}

	public void setDropOffLocationId(Integer dropOffLocationId) {
		this.dropOffLocationId = dropOffLocationId;
	}

	public String getQueuedTripId() {
		return queuedTripId;
	}

	public void setQueuedTripId(String queuedTripId) {
		this.queuedTripId = queuedTripId;
	}

	@Override
	public String toString() {
		return "Car [registration=" + registration + ", driver=" + driver + ", locationId=" + locationId
				+ ", dropOffLocationId=" + dropOffLocationId + ", lastUpdate=" + lastUpdate + ", startTime=" + startTime
				+ ", dropOffTime=" + dropOffTime + ", tripId=" + tripId + ", queuedTripId=" + queuedTripId + "]";
	}

}
