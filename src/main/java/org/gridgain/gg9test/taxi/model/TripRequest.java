package org.gridgain.gg9test.taxi.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

@Table(value = TripRequest.TABLE_NAME,
zone = @Zone(value = "zone_test", storageProfiles = "default"),
indexes = {
		@Index(value = "tripId", columns = { @ColumnRef(value = "tripId") })
}
)
public class TripRequest {
	
	public final static String TABLE_NAME = "TRIP_REQUEST";

	@Id
	@Column
	private String tripId;
	@Column
	private Long requestDatetime;
	@Column
	private Long pickupDatetime;
	@Column
	private Long dropoffDatetime;
	@Column
	private Integer pickupLocationId;
	@Column
	private Integer dropoffLocationId;
	
	public static TripRequest forId(String tripId) {
		return new TripRequest(tripId, null, null, null, null, null);
	}
	
	public TripRequest() {	
	}

	public TripRequest(String tripId, Long requestDatetime, Long pickupDatetime, Long dropoffDatetime,
			Integer pickupLocationId, Integer dropoffLocationId) {
		this.tripId = tripId;
		this.requestDatetime = requestDatetime;
		this.pickupDatetime = pickupDatetime;
		this.dropoffDatetime = dropoffDatetime;
		this.pickupLocationId = pickupLocationId;
		this.dropoffLocationId = dropoffLocationId;
	}

	public String getTripId() {
		return tripId;
	}

	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	public Long getRequestDatetime() {
		return requestDatetime;
	}

	public void setRequestDatetime(Long requestDatetime) {
		this.requestDatetime = requestDatetime;
	}

	public Long getPickupDatetime() {
		return pickupDatetime;
	}

	public void setPickupDatetime(Long pickupDatetime) {
		this.pickupDatetime = pickupDatetime;
	}

	public Long getDropoffDatetime() {
		return dropoffDatetime;
	}

	public void setDropoffDatetime(Long dropoffDatetime) {
		this.dropoffDatetime = dropoffDatetime;
	}

	public Integer getPickupLocationId() {
		return pickupLocationId;
	}

	public void setPickupLocationId(Integer pickupLocationId) {
		this.pickupLocationId = pickupLocationId;
	}

	public Integer getDropoffLocationId() {
		return dropoffLocationId;
	}

	public void setDropoffLocationId(Integer dropoffLocationId) {
		this.dropoffLocationId = dropoffLocationId;
	}

	@Override
	public String toString() {
		return "TripRequest [tripId=" + tripId + ", requestDatetime=" + requestDatetime + ", pickupDatetime="
				+ pickupDatetime + ", dropoffDatetime=" + dropoffDatetime + ", pickupLocationId=" + pickupLocationId
				+ ", dropoffLocationId=" + dropoffLocationId + "]";
	}

}
