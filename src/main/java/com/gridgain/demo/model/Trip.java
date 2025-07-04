package com.gridgain.demo.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.apache.avro.generic.GenericRecord;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
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

	public TripPickUp toTripPickUp() {
		return new TripPickUp(trip_id, pickup_datetime, PULocationID);
	}

}
