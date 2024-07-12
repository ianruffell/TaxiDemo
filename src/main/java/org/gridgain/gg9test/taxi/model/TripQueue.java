package org.gridgain.gg9test.taxi.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

@Table(value = TripQueue.TABLE_NAME,
zone = @Zone(value = "zone_test", storageProfiles = "default"),
indexes = {
		@Index(value = "tripId", columns = { @ColumnRef(value = "tripId") }),
		@Index(value = "PickUpLocationId", columns = { @ColumnRef(value = "PickUpLocationId") }),
		@Index(value = "pickUpTime", columns = { @ColumnRef(value = "pickUpTime") })
		})
public class TripQueue {
	
	public final static String TABLE_NAME = "TRIP_QUEUE";

	@Id
	@Column
	private String tripId;
	@Column
	private String registration;
	@Column
	private Integer PickUpLocationId;
	@Column
	private Integer dropOffLocationId;
	@Column
	private Long pickUpTime;
	
	public static TripQueue forId(String tripId) {
		return new TripQueue(tripId, null, null, null, null);
	}

	public TripQueue() {
		
	}
	
	public TripQueue(String registration, Integer pickUpLocationId, Integer dropOffLocationId, Long pickUpTime,
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

	public Long getPickUpTime() {
		return pickUpTime;
	}

	public void setPickUpTime(Long pickUpTime) {
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
