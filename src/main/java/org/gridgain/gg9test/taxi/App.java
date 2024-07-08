package org.gridgain.gg9test.taxi;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

import org.apache.avro.generic.GenericRecord;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.apache.ignite.sql.Statement;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.TableRowEvent;
import org.apache.ignite.table.TableRowEventBatch;
import org.apache.ignite.table.mapper.Mapper;
import org.apache.parquet.avro.AvroParquetReader;
import org.gridgain.gg9test.taxi.model.Car;
import org.gridgain.gg9test.taxi.model.Trip;
import org.gridgain.gg9test.taxi.model.TripPickUp;
import org.gridgain.gg9test.taxi.model.TripQueue;
import org.gridgain.gg9test.taxi.model.TripRequest;

public class App implements Runnable {

	public static final String TRIP_DATA_FILE = "src/main/resources/fhvhv_tripdata_2023-07.parquet";
	private static final int SCALE_FACTOR = 5;
	private static final int NUM_RECORDS = 1000000;
	public static final int MAX_CARS = 13000;

	ArrayListValuedHashMap<Long, Trip> request = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Long, Trip> pickup = new ArrayListValuedHashMap<>();
	ArrayListValuedHashMap<Long, Trip> dropOff = new ArrayListValuedHashMap<>();
	List<Long> times = Collections.synchronizedList(new LinkedList<Long>());

	private Long currentTime = 1688161860000l;
	private Table carTable;
	private Table tripTable;
	private Table tripPickUpTable;
	private Table tripQueueTable;
	private Table tripRequestTable;
	private KeyValueView<String, Car> carKVView;
	private KeyValueView<String, Trip> tripKVView;
	private KeyValueView<String, TripPickUp> tripPickUpKVView;
	private KeyValueView<String, TripQueue> tripQueueKVView;
	private KeyValueView<String, TripRequest> tripRequestKVView;
	private RecordView<Car> carRView;
	private RecordView<Trip> tripRView;
	private RecordView<TripPickUp> tripPickUpRView;
	private RecordView<TripQueue> tripQueueRView;
	private RecordView<TripRequest> tripRequestRView;
	private IgniteClient ignite;

	public static void main(String[] args) throws Exception {
		new App();
	}

