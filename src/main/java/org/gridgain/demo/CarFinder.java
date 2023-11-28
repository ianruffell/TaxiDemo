package org.gridgain.demo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.gridgain.demo.model.Car;
import org.gridgain.demo.model.TripQueue;
import org.gridgain.demo.model.TripRequest;

import com.github.javafaker.Faker;

public class CarFinder {

	private final int MAX_CARS = 10000;
	private static AtomicInteger carCount = new AtomicInteger();
	private final Set<String> regs = new HashSet<>();
	private Faker faker;

	public CarFinder() {
		faker = new Faker();
	}

	public Car findCarForTrip(IgniteClientHelper ich, TripRequest tripRequest) {
		IgniteCache<String, Car> carCache = ich.getCarCache();
		SqlFieldsQuery query = new SqlFieldsQuery(
				"select registration from car where isQueued = 'false' and isCommitted = 'false' and isWorking = 'true' ORDER BY ABS( locationId - ?) ASC LIMIT 1");
		query.setArgs(tripRequest.getPULocationID());

		FieldsQueryCursor<List<?>> c1 = carCache.query(query);
		List<List<?>> all = c1.getAll();
		Car car = null;
		if (all.size() == 0) {
			int carId = carCount.incrementAndGet();
			if (carId <= MAX_CARS) {
				car = new Car(getReg(), faker.name().name(),
						tripRequest.getPULocationID(), tripRequest.getDOLocationID(), tripRequest.getRequest_datetime(),
						tripRequest.getRequest_datetime(), tripRequest.getDropoff_datetime(), true, true, false,
						tripRequest.getTrip_id());
			} else {
			}
		} else {
			// System.out.println("Found " + all.size() + " available cars");
			List<?> list = all.get(0);
			String reg = (String) list.get(0);
			car = ich.getCarCache().get(reg);
			car.setTripId(tripRequest.getTrip_id());
			car.setIsCommitted(true);
			car.setDropOffTime(tripRequest.getDropoff_datetime());
		}
		return car;
	}

	public TripQueue getTripQueue(IgniteClientHelper ich, TripRequest tripRequest) {
		// System.out.println("Find best car for queue");
		IgniteCache<String, Car> carCache = ich.getCarCache();

		SqlFieldsQuery carQuery = new SqlFieldsQuery(
				"SELECT registration FROM car WHERE isQueued = 'false' and dropOffTime < ? ORDER BY ABS( dropOffLocationId - ?) ASC LIMIT 1");
		carQuery.setArgs(tripRequest.getPickup_datetime(), tripRequest.getPULocationID());

		FieldsQueryCursor<List<?>> c2 = carCache.query(carQuery);
		List<List<?>> all = c2.getAll();
		if (all.size() == 0) {
			return null;
		} else {
			List<?> row = all.get(0);
			String reg = (String) row.get(0);
			TripQueue tripQueue = new TripQueue(reg, tripRequest.getPULocationID(), tripRequest.getDOLocationID(),
					tripRequest.getPickup_datetime(), tripRequest.getTrip_id());
			ich.getTripQueueCache().put(tripQueue.getTripId(), tripQueue);
			Car car = ich.getCarCache().get(reg);
			car.setIsQueued(true);
			ich.getCarCache().put(reg, car);
			// System.out.println(tripQueue);
			return tripQueue;
		}
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
