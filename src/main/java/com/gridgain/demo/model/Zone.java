package com.gridgain.demo.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class Zone {

	@QuerySqlField(index = true)
	private Integer locationID;
	@QuerySqlField
	private String borough;
	@QuerySqlField
	private String zone;
	@QuerySqlField
	private String service_zone;

	public Zone(Integer locationID, String borough, String zone, String service_zone) {
		this.locationID = locationID;
		this.borough = borough;
		this.zone = zone;
		this.service_zone = service_zone;
	}

	public Integer getLocationID() {
		return locationID;
	}

	public void setLocationID(Integer locationID) {
		this.locationID = locationID;
	}

	public String getBorough() {
		return borough;
	}

	public void setBorough(String borough) {
		this.borough = borough;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getService_zone() {
		return service_zone;
	}

	public void setService_zone(String service_zone) {
		this.service_zone = service_zone;
	}


}
