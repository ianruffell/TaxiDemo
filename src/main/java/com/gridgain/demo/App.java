package com.gridgain.demo;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;

import org.apache.avro.generic.GenericRecord;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.parquet.avro.AvroParquetReader;

import com.gridgain.demo.model.Car;
import com.gridgain.demo.model.Trip;
import com.gridgain.demo.model.TripPickUp;
import com.gridgain.demo.model.TripQueue;
import com.gridgain.demo.model.TripRequest;
import com.gridgain.demo.model.TripValidator;

public class App implements Runnable {

	public static final String TRIP_DATA_FILE = "src/main/resources/fhvhv_tripdata_2023-07.parquet";
	private static final int SCALE_FACTOR = 5;
	private static final int NUM_RECORDS = 1000000;
	public static final int MAX_CARS = 13000;

	ArrayListValuedHashMap<Timestamp, Trip> request = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Timestamp, Trip> pickup = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Timestamp, Trip> dropOff = new ArrayListValuedHashMap<>();
	List<Timestamp> times = Collections.synchronizedList(new LinkedList<Timestamp>());

	private IgniteClientHelper ich;

	private Timestamp currentTime = Timestamp.from(Instant.ofEpochMilli(1688161860000l));

	public static void main(String[] args) throws Exception {
		new App();
	}

	public App() throws Exception {
		ich = new IgniteClientHelper(true);

		System.out.print("Setup Continuous Queries ...");
		System.out.print(" TripRequest");
		setupTripRequestProcessor();
		System.out.print(", TripPickup");
		setupTripPickUpProcessor();
		System.out.print(", TripComplete");
		setupTripCompleteProcessor();
		System.out.println(" Done");

		Set<Timestamp> timesSet = new HashSet<>();

		int count = 0;
		GenericRecord record;
		AvroParquetReader<GenericRecord> reader = getTripReader();
		while ((record = reader.read()) != null) {
			Trip trip = Trip.fromRecord(record);

			if (TripValidator.isValid(trip)) {
				Timestamp req = trip.getRequest_datetime();
				Timestamp pu = trip.getPickup_datetime();
				Timestamp dof = trip.getDropoff_datetime();

				if (req.toInstant().isBefore(pu.toInstant()) && pu.toInstant().isBefore(dof.toInstant())) {
					timesSet.add(req);
					timesSet.add(pu);
					timesSet.add(dof);

					request.put(req, trip);
					pickup.put(pu, trip);
					dropOff.put(dof, trip);

					// System.out.printf("%d,%s,%s,%s\n", count, req.toString(), pu.toString(),
					// dof.toString());

					count++;
					if (count % 10000 == 0) {
						System.out.println("Read: " + count);
					}
					if (count % NUM_RECORDS == 0) {
						break;
					}
				}
			}
		}

		times.addAll(timesSet);
		Collections.sort(times);

		System.out.println("Start dataflow ...");
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this, 0, (1000 / SCALE_FACTOR),
				TimeUnit.MILLISECONDS);
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Reporter(ich), 0, 2, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		try {
			currentTime = times.remove(0);

			List<Trip> list = request.get(currentTime);
			for (Trip trip : list) {
				TripRequest tr = trip.toTripRequest();
				// System.out.println("Request : " + tr);
				ich.getTripRequestCache().put(tr.getTrip_id(), tr);
			}
			request.remove(currentTime);

			list = pickup.get(currentTime);
			for (Trip trip : list) {
				TripPickUp tup = trip.toTripPickUp();
				// System.out.println("Pickup : " + tup);
				ich.getTripPickUpCache().put(tup.getTrip_id(), tup);
			}
			pickup.remove(currentTime);

			list = dropOff.get(currentTime);
			for (Trip trip : list) {
				// System.out.println("Drop Off: " + trip);
				ich.getTripCache().put(trip.getTrip_id(), trip);
			}
			dropOff.remove(currentTime);

		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

	}

	public static AvroParquetReader<GenericRecord> getTripReader() throws IllegalArgumentException, IOException {
		@SuppressWarnings("deprecation")
		AvroParquetReader<GenericRecord> reader = new AvroParquetReader<GenericRecord>(new Configuration(),
				new Path(TRIP_DATA_FILE));

		return reader;

	}

