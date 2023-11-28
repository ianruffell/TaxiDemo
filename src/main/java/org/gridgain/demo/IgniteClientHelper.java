package org.gridgain.demo;

import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cluster.ClusterState.ACTIVE;
import static org.apache.ignite.configuration.DeploymentMode.CONTINUOUS;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryBasicNameMapper;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.gridgain.demo.model.Car;
import org.gridgain.demo.model.Trip;
import org.gridgain.demo.model.TripOnScene;
import org.gridgain.demo.model.TripPickUp;
import org.gridgain.demo.model.TripQueue;
import org.gridgain.demo.model.TripRequest;
import org.gridgain.demo.model.Zone;

public class IgniteClientHelper implements AutoCloseable {

	public static final String DATA_REGION = "MyDataRegion";
	public static final String SQL_SCHEMA = "PUBLIC";

	public static final String ZONE_CACHE_NAME = "Zone";
	public static final String CAR_CACHE_NAME = "Car";
	public static final String TRIP_REQUEST_CACHE_NAME = "TripRequest";
	public static final String TRIP_ON_SCENE_CACHE_NAME = "TripOnScene";
	public static final String TRIP_PICKUP_CACHE_NAME = "TripPickUp";
	public static final String TRIP_QUEUE_CACHE_NAME = "TripQueue";
	public static final String TRIP_CACHE_NAME = "Trip";

	private final Ignite ignite;
	private final IgniteCache<Integer, Zone> zoneCache;
	private final IgniteCache<String, Car> carCache;
	private final IgniteCache<String, TripRequest> tripRequestCache;
	private final IgniteCache<String, TripOnScene> tripOnSceneCache;
	private final IgniteCache<String, TripPickUp> tripPickUpCache;
	private final IgniteCache<String, TripQueue> tripQueueCache;
	private final IgniteCache<String, Trip> tripCache;

	public static void main(String args[]) throws Exception {
		try (IgniteClientHelper ich = new IgniteClientHelper()) {
			System.out.println("IgniteClientHelper");
		}
	}

	public IgniteClientHelper() throws Exception {
		this(true);
	}

	public IgniteClientHelper(boolean destroyCaches) throws Exception {
		System.setProperty("java.net.preferIPv4Stack", "true");

		IgniteConfiguration cfg = new IgniteConfiguration();
		cfg.setClientMode(true);
		cfg.setPeerClassLoadingEnabled(true);
		cfg.setDeploymentMode(CONTINUOUS);

		TcpDiscoverySpi tcpDiscoverySpi = new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi();
		TcpDiscoveryVmIpFinder tcpDiscoveryVmIpFinder = new org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder();
		ArrayList<String> list = new ArrayList<String>();
		list.add("127.0.0.1:47500..47510");

		tcpDiscoveryVmIpFinder.setAddresses(list);
		tcpDiscoverySpi.setIpFinder(tcpDiscoveryVmIpFinder);

		cfg.setDiscoverySpi(tcpDiscoverySpi);

		BinaryConfiguration binaryConfiguration = new BinaryConfiguration();
		BinaryBasicNameMapper nameMapper = new BinaryBasicNameMapper();
		nameMapper.setSimpleName(true);
		binaryConfiguration.setNameMapper(nameMapper);
		cfg.setBinaryConfiguration(binaryConfiguration);

		ignite = Ignition.start(cfg);
		ignite.cluster().state(ACTIVE);
		ignite.cluster().tag("Demo Cluster");

		if (destroyCaches) {
			System.out.println("Deleting Caches...");

			ignite.destroyCache(ZONE_CACHE_NAME);
			ignite.destroyCache(CAR_CACHE_NAME);
			ignite.destroyCache(TRIP_REQUEST_CACHE_NAME);
			ignite.destroyCache(TRIP_ON_SCENE_CACHE_NAME);
			ignite.destroyCache(TRIP_PICKUP_CACHE_NAME);
			ignite.destroyCache(TRIP_QUEUE_CACHE_NAME);
			ignite.destroyCache(TRIP_CACHE_NAME);

			System.out.println("Creating Caches...");
		}
		zoneCache = ignite.getOrCreateCache(new ZoneCacheConfiguration<Integer, Zone>());
		carCache = ignite.getOrCreateCache(new CarCacheConfiguration<String, Car>());
		tripRequestCache = ignite.getOrCreateCache(new TripRequestCacheConfiguration<String, TripRequest>());
		tripOnSceneCache = ignite.getOrCreateCache(new TripOnSceneCacheConfiguration<String, TripOnScene>());
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

	public IgniteCache<String, TripOnScene> getTripOnSceneCache() {
		return tripOnSceneCache;
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
	public static class TripOnSceneCacheConfiguration<K, V> extends CacheConfiguration<String, TripOnScene> {

		private static final long serialVersionUID = 0L;

		public TripOnSceneCacheConfiguration() {
			// Set required cache configuration properties.
			setName(TRIP_ON_SCENE_CACHE_NAME);
			setIndexedTypes(String.class, TripOnScene.class);
			setBackups(1);
			setCacheMode(PARTITIONED);
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
