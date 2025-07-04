package com.gridgain.demo;

import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cluster.ClusterState.ACTIVE;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.IgniteEvents;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;

import com.gridgain.demo.model.Car;
import com.gridgain.demo.model.Trip;
import com.gridgain.demo.model.TripPickUp;
import com.gridgain.demo.model.TripQueue;
import com.gridgain.demo.model.TripRequest;
import com.gridgain.demo.model.Zone;

public class IgniteClientHelper implements AutoCloseable {

	public static final String DATA_REGION = "MyDataRegion";
	public static final String SQL_SCHEMA = "PUBLIC";

	public static final String ZONE_CACHE_NAME = "Zone";
	public static final String CAR_CACHE_NAME = "Car";
	public static final String TRIP_REQUEST_CACHE_NAME = "TripRequest";
	public static final String TRIP_PICKUP_CACHE_NAME = "TripPickUp";
	public static final String TRIP_QUEUE_CACHE_NAME = "TripQueue";
	public static final String TRIP_CACHE_NAME = "Trip";

	private final Ignite ignite;
	private final IgniteCache<Integer, Zone> zoneCache;
	private final IgniteCache<String, Car> carCache;
	private final IgniteCache<String, TripRequest> tripRequestCache;
	private final IgniteCache<String, TripPickUp> tripPickUpCache;
	private final IgniteCache<String, TripQueue> tripQueueCache;
	private final IgniteCache<String, Trip> tripCache;

	public static IgniteClientHelper start() throws Exception {
		return new IgniteClientHelper(true);
	}

	public IgniteClientHelper(boolean destroyCaches) throws Exception {
		System.setProperty("IGNITE_QUIET", "true");
		System.setProperty("java.net.preferIPv4Stack", "true");

		IgniteConfiguration cfg = new AppConfiguration();
		cfg.setClientMode(true);

		ignite = Ignition.start(cfg);
		ignite.cluster().state(ACTIVE);
		ignite.cluster().tag("Demo Cluster");

		if (destroyCaches) {
			System.out.println("Deleting Caches...");

			ignite.destroyCache(ZONE_CACHE_NAME);
			ignite.destroyCache(CAR_CACHE_NAME);
			ignite.destroyCache(TRIP_REQUEST_CACHE_NAME);
			ignite.destroyCache(TRIP_PICKUP_CACHE_NAME);
			ignite.destroyCache(TRIP_QUEUE_CACHE_NAME);
			ignite.destroyCache(TRIP_CACHE_NAME);

			System.out.println("Creating Caches...");
		}
		zoneCache = ignite.getOrCreateCache(new ZoneCacheConfiguration<Integer, Zone>());
		carCache = ignite.getOrCreateCache(new CarCacheConfiguration<String, Car>());
		tripRequestCache = ignite.getOrCreateCache(new TripRequestCacheConfiguration<String, TripRequest>());
		tripPickUpCache = ignite.getOrCreateCache(new TripPickUpCacheConfiguration<String, TripPickUp>());
		tripQueueCache = ignite.getOrCreateCache(new TripQueueCacheConfiguration<String, TripQueue>());
		tripCache = ignite.getOrCreateCache(new TripCacheConfiguration<String, Trip>());

		if (destroyCaches) {
			try (IgniteDataStreamer<Integer, Zone> ds = ignite.dataStreamer(ZONE_CACHE_NAME)) {
				InputStream stream = IgniteClientHelper.class.getClassLoader()
						.getResourceAsStream("taxi_zone_lookup.csv");
				InputStreamReader in = new InputStreamReader(stream);
				@SuppressWarnings("deprecation")
				Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().withSkipHeaderRecord().parse(in);
				for (CSVRecord record : records) {
					Integer id = Integer.parseInt(record.get("LocationID"));
					String borough = record.get("Borough");
					String zone = record.get("Zone");
					String service_zone = record.get("service_zone");
					ds.addData(id, new Zone(id, borough, zone, service_zone));
				}
				System.out.println("Loaded Zones");
			}
		}
	}

