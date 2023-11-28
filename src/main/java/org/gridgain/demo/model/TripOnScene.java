package org.gridgain.demo.model;

import java.sql.Timestamp;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class TripOnScene {

	@QuerySqlField
	private String trip_id;
	@QuerySqlField
	private Timestamp on_scene_datetime;
	@QuerySqlField
	private Integer PULocationID;

	public TripOnScene(String trip_id, Timestamp on_scene_datetime, Integer pULocationID) {
		this.trip_id = trip_id;
		this.on_scene_datetime = on_scene_datetime;
		PULocationID = pULocationID;
	}

	public String getTrip_id() {
		return trip_id;
	}

	public void setTrip_id(String trip_id) {
		this.trip_id = trip_id;
	}

	public Timestamp getOn_scene_datetime() {
		return on_scene_datetime;
	}

	public void setOn_scene_datetime(Timestamp on_scene_datetime) {
		this.on_scene_datetime = on_scene_datetime;
	}

	public Integer getPULocationID() {
		return PULocationID;
	}

	public void setPULocationID(Integer pULocationID) {
		PULocationID = pULocationID;
	}

	@Override
	public String toString() {
		return "TripOnScene [trip_id=" + trip_id + ", on_scene_datetime=" + on_scene_datetime + ", PULocationID="
				+ PULocationID + "]";
	}

}
