package org.gridgain.gg9test;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgnitionManager;
import org.apache.ignite.InitParameters;

public class IgniteServer {

	public static final String CONFIG_PATH = "config/ignite-config.conf";
	public static final String WORKDIR_PATH = "work";

	public static void main(String[] args) throws Exception {
		new IgniteServer();
	}

	public IgniteServer() throws InterruptedException, ExecutionException {
		CompletableFuture<Ignite> igniteFuture = IgnitionManager.start("node1", Paths.get(CONFIG_PATH),
				Paths.get(WORKDIR_PATH));

		InitParameters initParameters = InitParameters.builder().destinationNodeName("node1")
				.metaStorageNodeNames(List.of("node1")).clusterName("cluster").build();

		IgnitionManager.init(initParameters);
		Ignite ignite = igniteFuture.get();

		System.out.println(ignite.clusterNodes());
	}

}
