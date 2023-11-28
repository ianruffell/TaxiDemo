package org.gridgain.demo;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;

import org.apache.avro.generic.GenericRecord;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.parquet.avro.AvroParquetReader;
import org.gridgain.demo.model.Car;
import org.gridgain.demo.model.Trip;
import org.gridgain.demo.model.TripOnScene;
import org.gridgain.demo.model.TripPickUp;
import org.gridgain.demo.model.TripQueue;
import org.gridgain.demo.model.TripRequest;
import org.gridgain.demo.model.TripValidator;

public class DataGen implements Runnable {

	private static final int SCALE_FACTOR = 25;
	private static final int NUM_RECORDS = 1000000;

	
	ArrayListValuedHashMap<Timestamp, Trip> request = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Timestamp, Trip> onScene = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Timestamp, Trip> pickup = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Timestamp, Trip> dropOff = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Timestamp, TripQueue> queue = new ArrayListValuedHashMap<>();
	TreeSet<Timestamp> times = new TreeSet<>();

	private IgniteClientHelper ich;

	private Timestamp currentTime = Timestamp.from(Instant.ofEpochMilli(1688161860000l));

	public static void main(String[] args) throws Exception {
		new DataGen();
	}

	public DataGen() throws Exception {
		@SuppressWarnings("deprecation")
		AvroParquetReader<GenericRecord> reader = new AvroParquetReader<GenericRecord>(new Configuration(),
				new Path("src/main/resources/fhvhv_tripdata_2023-07.parquet"));

		int count = 0;
		GenericRecord record;
		while ((record = reader.read()) != null) {
			Trip trip = Trip.fromRecord(record);

			if (TripValidator.isValid(trip)) {
				Timestamp req = trip.getRequest_datetime();
				times.add(req);
				request.put(req, trip);

				if (record.get("on_scene_datetime") != null) {
					Timestamp os = trip.getOn_scene_datetime();
					times.add(os);
					onScene.put(os, trip);
				}

				Timestamp pu = trip.getPickup_datetime();
				times.add(pu);
				pickup.put(pu, trip);

				Timestamp dof = trip.getDropoff_datetime();
				times.add(dof);
				dropOff.put(dof, trip);

				count++;
				if (count % 10000 == 0) {
					System.out.println("Read: " + count);
				}
				if (count % NUM_RECORDS == 0) {
					break;
				}
			}
		}

		ich = new IgniteClientHelper(true);

		setupTripRequestProcessor();
		setupTripOnSceneProcessor();
		setupTripPickUpProcessor();
		setupTripCompleteProcessor();

		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this, 0, (1000 / SCALE_FACTOR),
				TimeUnit.MILLISECONDS);
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Reporter(ich), 0, 2, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		try {
			currentTime = times.pollFirst();

			List<TripQueue> queueList = queue.get(currentTime);
			IgniteCache<String, Car> carCache = ich.getCarCache();
			IgniteCache<String, TripQueue> tripQueueCache = ich.getTripQueueCache();
			for (TripQueue tripQueue : queueList) {
				Car car = carCache.get(tripQueue.getRegistration());
				car.setIsCommitted(true);
				car.setIsQueued(false);
				car.setTripId(tripQueue.getTripId());
				carCache.put(tripQueue.getRegistration(), car);
				tripQueueCache.remove(tripQueue.getTripId());
			}

			List<Trip> list = request.get(currentTime);
			for (Trip trip : list) {
				TripRequest tr = trip.toTripRequest();
				// System.out.println("Request : " + tr);
				ich.getTripRequestCache().put(tr.getTrip_id(), tr);
				request.remove(currentTime);
			}

			list = onScene.get(currentTime);
			for (Trip trip : list) {
				TripOnScene tos = trip.toTripOnScene();
				// System.out.println("On Scene: " + tos);
				ich.getTripOnSceneCache().put(tos.getTrip_id(), tos);
				onScene.remove(currentTime);
			}

			list = pickup.get(currentTime);
			for (Trip trip : list) {
				TripPickUp tup = trip.toTripPickUp();
				// System.out.println("Pickup : " + tup);
				ich.getTripPickUpCache().put(tup.getTrip_id(), tup);
				pickup.remove(currentTime);
			}

			list = dropOff.get(currentTime);
			for (Trip trip : list) {
				// System.out.println("Drop Off: " + trip);
				ich.getTripCache().put(trip.getTrip_id(), trip);
				dropOff.remove(currentTime);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

	}

	private void setupTripRequestProcessor() {
		IgniteCache<String, TripRequest> cache = ich.getTripRequestCache();
		IgniteCache<String, Car> carCache = ich.getCarCache();

		ContinuousQuery<String, TripRequest> query = new ContinuousQuery<>();

		CarFinder carFinder = new CarFinder();

		query.setLocalListener(new CacheEntryUpdatedListener<String, TripRequest>() {

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends TripRequest>> events)
					throws CacheEntryListenerException {
				for (CacheEntryEvent<? extends String, ? extends TripRequest> entry : events) {
					TripRequest tr = entry.getValue();
					Car car = carFinder.findCarForTrip(ich, tr);
					if (car != null) {
						car.setTripId(tr.getTrip_id());
						carCache.put(car.getRegistration(), car);
					} else {
						TripQueue tripQueue = null;
						while (tripQueue == null) {
							tripQueue = carFinder.getTripQueue(ich, tr);
							if (tripQueue == null) {
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						queue.put(tripQueue.getPickUpTime(), tripQueue);
						ich.getTripQueueCache().put(tripQueue.getTripId(), tripQueue);
					}
				}
			}
		});

		cache.query(query);
	}

	private void setupTripOnSceneProcessor() {
		IgniteCache<String, TripOnScene> tosCache = ich.getTripOnSceneCache();
		IgniteCache<String, Car> carCache = ich.getCarCache();

		ContinuousQuery<String, TripOnScene> query = new ContinuousQuery<>();

		query.setLocalListener(new CacheEntryUpdatedListener<String, TripOnScene>() {

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends TripOnScene>> events)
					throws CacheEntryListenerException {
				try {
					for (CacheEntryEvent<? extends String, ? extends TripOnScene> entry : events) {
						TripOnScene tos = entry.getValue();
						// System.out.println(tos);
						SqlFieldsQuery query = new SqlFieldsQuery(
								"update car set locationId = ?, lastUpdate = ? where tripId = ?");
						query.setArgs(tos.getPULocationID(), tos.getOn_scene_datetime(), tos.getTrip_id());
						carCache.query(query);
						// List<List<?>> all = carCache.query(query).getAll();
						// System.out.println("TripOnScene Updated car for tripId " + tos.getTrip_id() +
						// " - " + all.size());
						ich.getTripRequestCache().remove(tos.getTrip_id());
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		});
		tosCache.query(query);
	}

	private void setupTripPickUpProcessor() {
		IgniteCache<String, TripPickUp> tpuCache = ich.getTripPickUpCache();
		IgniteCache<String, Car> carCache = ich.getCarCache();

		ContinuousQuery<String, TripPickUp> query = new ContinuousQuery<>();

		query.setLocalListener(new CacheEntryUpdatedListener<String, TripPickUp>() {

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends TripPickUp>> events)
					throws CacheEntryListenerException {
				try {
					for (CacheEntryEvent<? extends String, ? extends TripPickUp> entry : events) {
						TripPickUp tpu = entry.getValue();
						// System.out.println(tpu);
						SqlFieldsQuery query = new SqlFieldsQuery(
								"update car set locationId = ?, lastUpdate = ? where tripId = ?");
						query.setArgs(tpu.getPULocationID(), tpu.getPickup_datetime(), tpu.getTrip_id());
						carCache.query(query);
						// List<List<?>> all = carCache.query(query).getAll();
						// System.out.println("TripPickUp Updated car for tripId " + tpu.getTrip_id() +
						// " - " + all.size());
						ich.getTripRequestCache().remove(tpu.getTrip_id());
						ich.getTripOnSceneCache().remove(tpu.getTrip_id());
						ich.getTripQueueCache().remove(tpu.getTrip_id());
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		});
		tpuCache.query(query);
	}

	private void setupTripCompleteProcessor() {
		IgniteCache<String, Trip> tripCache = ich.getTripCache();
		IgniteCache<String, Car> carCache = ich.getCarCache();

		ContinuousQuery<String, Trip> query = new ContinuousQuery<>();

		query.setLocalListener(new CacheEntryUpdatedListener<String, Trip>() {

			@Override
			public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends Trip>> events)
					throws CacheEntryListenerException {
				try {
					for (CacheEntryEvent<? extends String, ? extends Trip> entry : events) {
						Trip trip = entry.getValue();

						if (trip.getRegistration() == null) {
							SqlFieldsQuery regQuery = new SqlFieldsQuery(
									"select registration from car where tripId = ?");
							regQuery.setArgs(trip.getTrip_id());
							List<List<?>> all = carCache.query(regQuery).getAll();
							if (all.size() == 0) {
								// System.err.println("Can't find car for trip id : " + trip.getTrip_id());
								// System.err.println(trip);
							} else {
								List<?> res = all.get(0);
								String reg = (String) res.get(0);
								Car car = carCache.get(reg);

								trip.setRegistration(reg);
								tripCache.put(trip.getTrip_id(), trip);
								// System.out.println("Trip Updated car reg for tripId " + trip.getTrip_id() + "
								// - " + all.size());

								car.setIsCommitted(false);
								car.setTripId(null);
								carCache.put(reg, car);
							}
							ich.getTripPickUpCache().remove(trip.getTrip_id());
						}
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}

			}
		});
		tripCache.query(query);
	}

	public class Reporter implements Runnable {

		private IgniteClientHelper ich;

		public Reporter(IgniteClientHelper ich) {
			this.ich = ich;
		}

		@Override
		public void run() {
			int tripRequestSize = ich.getTripRequestCache().size();
			int tripOnSceneSize = ich.getTripOnSceneCache().size();
			int tripPickUpSize = ich.getTripPickUpCache().size();
			int tripQueueSize = ich.getTripQueueCache().size();
			int carSize = ich.getCarCache().size();
			int tripSize = ich.getTripCache().size();
			System.out.println(currentTime.toString());

			System.out.println("+--------+--------+--------+--------+--------+--------+--------+");
			System.out.println("|Request |OnScene |PickUp  |Queue   |Complete|Cars    |Times   |");
			System.out.println("+--------+--------+--------+--------+--------+--------+--------+");
			System.out.printf("|%8d|%8d|%8d|%8d|%8d|%8d|%8d|\n", tripRequestSize, tripOnSceneSize, tripPickUpSize,
					tripQueueSize, tripSize, carSize, times.size());
			System.out.println("+--------+--------+--------+--------+--------+--------+--------+");
		}

	}

}
