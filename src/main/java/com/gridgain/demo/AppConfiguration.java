package com.gridgain.demo;

import static org.apache.ignite.configuration.DataPageEvictionMode.RANDOM_2_LRU;
import static org.apache.ignite.configuration.DeploymentMode.CONTINUOUS;

import java.util.ArrayList;

import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

public class AppConfiguration extends IgniteConfiguration {

	public AppConfiguration() {
		super();
		
		System.setProperty("IGNITE_QUIET", "true");
		System.setProperty("java.net.preferIPv4Stack", "true");

		setPeerClassLoadingEnabled(true);
		setDeploymentMode(CONTINUOUS);
		setWorkDirectory("/tmp/gridgain/work");

		TcpDiscoverySpi tcpDiscoverySpi = new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi();
		TcpDiscoveryVmIpFinder tcpDiscoveryVmIpFinder = new org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder();
		ArrayList<String> list = new ArrayList<String>();
		list.add("127.0.0.1:47500..47510");
		/*
		list.add("10.10.10.1:47500..47510");
		list.add("10.10.10.11:47500..47510");
		list.add("10.10.10.12:47500..47510");
		list.add("10.10.10.13:47500..47510");
		*/

		tcpDiscoveryVmIpFinder.setAddresses(list);
		tcpDiscoverySpi.setIpFinder(tcpDiscoveryVmIpFinder);

		setDiscoverySpi(tcpDiscoverySpi);

		//BinaryConfiguration binaryConfiguration = new BinaryConfiguration();
		//BinaryBasicNameMapper nameMapper = new BinaryBasicNameMapper();
		//nameMapper.setSimpleName(true);
		//binaryConfiguration.setNameMapper(nameMapper);
		//setBinaryConfiguration(binaryConfiguration);

		DataRegionConfiguration dataRegionConfiguration = new org.apache.ignite.configuration.DataRegionConfiguration();
		dataRegionConfiguration.setName("Default_Region");
		dataRegionConfiguration.setInitialSize(100l * 1024 * 1024);

		DataStorageConfiguration dataStorageConfiguration = new org.apache.ignite.configuration.DataStorageConfiguration();
		dataStorageConfiguration.setStoragePath("/tmp/gridgain/data");

		DataRegionConfiguration myRegion = new org.apache.ignite.configuration.DataRegionConfiguration();
		myRegion.setName("MyDataRegion");
		myRegion.setPersistenceEnabled(true);
		myRegion.setInitialSize(200l * 1024 * 1024);
		myRegion.setMaxSize(400l * 1024 * 1024);
		myRegion.setPageEvictionMode(RANDOM_2_LRU);

		dataStorageConfiguration.setDataRegionConfigurations(myRegion);

		dataStorageConfiguration.setDefaultDataRegionConfiguration(dataRegionConfiguration);
		setDataStorageConfiguration(dataStorageConfiguration);
	}

}
