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
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.TableRowEvent;
import org.apache.ignite.table.TableRowEventBatch;
import org.apache.ignite.table.TableRowEventType;
import org.apache.ignite.table.mapper.Mapper;
import org.apache.ignite.tx.Transaction;
import org.apache.parquet.avro.AvroParquetReader;
import org.gridgain.gg9test.taxi.model.Car;
import org.gridgain.gg9test.taxi.model.Trip;
import org.gridgain.gg9test.taxi.model.TripPickUp;
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
	private RecordView<Car> carRView;
	private RecordView<Trip> tripRView;
	private RecordView<TripPickUp> tripPickUpRView;
	private RecordView<TripRequest> tripRequestRView;
	private IgniteClient ignite;

	public static void main(String[] args) throws Exception {
		new App();
	}

	public App() throws Exception {
		long start = System.currentTimeMillis();
		ignite = IgniteClient.builder().addresses("127.0.0.1:10800", "127.0.0.1:10801", "127.0.0.1:10802").build();
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

		// ignite.catalog().dropTable(TRIP_DATA_FILE);

		System.out.printf("Creating tables - %s", Car.TABLE_NAME);
		ignite.catalog().createTable(Car.class);
		carTable = ignite.tables().table(Car.TABLE_NAME);
		carRView = carTable.recordView(Mapper.of(Car.class));

		System.out.printf(", %s", Trip.TABLE_NAME);
		ignite.catalog().createTable(Trip.class);
		tripTable = ignite.tables().table(Trip.TABLE_NAME);
		tripRView = tripTable.recordView(Mapper.of(Trip.class));

		System.out.printf(", %s", TripPickUp.TABLE_NAME);
		ignite.catalog().createTable(TripPickUp.class);
		tripPickUpTable = ignite.tables().table(TripPickUp.TABLE_NAME);
		tripPickUpRView = tripPickUpTable.recordView(Mapper.of(TripPickUp.class));

		System.out.printf(", %s", TripRequest.TABLE_NAME);
		ignite.catalog().createTable(TripRequest.class);
		tripRequestTable = ignite.tables().table(TripRequest.TABLE_NAME);
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
				tripRequestRView.insert(null, tr);
				tripRView.insert(null, trip);
			}
			request.remove(currentTime);

			list = pickup.get(currentTime);
			for (Trip trip : list) {
				TripPickUp tup = trip.toTripPickUp();
				// System.out.println("Pickup : " + tup);
				tripPickUpRView.insert(null, tup);
			}
			pickup.remove(currentTime);

			list = dropOff.get(currentTime);
			for (Trip trip : list) {
				// System.out.println("Drop Off: " + trip);
				getTripRView().insert(null, trip);
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

	public RecordView<TripRequest> getTripRequestRView() {
		return tripRequestRView;
	}

	public void setTripRequestRView(RecordView<TripRequest> tripRequestRView) {
		this.tripRequestRView = tripRequestRView;
	}

	public IgniteClient getIgnite() {
		return ignite;
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
				// System.out.println("TripRequest: " + item.type() + ", old=" + item.oldEntry()
				// + ", new=" + item.entry());
				TripRequest tr = item.entry();
				String tripId = tr.getTripId();

				Car car = carFinder.findCarForTrip(App.this, tr);
				car.setTripId(tripId);
				carRView.insert(null, car);

				Trip trip = tripRView.get(null, Trip.forId(tripId));
				trip.setRegistration(car.getRegistration());
				tripRView.replace(null, trip);
			}
		}

		@Override
		public void onError(Throwable throwable) {
			System.out.println("onError: " + throwable);
			throwable.printStackTrace();
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
				// System.out.println("TripPickup: " + item.type() + ", old=" + item.oldEntry()
				// + ", new=" + item.entry());

				TripPickUp tpu = item.entry();
				String tripId = tpu.getTripId();
				TripRequest tr = new TripRequest();
				tr.setTripId(tripId);
				TripRequest tripRequest = tripRequestRView.get(null, tr);

				Statement query = ignite.sql()
						.createStatement("update car set locationId = ?, lastUpdate = ? where tripId = ?");
				ResultSet<SqlRow> resultSet = ignite.sql().execute(null, query, tpu.getPULocationID(),
						tpu.getPickupTime(), tripId);
				long updated = resultSet.affectedRows();

				query = ignite.sql().createStatement("SELECT REGISTRATION FROM CAR WHERE tripId = ?");
				resultSet = ignite.sql().execute(null, query, tripId);

				String reg = "";
				if (resultSet.hasNext()) {
					reg = resultSet.next().stringValue(0);
					System.out.printf("Pickup [%s] [%s] %s %d\n", reg, tripId,
							Boolean.valueOf(tripRequest != null).toString(), updated);
				} else {
					System.err.printf("Pickup [%s] [%s] %s %d\n", reg, tripId,
							Boolean.valueOf(tripRequest != null).toString(), updated);
				}
				tripRequestRView.delete(null, tr);
			}

		}

		@Override
		public void onError(Throwable throwable) {
			System.out.println("onError: " + throwable);
			throwable.printStackTrace();
		}

		@Override
		public void onComplete() {
			System.out.println("onComplete");
		}

	}

	private class TripCompleteSubscriber implements Flow.Subscriber<TableRowEventBatch<Trip>> {

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(TableRowEventBatch<Trip> batch) {
			List<TableRowEvent<Trip>> items = batch.rows();
			for (TableRowEvent<Trip> item : items) {
				// System.out.println("TripComplete: " + item.type() + ", old=" +
				// item.oldEntry() + ", new=" + item.entry());

				Trip trip = item.entry();

				if (item.type() == TableRowEventType.CREATED) {
					//System.out.printf("%s - %s\n", trip.getTrip_id(), item.type());
					Transaction tx = ignite.transactions().begin();

					Statement query = ignite.sql().createStatement("SELECT REGISTRATION FROM CAR WHERE TRIPID = ?");
					ResultSet<SqlRow> resultSet = ignite.sql().execute(tx, query, trip.getTrip_id());
					if (resultSet.hasNext()) {
						String reg = resultSet.next().stringValue(0);
						Car car = carRView.get(tx, Car.forId(reg));

						car.setTripId(null);
						car.setLocationId(trip.getDOLocationID());
						car.setLastUpdate(trip.getDropoff_datetime());

						trip.setRegistration(reg);
						tripRView.replace(tx, Trip.forId(reg));
						System.out.printf("Delete PickUp %s\n", trip.getTrip_id());
						tripPickUpRView.delete(tx, TripPickUp.forId(trip.getTrip_id()));
						carRView.replace(tx, car);
					}
					tx.commit();
				}
			}
		}

		@Override
		public void onError(Throwable throwable) {
			System.out.println("onError: " + throwable);
			throwable.printStackTrace();
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
				long carSize = size(Car.TABLE_NAME);
				long tripSize = size(Trip.TABLE_NAME);
				System.out.println(currentTime.toString());

				Statement query = ignite.sql().createStatement("select count(*) from car where TRIPID IS NULL");
				ResultSet<SqlRow> resultSet = ignite.sql().execute(null, query);
				Long free = resultSet.next().longValue(0);

				System.out.println("+--------+--------+--------+--------+--------+--------+--------+");
				System.out.println("|Request |PickUp  |Total   |Complete|Cars    |Free Car|Times   |");
				System.out.println("+--------+--------+--------+--------+--------+--------+--------+");
				System.out.printf("|%8d|%8d|%8d|%8d|%8d|%8d|%8d|\n", tripRequestSize, tripPickUpSize,
						(tripRequestSize + tripPickUpSize), tripSize, carSize, free, times.size());
				System.out.println("+--------+--------+--------+--------+--------+--------+--------+");
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
