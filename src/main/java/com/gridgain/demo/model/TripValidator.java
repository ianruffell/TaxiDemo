package com.gridgain.demo.model;

import java.time.Instant;

public class TripValidator {
	private static final Instant START_MONTH = Instant.parse("2023-07-01T00:00:00.00Z");
	private static final Instant END_MONTH = Instant.parse("2023-07-31T23:59:59.00Z");

	public static boolean isValid(Trip trip) {
		// Rules
		/*
		 * Pickup/drop-off date and time are strictly within the selected month period.
		 * Pickup/drop-off Location ID should be within the range of [1, 263]. Trip
		 * distance should greater than 0 miles but less than 100 miles. Fare amount
		 * should be at least $2.5 but at most $250. Trip duration should be more than a
		 * minute and less than three hours.
		 */

		if (trip.getPULocationID() < 1 || trip.getPULocationID() > 265) {
			System.err.println("Invalid pickup location");
			return false;
		}
		if (trip.getDOLocationID() < 1 || trip.getDOLocationID() > 265) {
			System.err.println("Invalid dropoff location");
			return false;
		}
		if (trip.getPickup_datetime().toInstant().isBefore(START_MONTH)) {
			System.err.println("Pickup before start of month");
			return false;
		}
		if (trip.getPickup_datetime().toInstant().isAfter(END_MONTH)) {
			System.err.println("Pickup after end of month");
			return false;
		}
		if (trip.getDropoff_datetime().toInstant().isBefore(START_MONTH)) {
			System.err.println("Dropoff before start of month");
			return false;
		}
		if (trip.getDropoff_datetime().toInstant().isAfter(END_MONTH)) {
			System.err.println("Dropoff after end of month");
			return false;
		}
		if (trip.getTrip_miles() <= 0.0) {
			System.err.println("Trip distance should be greater than 0 miles");
			return false;
		}
		if (trip.getTrip_miles() > 250.0) {
			System.err.println("Trip distance should be less than 250 miles, was " + trip.getTrip_miles());
			return false;
		}
		if (trip.getBase_passenger_fare() < 2.5 || trip.getBase_passenger_fare() > 600) {
			System.err.println(
					"Fare amount should be at least $2.5 but at most $600, was " + trip.getBase_passenger_fare());
			return false;
		}
		if (trip.getTrip_time() < 10 || trip.getTrip_time() > 14400) {
			System.err
					.println("Trip duration should be more than 10s and less than 4 hours, was " + trip.getTrip_time());
			return false;
		}

		return true;
	}

}