	public App() throws Exception {
		long start = System.currentTimeMillis();
		ignite = IgniteClient.builder().addresses("127.0.0.1:10800").build();
		System.out.printf("Client connected in %dms\n", System.currentTimeMillis() - start);
		
		// Drop Tables
		System.out.printf("Dropping tables - %s", Car.TABLE_NAME);
		Statement query = ignite.sql().createStatement("DROP TABLE IF EXISTS " + Car.TABLE_NAME);
		ignite.sql().execute(null, query);
		query = ignite.sql().createStatement("DROP TABLE IF EXISTS " + Trip.TABLE_NAME);
		System.out.printf(", %s", Trip.TABLE_NAME);
		ignite.sql().execute(null, query);
		query = ignite.sql().createStatement("DROP TABLE IF EXISTS " + TripRequest.TABLE_NAME);
		System.out.printf(", %s", TripRequest.TABLE_NAME);
		ignite.sql().execute(null, query);
		query = ignite.sql().createStatement("DROP TABLE IF EXISTS " + TripPickUp.TABLE_NAME);
		System.out.printf(", %s", TripPickUp.TABLE_NAME);
		ignite.sql().execute(null, query);
		query = ignite.sql().createStatement("DROP TABLE IF EXISTS " + TripQueue.TABLE_NAME);
		System.out.printf(", %s\n", TripQueue.TABLE_NAME);
		ignite.sql().execute(null, query);
		
		
		System.out.printf("Creating tables - %s", Car.TABLE_NAME);
		ignite.catalog().createTable(Car.class);
		carTable = ignite.tables().table(Car.TABLE_NAME);
		carKVView = carTable.keyValueView(Mapper.of(String.class), Mapper.of(Car.class));
		carRView = carTable.recordView(Mapper.of(Car.class));

		System.out.printf(", %s", Trip.TABLE_NAME);
		ignite.catalog().createTable(Trip.class);
		tripTable = ignite.tables().table(Trip.TABLE_NAME);
		tripKVView = tripTable.keyValueView(Mapper.of(String.class), Mapper.of(Trip.class));
		tripRView = tripTable.recordView(Mapper.of(Trip.class));

		System.out.printf(", %s", TripPickUp.TABLE_NAME);
		ignite.catalog().createTable(TripPickUp.class);
		tripPickUpTable = ignite.tables().table(TripPickUp.TABLE_NAME);
		tripPickUpKVView = tripPickUpTable.keyValueView(Mapper.of(String.class), Mapper.of(TripPickUp.class));
		tripPickUpRView = tripPickUpTable.recordView(Mapper.of(TripPickUp.class));

		System.out.printf(", %s", TripQueue.TABLE_NAME);
		ignite.catalog().createTable(TripQueue.class);
		tripQueueTable = ignite.tables().table(TripQueue.TABLE_NAME);
		tripQueueKVView = tripQueueTable.keyValueView(Mapper.of(String.class), Mapper.of(TripQueue.class));
		tripQueueRView = tripQueueTable.recordView(Mapper.of(TripQueue.class));

		System.out.printf(", %s", TripRequest.TABLE_NAME);
		ignite.catalog().createTable(TripRequest.class);
		tripRequestTable = ignite.tables().table(TripRequest.TABLE_NAME);
		tripRequestKVView = tripRequestTable.keyValueView(Mapper.of(String.class), Mapper.of(TripRequest.class));
		tripRequestRView = tripRequestTable.recordView(Mapper.of(TripRequest.class));

		System.out.print("Setup Continuous Queries ...");
		System.out.print(" TripRequest");
		tripRequestRView.queryContinuously(new TripRequestSubscriber(), null);
		System.out.print(", TripPickup");
		tripPickUpRView.queryContinuously(new TripPickupSubscriber(), null);
		System.out.print(", TripComplete");
		tripRView.queryContinuously(new TripCompleteSubscriber(), null);
		System.out.println(" Done");

		Set<Long> timesSet = new HashSet<>();

		int count = 0;
		GenericRecord record;
		AvroParquetReader<GenericRecord> reader = getTripReader();
		while ((record = reader.read()) != null) {
			Trip trip = Trip.fromRecord(record);

			if (TripValidator.isValid(trip)) {
				Long req = trip.getRequest_datetime();
				Long pu = trip.getPickup_datetime();
				Long dof = trip.getDropoff_datetime();

				if (req < pu && pu < dof) {
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
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Reporter(ignite), 0, 2, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		try {
			currentTime = times.remove(0);

			List<Trip> list = request.get(currentTime);
			for (Trip trip : list) {
				TripRequest tr = trip.toTripRequest();
				// System.out.println("Request : " + tr);
				tripRequestKVView.put(null, tr.getTripId(), tr);
			}
			request.remove(currentTime);

			list = pickup.get(currentTime);
			for (Trip trip : list) {
				TripPickUp tup = trip.toTripPickUp();
				// System.out.println("Pickup : " + tup);
				tripPickUpKVView.put(null, tup.getTripId(), tup);
			}
			pickup.remove(currentTime);

			list = dropOff.get(currentTime);
			for (Trip trip : list) {
				// System.out.println("Drop Off: " + trip);
				tripKVView.put(null, trip.getTrip_id(), trip);
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

	public Table getCarTable() {
		return carTable;
	}

	public void setCarTable(Table carTable) {
		this.carTable = carTable;
	}

	public Table getTripTable() {
		return tripTable;
	}

	public void setTripTable(Table tripTable) {
		this.tripTable = tripTable;
	}

	public Table getTripPickUpTable() {
		return tripPickUpTable;
	}

	public void setTripPickUpTable(Table tripPickUpTable) {
		this.tripPickUpTable = tripPickUpTable;
	}

	public Table getTripQueueTable() {
		return tripQueueTable;
	}

	public void setTripQueueTable(Table tripQueueTable) {
		this.tripQueueTable = tripQueueTable;
	}

	public Table getTripRequestTable() {
		return tripRequestTable;
	}

	public void setTripRequestTable(Table tripRequestTable) {
		this.tripRequestTable = tripRequestTable;
	}

	public KeyValueView<String, TripRequest> getTripRequestKVView() {
		return tripRequestKVView;
	}

	public void setTripRequestKVView(KeyValueView<String, TripRequest> tripRequestKVView) {
		this.tripRequestKVView = tripRequestKVView;
	}

	public KeyValueView<String, Car> getCarKVView() {
		return carKVView;
	}

	public void setCarKVView(KeyValueView<String, Car> carKVView) {
		this.carKVView = carKVView;
	}

	public KeyValueView<String, Trip> getTripKVView() {
		return tripKVView;
	}

	public void setTripKVView(KeyValueView<String, Trip> tripKVView) {
		this.tripKVView = tripKVView;
	}

	public KeyValueView<String, TripQueue> getTripQueueKVView() {
		return tripQueueKVView;
	}

	public void setTripQueueKVView(KeyValueView<String, TripQueue> tripQueueKVView) {
		this.tripQueueKVView = tripQueueKVView;
	}

	public KeyValueView<String, TripPickUp> getTripPickUpKVView() {
		return tripPickUpKVView;
	}

	public void setTripPickUpKVView(KeyValueView<String, TripPickUp> tripPickUpKVView) {
		this.tripPickUpKVView = tripPickUpKVView;
	}

	public RecordView<Car> getCarRView() {
		return carRView;
	}

	public void setCarRView(RecordView<Car> carRView) {
		this.carRView = carRView;
	}

	public RecordView<Trip> getTripRView() {
		return tripRView;
	}

	public void setTripRView(RecordView<Trip> tripRView) {
		this.tripRView = tripRView;
	}

	public RecordView<TripPickUp> getTripPickUpRView() {
		return tripPickUpRView;
	}

	public void setTripPickUpRView(RecordView<TripPickUp> tripPickUpRView) {
		this.tripPickUpRView = tripPickUpRView;
	}

	public RecordView<TripQueue> getTripQueueRView() {
		return tripQueueRView;
	}

	public void setTripQueueRView(RecordView<TripQueue> tripQueueRView) {
		this.tripQueueRView = tripQueueRView;
	}

	public RecordView<TripRequest> getTripRequestRView() {
		return tripRequestRView;
	}

	public void setTripRequestRView(RecordView<TripRequest> tripRequestRView) {
		this.tripRequestRView = tripRequestRView;
	}

	public IgniteClient getIgnite() {
		return ignite;
	}

	public void setIgnite(IgniteClient ignite) {
		this.ignite = ignite;
	}

	private class TripRequestSubscriber implements Flow.Subscriber<TableRowEventBatch<TripRequest>> {

		private final CarFinder carFinder = new CarFinder();

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(TableRowEventBatch<TripRequest> batch) {
			List<TableRowEvent<TripRequest>> items = batch.rows();
			for (TableRowEvent<TripRequest> item : items) {
				System.out.println("onNext: " + item.type() + ", old=" + item.oldEntry() + ", new=" + item.entry());
				TripRequest tr = item.entry();

				Car car = carFinder.findCarForTrip(App.this, tr);
				if (car != null) {
					car.setTripId(tr.getTripId());
					car.setQueuedTripId(null);
				} else {
					car = carFinder.findCarForQueue(App.this, tr);
					if (car != null) {
						car.setQueuedTripId(tr.getTripId());
						TripQueue tripQueue = new TripQueue(car.getRegistration(), tr.getPickupLocationId(),
								tr.getDropoffLocationId(), tr.getPickupDatetime(), tr.getTripId());
						tripQueueKVView.put(null, tripQueue.getTripId(), tripQueue);
						tripRequestKVView.remove(null, tr.getTripId());
						// System.out.println("Add to Queue " + tripQueue.getTripId());
					} else {
						System.err.println("No car for queue!");
					}
				}
				carKVView.put(null, car.getRegistration(), car);
			}
		}

		@Override
		public void onError(Throwable throwable) {
			System.out.println("onError: " + throwable);
		}

		@Override
		public void onComplete() {
			System.out.println("onComplete");
		}

	}

	private class TripPickupSubscriber implements Flow.Subscriber<TableRowEventBatch<TripPickUp>> {

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(TableRowEventBatch<TripPickUp> batch) {
			List<TableRowEvent<TripPickUp>> items = batch.rows();
			for (TableRowEvent<TripPickUp> item : items) {
				System.out.println("onNext: " + item.type() + ", old=" + item.oldEntry() + ", new=" + item.entry());
				TripPickUp tpu = item.entry();
				TripRequest tripRequest = tripRequestKVView.get(null, tpu.getTripId());
				TripQueue tripQueue = tripQueueKVView.get(null, tpu.getTripId());

				Statement query = ignite.sql().createStatement("update car set locationId = ?, lastUpdate = ?, queuedTripId = null, tripId = ? where tripId = ? or queuedTripId = ?");
				ResultSet<SqlRow> resultSet = ignite.sql().execute(null, query, tpu.getPULocationID(), tpu.getPickupTime(), tpu.getTripId(), tpu.getTripId(), tpu.getTripId());
				long updated = resultSet.affectedRows();

				query = ignite.sql().createStatement("SELECT REGISTRATION FROM CAR WHERE tripId = ?");
				resultSet = ignite.sql().execute(null, query, tpu.getTripId());

				String reg = "";
				if (resultSet.hasNext()) {
					reg = resultSet.next().stringValue(0);
					System.out.printf("Pickup [%s] [%s] %s %s %d\n", reg, tpu.getTripId(),
					new Boolean(tripRequest != null).toString(),
					new Boolean(tripQueue != null).toString(), updated);
				} else {
					System.err.printf("Pickup [%s] [%s] %s %s %d\n", reg, tpu.getTripId(),
							new Boolean(tripRequest != null).toString(), new Boolean(tripQueue != null).toString(),
							updated);
				}

				tripRequestKVView.remove(null, tpu.getTripId());
				tripQueueKVView.remove(null, tpu.getTripId());
			}

		}

		@Override
		public void onError(Throwable throwable) {
			System.out.println("onError: " + throwable);
		}

		@Override
		public void onComplete() {
			System.out.println("onComplete");
		}

	}

	private class TripCompleteSubscriber implements Flow.Subscriber<TableRowEventBatch<Trip>> {

		private final CarFinder carFinder = new CarFinder();

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(TableRowEventBatch<Trip> batch) {
			List<TableRowEvent<Trip>> items = batch.rows();
			for (TableRowEvent<Trip> item : items) {
				System.out.println("onNext: " + item.type() + ", old=" + item.oldEntry() + ", new=" + item.entry());

				Trip trip = item.entry();

				// System.out.printf("%s - %s\n", trip.getTrip_id(),
				// entry.getEventType().name());

				Statement query = ignite.sql().createStatement("SELECT REGISTRATION FROM CAR WHERE TRIPID = ?");
				ResultSet<SqlRow> resultSet = ignite.sql().execute(null, query, trip.getTrip_id());
				if (resultSet.hasNext()) {
					String reg = resultSet.next().stringValue(0);
					Car car = carKVView.get(null, reg);

					trip.setRegistration(reg);
					tripKVView.put(null, trip.getTrip_id(), trip);

					car.setTripId(null);
					car.setLocationId(trip.getDOLocationID());
					car.setLastUpdate(trip.getDropoff_datetime());

					/*
					 * String qTtripId = car.getQueuedTripId(); if (qTtripId != null) { TripQueue
					 * qTtrip = ignite.getTripQueueCache().get(qTtripId); if (qTtrip == null) {
					 * System.err.println("No TripQueue! - " + qTtripId); } else {
					 * car.setTripId(qTtripId); car.setLocationId(qTtrip.getPickUpLocationId());
					 * car.setLastUpdate(qTtrip.getPickUpTime()); car.setQueuedTripId(null);
					 * car.setDropOffLocationId(trip.getDOLocationID());
					 * car.setDropOffTime(trip.getDropoff_datetime()); } }
					 */
					tripPickUpKVView.remove(null, trip.getTrip_id());
					carKVView.put(null, reg, car);
				} else {
					System.err.println("Didn't find car - " + trip.getTrip_id());
				}
				tripPickUpKVView.remove(null, trip.getTrip_id());
			}
		}
		
		@Override
		public void onError(Throwable throwable) {
			System.out.println("onError: " + throwable);
		}

		@Override
		public void onComplete() {
			System.out.println("onComplete");
		}
	}

	public class Reporter implements Runnable {

		private IgniteClient ignite;

		public Reporter(IgniteClient ignite) {
			this.ignite = ignite;
		}

		@Override
		public void run() {
			try {
				long tripRequestSize = size(TripRequest.TABLE_NAME);
				long tripPickUpSize = size(TripPickUp.TABLE_NAME);
				long tripQueueSize = size(TripQueue.TABLE_NAME);
				long carSize = size(Car.TABLE_NAME);
				long tripSize = size(Trip.TABLE_NAME);
				System.out.println(currentTime.toString());

				Statement query = ignite.sql().createStatement("select count(*) from car where TRIPID IS NULL");
				ResultSet<SqlRow> resultSet = ignite.sql().execute(null, query);
				Long free = resultSet.next().longValue(0);

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
	
	public long size(String table) {
		Statement query = ignite.sql().createStatement("select count(*) from " + table);
		ResultSet<SqlRow> resultSet = ignite.sql().execute(null, query);
		return resultSet.next().longValue(0);
	}

}
