package org.gridgain.demo.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.apache.avro.generic.GenericRecord;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class Trip {

	@QuerySqlField(index = true)
	private String trip_id;
	@QuerySqlField(index = true)
	private String registration;
	@QuerySqlField
	private String hvfhs_license_num;
	@QuerySqlField
	private String dispatching_base_num;
	@QuerySqlField
	private String originating_base_num;
	@QuerySqlField
	private Timestamp request_datetime;
	@QuerySqlField
	private Timestamp on_scene_datetime;
	@QuerySqlField
	private Timestamp pickup_datetime;
	@QuerySqlField
	private Timestamp dropoff_datetime;
	@QuerySqlField
	private Integer PULocationID;
	@QuerySqlField
	private Integer DOLocationID;
	@QuerySqlField
	private Double trip_miles;
	@QuerySqlField
	private Long trip_time;
	@QuerySqlField
	private Double base_passenger_fare;
	@QuerySqlField
	private Double tolls;
	@QuerySqlField
	private Double bcf;
	@QuerySqlField
	private Double sales_tax;
	@QuerySqlField
	private Double congestion_surcharge;
	@QuerySqlField
	private Double airport_fee;
	@QuerySqlField
	private Double tips;
	@QuerySqlField
	private Double driver_pay;
	@QuerySqlField
	private String shared_request_flag;
	@QuerySqlField
	private String shared_match_flag;
	@QuerySqlField
	private String access_a_ride_flag;
	@QuerySqlField
	private String wav_request_flag;
	@QuerySqlField
	private String wav_match_flag;

	public static Trip fromRecord(GenericRecord record) {
		Trip t = new Trip();
		t.trip_id = UUID.randomUUID().toString();
		t.hvfhs_license_num = (String) record.get("hvfhs_license_num");
		t.dispatching_base_num = (String) record.get("dispatching_base_num");
		t.originating_base_num = (String) record.get("originating_base_num");
		t.request_datetime = Timestamp.from(Instant.ofEpochMilli((Long) record.get("request_datetime") / 1000));
		if (record.get("on_scene_datetime") != null) {
			t.on_scene_datetime = Timestamp.from(Instant.ofEpochMilli((Long) record.get("on_scene_datetime") / 1000));
		}
		t.pickup_datetime = Timestamp.from(Instant.ofEpochMilli((Long) record.get("pickup_datetime") / 1000));
		t.dropoff_datetime = Timestamp.from(Instant.ofEpochMilli((Long) record.get("dropoff_datetime") / 1000));
		t.PULocationID = (Integer) record.get("PULocationID");
		t.DOLocationID = (Integer) record.get("DOLocationID");
		t.trip_miles = (Double) record.get("trip_miles");
		t.trip_time = (Long) record.get("trip_time");
		t.base_passenger_fare = (Double) record.get("base_passenger_fare");
		t.tolls = (Double) record.get("tolls");
		t.bcf = (Double) record.get("bcf");
		t.sales_tax = (Double) record.get("sales_tax");
		t.congestion_surcharge = (Double) record.get("congestion_surcharge");
		t.airport_fee = (Double) record.get("airport_fee");
		t.tips = (Double) record.get("tips");
		t.driver_pay = (Double) record.get("driver_pay");
		t.shared_request_flag = (String) record.get("shared_request_flag");
		t.shared_match_flag = (String) record.get("shared_match_flag");
		t.access_a_ride_flag = (String) record.get("access_a_ride_flag");
		t.wav_request_flag = (String) record.get("wav_request_flag");
		t.wav_match_flag = (String) record.get("wav_match_flag");
		return t;
	}

	public TripRequest toTripRequest() {
		return new TripRequest(trip_id, request_datetime, pickup_datetime, dropoff_datetime, PULocationID,
				DOLocationID);
	}

	public TripOnScene toTripOnScene() {
		return new TripOnScene(trip_id, on_scene_datetime, PULocationID);
	}

	public TripPickUp toTripPickUp() {
		return new TripPickUp(trip_id, pickup_datetime, PULocationID);
	}


	private Trip() {
	}

	public Trip(String trip_id, String registration, String hvfhs_license_num, String dispatching_base_num, String originating_base_num,
			Timestamp request_datetime, Timestamp on_scene_datetime, Timestamp pickup_datetime,
			Timestamp dropoff_datetime,
			Integer pULocationID, Integer dOLocationID, Double trip_miles, Long trip_time, Double base_passenger_fare,
			Double tolls, Double bcf, Double sales_tax, Double congestion_surcharge, Double airport_fee, Double tips,
			Double driver_pay, String shared_request_flag, String shared_match_flag, String access_a_ride_flag,
			String wav_request_flag, String wav_match_flag) {
		this.trip_id = trip_id;
		this.setRegistration(registration);
		this.hvfhs_license_num = hvfhs_license_num;
		this.dispatching_base_num = dispatching_base_num;
		this.originating_base_num = originating_base_num;
		this.request_datetime = request_datetime;
		this.on_scene_datetime = on_scene_datetime;
		this.pickup_datetime = pickup_datetime;
		this.dropoff_datetime = dropoff_datetime;
		this.PULocationID = pULocationID;
		this.DOLocationID = dOLocationID;
		this.trip_miles = trip_miles;
		this.trip_time = trip_time;
		this.base_passenger_fare = base_passenger_fare;
		this.tolls = tolls;
		this.bcf = bcf;
		this.sales_tax = sales_tax;
		this.congestion_surcharge = congestion_surcharge;
		this.airport_fee = airport_fee;
		this.tips = tips;
		this.driver_pay = driver_pay;
		this.shared_request_flag = shared_request_flag;
		this.shared_match_flag = shared_match_flag;
		this.access_a_ride_flag = access_a_ride_flag;
		this.wav_request_flag = wav_request_flag;
		this.wav_match_flag = wav_match_flag;
	}

	public String getHvfhs_license_num() {
		return hvfhs_license_num;
	}

	public void setHvfhs_license_num(String hvfhs_license_num) {
		this.hvfhs_license_num = hvfhs_license_num;
	}

	public String getDispatching_base_num() {
		return dispatching_base_num;
	}

	public void setDispatching_base_num(String dispatching_base_num) {
		this.dispatching_base_num = dispatching_base_num;
	}

	public String getOriginating_base_num() {
		return originating_base_num;
	}

	public void setOriginating_base_num(String originating_base_num) {
		this.originating_base_num = originating_base_num;
	}

	public Timestamp getRequest_datetime() {
		return request_datetime;
	}

	public void setRequest_datetime(Timestamp request_datetime) {
		this.request_datetime = request_datetime;
	}

	public Timestamp getOn_scene_datetime() {
		return on_scene_datetime;
	}

	public void setOn_scene_datetime(Timestamp on_scene_datetime) {
		this.on_scene_datetime = on_scene_datetime;
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

	public Double getTrip_miles() {
		return trip_miles;
	}

	public void setTrip_miles(Double trip_miles) {
		this.trip_miles = trip_miles;
	}

	public Long getTrip_time() {
		return trip_time;
	}

	public void setTrip_time(Long trip_time) {
		this.trip_time = trip_time;
	}

	public Double getBase_passenger_fare() {
		return base_passenger_fare;
	}

	public void setBase_passenger_fare(Double base_passenger_fare) {
		this.base_passenger_fare = base_passenger_fare;
	}

	public Double getTolls() {
		return tolls;
	}

	public void setTolls(Double tolls) {
		this.tolls = tolls;
	}

	public Double getBcf() {
		return bcf;
	}

	public void setBcf(Double bcf) {
		this.bcf = bcf;
	}

	public Double getSales_tax() {
		return sales_tax;
	}

	public void setSales_tax(Double sales_tax) {
		this.sales_tax = sales_tax;
	}

	public Double getCongestion_surcharge() {
		return congestion_surcharge;
	}

	public void setCongestion_surcharge(Double congestion_surcharge) {
		this.congestion_surcharge = congestion_surcharge;
	}

	public Double getAirport_fee() {
		return airport_fee;
	}

	public void setAirport_fee(Double airport_fee) {
		this.airport_fee = airport_fee;
	}

	public Double getTips() {
		return tips;
	}

	public void setTips(Double tips) {
		this.tips = tips;
	}

	public Double getDriver_pay() {
		return driver_pay;
	}

	public void setDriver_pay(Double driver_pay) {
		this.driver_pay = driver_pay;
	}

	public String getShared_request_flag() {
		return shared_request_flag;
	}

	public void setShared_request_flag(String shared_request_flag) {
		this.shared_request_flag = shared_request_flag;
	}

	public String getShared_match_flag() {
		return shared_match_flag;
	}

	public void setShared_match_flag(String shared_match_flag) {
		this.shared_match_flag = shared_match_flag;
	}

	public String getAccess_a_ride_flag() {
		return access_a_ride_flag;
	}

	public void setAccess_a_ride_flag(String access_a_ride_flag) {
		this.access_a_ride_flag = access_a_ride_flag;
	}

	public String getWav_request_flag() {
		return wav_request_flag;
	}

	public void setWav_request_flag(String wav_request_flag) {
		this.wav_request_flag = wav_request_flag;
	}

	public String getWav_match_flag() {
		return wav_match_flag;
	}

	public void setWav_match_flag(String wav_match_flag) {
		this.wav_match_flag = wav_match_flag;
	}

	public String getTrip_id() {
		return trip_id;
	}

	public void setTrip_id(String trip_id) {
		this.trip_id = trip_id;
	}

	public String getRegistration() {
		return registration;
	}

	public void setRegistration(String registration) {
		this.registration = registration;
	}

	@Override
	public String toString() {
		return "Trip [trip_id=" + trip_id + ", registration=" + registration + ", hvfhs_license_num="
				+ hvfhs_license_num + ", dispatching_base_num=" + dispatching_base_num + ", originating_base_num="
				+ originating_base_num + ", request_datetime=" + request_datetime + ", on_scene_datetime="
				+ on_scene_datetime + ", pickup_datetime=" + pickup_datetime + ", dropoff_datetime=" + dropoff_datetime
				+ ", PULocationID=" + PULocationID + ", DOLocationID=" + DOLocationID + ", trip_miles=" + trip_miles
				+ ", trip_time=" + trip_time + ", base_passenger_fare=" + base_passenger_fare + ", tolls=" + tolls
				+ ", bcf=" + bcf + ", sales_tax=" + sales_tax + ", congestion_surcharge=" + congestion_surcharge
				+ ", airport_fee=" + airport_fee + ", tips=" + tips + ", driver_pay=" + driver_pay
				+ ", shared_request_flag=" + shared_request_flag + ", shared_match_flag=" + shared_match_flag
				+ ", access_a_ride_flag=" + access_a_ride_flag + ", wav_request_flag=" + wav_request_flag
				+ ", wav_match_flag=" + wav_match_flag + "]";
	}

}