	public Ignite getIgnite() {
		return ignite;
	}

	@Override
	public void close() throws Exception {
		ignite.close();
	}

	public IgniteCache<Integer, Zone> getZoneCache() {
		return zoneCache;
	}

	public IgniteCache<String, Trip> getTripCache() {
		return tripCache;
	}

	public IgniteCache<String, Car> getCarCache() {
		return carCache;
	}

	public IgniteCache<String, TripRequest> getTripRequestCache() {
		return tripRequestCache;
	}

	public IgniteCache<String, TripPickUp> getTripPickUpCache() {
		return tripPickUpCache;
	}

	public IgniteCache<String, TripQueue> getTripQueueCache() {
		return tripQueueCache;
	}

	public static class ZoneCacheConfiguration<K, V> extends CacheConfiguration<Integer, Zone> {

		private static final long serialVersionUID = 0L;

		public ZoneCacheConfiguration() {
			// Set required cache configuration properties.
			setName(ZONE_CACHE_NAME);
			setIndexedTypes(Integer.class, Zone.class);
			setCacheMode(REPLICATED);
			setDataRegionName(DATA_REGION);
			setSqlSchema(SQL_SCHEMA);
			setStatisticsEnabled(true);
		}
	}
	public static class CarCacheConfiguration<K, V> extends CacheConfiguration<String, Car> {

		private static final long serialVersionUID = 0L;

		public CarCacheConfiguration() {
			// Set required cache configuration properties.
			setName(CAR_CACHE_NAME);
			setIndexedTypes(String.class, Car.class);
			setCacheMode(PARTITIONED);
			setBackups(1);
			setDataRegionName(DATA_REGION);
			setSqlSchema(SQL_SCHEMA);
			setStatisticsEnabled(true);
		}
	}

	public static class TripRequestCacheConfiguration<K, V> extends CacheConfiguration<String, TripRequest> {

		private static final long serialVersionUID = 0L;

		public TripRequestCacheConfiguration() {
			// Set required cache configuration properties.
			setName(TRIP_REQUEST_CACHE_NAME);
			setIndexedTypes(String.class, TripRequest.class);
			setCacheMode(PARTITIONED);
			setBackups(1);
			setDataRegionName(DATA_REGION);
			setSqlSchema(SQL_SCHEMA);
			setStatisticsEnabled(true);
		}
	}

	public static class TripPickUpCacheConfiguration<K, V> extends CacheConfiguration<String, TripPickUp> {

		private static final long serialVersionUID = 0L;

		public TripPickUpCacheConfiguration() {
			// Set required cache configuration properties.
			setName(TRIP_PICKUP_CACHE_NAME);
			setIndexedTypes(String.class, TripPickUp.class);
			setBackups(1);
			setCacheMode(PARTITIONED);
			setDataRegionName(DATA_REGION);
			setSqlSchema(SQL_SCHEMA);
			setStatisticsEnabled(true);
		}
	}

	public static class TripCacheConfiguration<K, V> extends CacheConfiguration<String, Trip> {

		private static final long serialVersionUID = 0L;

		public TripCacheConfiguration() {
			// Set required cache configuration properties.
			setName(TRIP_CACHE_NAME);
			setIndexedTypes(String.class, Trip.class);
			setBackups(1);
			setCacheMode(PARTITIONED);
			setDataRegionName(DATA_REGION);
			setSqlSchema(SQL_SCHEMA);
			setStatisticsEnabled(true);
		}
	}
	
	public static class TripQueueCacheConfiguration<K, V> extends CacheConfiguration<String, TripQueue> {

		private static final long serialVersionUID = 0L;

		public TripQueueCacheConfiguration() {
			// Set required cache configuration properties.
			setName(TRIP_QUEUE_CACHE_NAME);
			setIndexedTypes(String.class, TripQueue.class);
			setBackups(1);
			setCacheMode(PARTITIONED);
			setDataRegionName(DATA_REGION);
			setSqlSchema(SQL_SCHEMA);
			setStatisticsEnabled(true);
		}
	}


}
