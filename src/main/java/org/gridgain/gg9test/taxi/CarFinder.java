package org.gridgain.gg9test.taxi;

import static org.gridgain.gg9test.taxi.App.MAX_CARS;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.gridgain.gg9test.taxi.model.Car;
import org.gridgain.gg9test.taxi.model.TripRequest;

import com.github.javafaker.Faker;

public class CarFinder {

	private static AtomicInteger carCount = new AtomicInteger();
	private final Set<String> regs = new HashSet<>();
	private Faker faker;

	public CarFinder() {
		faker = new Faker();
		
		
	}

	public Car findCarForTrip(App app, TripRequest tripRequest) {
		Statement query = app.getIgnite().sql().createStatement("select registration from car where tripId IS NULL AND queuedTripId IS NULL ORDER BY ABS(locationId - ?) ASC LIMIT 1");
		ResultSet<SqlRow> resultSet = app.getIgnite().sql().execute(null, query, tripRequest.getPickupLocationId());
		Car car = null;
		if (!resultSet.hasNext()) {
			// System.out.println("No Car found");
			if (carCount.get() < MAX_CARS) {
				carCount.incrementAndGet();
				car = new Car(getReg(), faker.name().name(), tripRequest.getPickupLocationId(),
						tripRequest.getDropoffLocationId(), tripRequest.getRequestDatetime(),
						tripRequest.getRequestDatetime(), tripRequest.getDropoffDatetime(), tripRequest.getTripId(),
						null);
			}
		} else {
			// System.out.println("Found " + all.size() + " available cars");
			String reg = (String) resultSet.next().stringValue(0);
			car = app.getCarKVView().get(null, reg);
			car.setTripId(tripRequest.getTripId());
			car.setDropOffTime(tripRequest.getDropoffDatetime());
		}

		// System.out.println("findCarForTrip: " + car);
		return car;
	}

	public Car findCarForQueue(App app, TripRequest tripRequest) {
		Statement query = app.getIgnite().sql().createStatement("select registration from car where queuedTripId IS NULL ORDER BY ABS(locationId - ?) ASC LIMIT 1");
		ResultSet<SqlRow> resultSet = app.getIgnite().sql().execute(null, query, tripRequest.getPickupLocationId());

		Car car = null;
		if (!resultSet.hasNext()) {
			System.out.println("No Car found for queue");
		} else {
			// System.out.println("Found " + all.size() + " available cars");
			String reg = (String) resultSet.next().stringValue(0);
			car = app.getCarKVView().get(null, reg);
			car.setQueuedTripId(tripRequest.getTripId());
		}

		// System.out.println("findCarForTrip: " + car);
		return car;
	}

	private String getReg() {
		String reg = faker.aviation().airport() + " " + faker.number().digits(4);
		if (regs.contains(reg)) {
			return getReg();
		} else {
			regs.add(reg);
		}

		return reg;
	}

}