	private void setupTripRequestProcessor() {

		ContinuousQuery<String, TripRequest> contQuery = new ContinuousQuery<>();
		
		contQuery.setRemoteFilterFactory(new Factory<CacheEntryEventFilter<String, TripRequest>>() {
			private static final long serialVersionUID = -5075121585926851897L;

			@Override
			public CacheEntryEventFilter<String, TripRequest> create() {
                return new CacheEntryEventFilter<String, TripRequest>() {
                    @Override public boolean evaluate(CacheEntryEvent<? extends String, ? extends TripRequest> event) {
                    	return event.getEventType() == EventType.CREATED;
                    }
                };
            }
        });

		contQuery.setLocalListener(new CacheEntryUpdatedListener<String, TripRequest>() {
			IgniteCache<String, Car> carCache = ich.getCarCache();
			CarFinder carFinder = new CarFinder();

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends TripRequest>> events)
					throws CacheEntryListenerException {
				for (CacheEntryEvent<? extends String, ? extends TripRequest> entry : events) {
					TripRequest tr = entry.getValue();
					Car car = carFinder.findCarForTrip(ich, tr);
					if (car != null) {
						car.setTripId(tr.getTrip_id());
						car.setQueuedTripId(null);
					} else {
						car = carFinder.findCarForQueue(ich, tr);
						if (car != null) {
							car.setQueuedTripId(tr.getTrip_id());
							TripQueue tripQueue = new TripQueue(car.getRegistration(), tr.getPULocationID(),
									tr.getDOLocationID(), tr.getPickup_datetime(), tr.getTrip_id());
							ich.getTripQueueCache().put(tripQueue.getTripId(), tripQueue);
							ich.getTripRequestCache().remove(tr.getTrip_id());
							// System.out.println("Add to Queue " + tripQueue.getTripId());
						} else {
							System.err.println("No car for queue!");
						}
					}
					carCache.put(car.getRegistration(), car);
				}
			}
		});

