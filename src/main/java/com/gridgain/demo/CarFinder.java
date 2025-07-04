package com.gridgain.demo;

import static com.gridgain.demo.App.MAX_CARS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;

import com.github.javafaker.Faker;
import com.gridgain.demo.model.Car;
import com.gridgain.demo.model.TripRequest;

public class CarFinder {

	private static AtomicInteger carCount = new AtomicInteger();
	private final Set<String> regs = new HashSet<>();
	private Faker faker;

	public CarFinder() {
		faker = new Faker();
	}

	public Car findCarForTrip(IgniteClientHelper ich, TripRequest tripRequest) {
		IgniteCache<String, Car> carCache = ich.getCarCache();
		SqlFieldsQuery query = new SqlFieldsQuery(
				"select registration from car where tripId IS NULL AND queuedTripId IS NULL ORDER BY ABS(locationId - ?) ASC LIMIT 1");
		query.setArgs(tripRequest.getPULocationID());

		FieldsQueryCursor<List<?>> c1 = carCache.query(query);
		List<List<?>> all = c1.getAll();
		Car car = null;
		if (all.size() == 0) {
			// System.out.println("No Car found");
			if (carCount.get() < MAX_CARS) {
				carCount.incrementAndGet();
				car = new Car(getReg(), faker.name().name(), tripRequest.getPULocationID(),
						tripRequest.getDOLocationID(), tripRequest.getRequest_datetime(),
						tripRequest.getRequest_datetime(), tripRequest.getDropoff_datetime(), tripRequest.getTrip_id(),
						null);
			}
		} else {
			// System.out.println("Found " + all.size() + " available cars");
			List<?> list = all.get(0);
			String reg = (String) list.get(0);
			car = ich.getCarCache().get(reg);
			car.setTripId(tripRequest.getTrip_id());
			car.setDropOffTime(tripRequest.getDropoff_datetime());
		}

		// System.out.println("findCarForTrip: " + car);
		return car;
	}

	public Car findCarForQueue(IgniteClientHelper ich, TripRequest tripRequest) {
		IgniteCache<String, Car> carCache = ich.getCarCache();
		SqlFieldsQuery query = new SqlFieldsQuery(
				"select registration from car where queuedTripId IS NULL ORDER BY ABS(locationId - ?) ASC LIMIT 1");
		query.setArgs(tripRequest.getPULocationID());

		FieldsQueryCursor<List<?>> c1 = carCache.query(query);
		List<List<?>> all = c1.getAll();
		Car car = null;
		if (all.size() == 0) {
			System.out.println("No Car found for queue");
		} else {
			// System.out.println("Found " + all.size() + " available cars");
			List<?> list = all.get(0);
			String reg = (String) list.get(0);
			car = ich.getCarCache().get(reg);
			car.setQueuedTripId(tripRequest.getTrip_id());
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
