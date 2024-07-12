package org.gridgain.gg9test.taxi.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;


@Table(value = TripPickUp.TABLE_NAME,
zone = @Zone(value = "zone_test", storageProfiles = "default"),
indexes = {
		@Index(value = "tripId", columns = { @ColumnRef(value = "tripId") })
		})
public class TripPickUp {
	
	public final static String TABLE_NAME = "TRIP_PICKUP";

	@Id
	@Column
	private String tripId;
	@Column
	private Long pickupTime;
	@Column
	private Integer PULocationID;
	
	public static TripPickUp forId(String tripId) {
		return new TripPickUp(tripId, null, null);
	}
	
	public TripPickUp() {	
	}

	public TripPickUp(String tripId, Long pickupTime, Integer pULocationID) {
		this.tripId = tripId;
		this.pickupTime = pickupTime;
		PULocationID = pULocationID;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public Long getPickupTime() {
		return pickupTime;
	}

	public void setPickupTime(Long pickupTime) {
		this.pickupTime = pickupTime;
	}

	public Integer getPULocationID() {
		return PULocationID;
	}

	public void setPULocationID(Integer pULocationID) {
		PULocationID = pULocationID;
	}

	@Override
	public String toString() {
		return "TripPickUp [tripId=" + tripId + ", pickupTime=" + pickupTime + ", PULocationID=" + PULocationID + "]";
	}

}