		ich.getTripRequestCache().query(contQuery);
	}

	private void setupTripPickUpProcessor() {
		ContinuousQuery<String, TripPickUp> contQuery = new ContinuousQuery<>();
		contQuery.setRemoteFilterFactory(
				(Factory<? extends CacheEntryEventFilter<String, TripPickUp>>) () -> new CacheEntryEventFilter<String, TripPickUp>() {

					@Override
					public boolean evaluate(CacheEntryEvent<? extends String, ? extends TripPickUp> event)
							throws CacheEntryListenerException {
						return event.getEventType() == EventType.CREATED;
					}

				});

		contQuery.setLocalListener(new CacheEntryUpdatedListener<String, TripPickUp>() {
			IgniteCache<String, Car> carCache = ich.getCarCache();

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends TripPickUp>> events)
					throws CacheEntryListenerException {
				try {
					for (CacheEntryEvent<? extends String, ? extends TripPickUp> entry : events) {
						TripPickUp tpu = entry.getValue();
						TripRequest tripRequest = ich.getTripRequestCache().get(tpu.getTrip_id());
						TripQueue tripQueue = ich.getTripQueueCache().get(tpu.getTrip_id());

						SqlFieldsQuery query = new SqlFieldsQuery(
								"update car set locationId = ?, lastUpdate = ?, queuedTripId = null, tripId = ? where tripId = ? or queuedTripId = ?");
						query.setArgs(tpu.getPULocationID(), tpu.getPickup_datetime(), tpu.getTrip_id(),
								tpu.getTrip_id(), tpu.getTrip_id());
						long updated = (long) carCache.query(query).getAll().get(0).get(0);

						query = new SqlFieldsQuery("SELECT REGISTRATION FROM CAR WHERE tripId = ?");
						query.setArgs(tpu.getTrip_id());

						List<List<?>> all = carCache.query(query).getAll();
						String reg = "";

						if (all.size() > 0) {
							reg = (String) all.get(0).get(0);
							// System.out.printf("Pickup [%s] [%s] %s %s %d\n", reg, tpu.getTrip_id(),
							// new Boolean(tripRequest != null).toString(),
							// new Boolean(tripQueue != null).toString(), updated);
						} else {
							System.err.printf("Pickup [%s] [%s] %s %s %d\n", reg, tpu.getTrip_id(),
									Boolean.valueOf(tripRequest != null).toString(),
									Boolean.valueOf(tripQueue != null).toString(), updated);
						}

						ich.getTripRequestCache().remove(tpu.getTrip_id());
						ich.getTripQueueCache().remove(tpu.getTrip_id());
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		});
		ich.getTripPickUpCache().query(contQuery);
	}

	private void setupTripCompleteProcessor() {
		ContinuousQuery<String, Trip> contQuery = new ContinuousQuery<>();

		contQuery.setRemoteFilterFactory(
				(Factory<? extends CacheEntryEventFilter<String, Trip>>) () -> new CacheEntryEventFilter<String, Trip>() {

					@Override
					public boolean evaluate(CacheEntryEvent<? extends String, ? extends Trip> event)
							throws CacheEntryListenerException {
						return event.getEventType() == EventType.CREATED;
					}

				});

		contQuery.setLocalListener(new CacheEntryUpdatedListener<String, Trip>() {

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends Trip>> events)
					throws CacheEntryListenerException {
				try {
					for (CacheEntryEvent<? extends String, ? extends Trip> entry : events) {
						Trip trip = entry.getValue();

						// System.out.printf("%s - %s\n", trip.getTrip_id(),
						// entry.getEventType().name());

						SqlFieldsQuery regQuery = new SqlFieldsQuery("SELECT REGISTRATION FROM CAR WHERE TRIPID = ?");
						regQuery.setArgs(trip.getTrip_id());
						List<List<?>> all = ich.getCarCache().query(regQuery).getAll();
						if (all.size() > 0) {
							List<?> res = all.get(0);
							String reg = (String) res.get(0);
							Car car = ich.getCarCache().get(reg);

							trip.setRegistration(reg);
							ich.getTripCache().put(trip.getTrip_id(), trip);

							car.setTripId(null);
							car.setLocationId(trip.getDOLocationID());
							car.setLastUpdate(trip.getDropoff_datetime());

							/*
							 * String qTtripId = car.getQueuedTripId(); if (qTtripId != null) { TripQueue
							 * qTtrip = ich.getTripQueueCache().get(qTtripId); if (qTtrip == null) {
							 * System.err.println("No TripQueue! - " + qTtripId); } else {
							 * car.setTripId(qTtripId); car.setLocationId(qTtrip.getPickUpLocationId());
							 * car.setLastUpdate(qTtrip.getPickUpTime()); car.setQueuedTripId(null);
							 * car.setDropOffLocationId(trip.getDOLocationID());
							 * car.setDropOffTime(trip.getDropoff_datetime()); } }
							 */
							ich.getTripPickUpCache().remove(trip.getTrip_id());
							ich.getCarCache().put(reg, car);
						} else {
							System.err.println("Didn't find car - " + trip.getTrip_id());
						}
						ich.getTripPickUpCache().remove(trip.getTrip_id());
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		});
		ich.getTripCache().query(contQuery);
	}

	public class Reporter implements Runnable {

		private IgniteClientHelper ich;

		public Reporter(IgniteClientHelper ich) {
			this.ich = ich;
		}

		@Override
		public void run() {
			try {
				int tripRequestSize = ich.getTripRequestCache().size();
				int tripPickUpSize = ich.getTripPickUpCache().size();
				int tripQueueSize = ich.getTripQueueCache().size();
				int carSize = ich.getCarCache().size();
				int tripSize = ich.getTripCache().size();
				System.out.println(currentTime.toString());

				String qry = "select count(*) from car where TRIPID IS NULL";
				Long free = (Long) ich.getCarCache().query(new SqlFieldsQuery(qry)).getAll().get(0).get(0);

				System.out.println("+--------+--------+--------+--------+--------+--------+--------+--------+");
				System.out.println("|Request |PickUp  |Queue   |Total   |Complete|Cars    |Free Car|Times   |");
				System.out.println("+--------+--------+--------+--------+--------+--------+--------+--------+");
				System.out.printf("|%8d|%8d|%8d|%8d|%8d|%8d|%8d|%8d|\n", tripRequestSize, tripPickUpSize, tripQueueSize,
						(tripRequestSize + tripPickUpSize + tripQueueSize), tripSize, carSize, free, times.size());
				System.out.println("+--------+--------+--------+--------+--------+--------+--------+--------+");
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}

	}

}
