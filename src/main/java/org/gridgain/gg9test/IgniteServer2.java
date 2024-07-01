package org.gridgain.gg9test;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgnitionManager;
import org.apache.ignite.InitParameters;

public class IgniteServer2 {

	public static final String CONFIG_PATH = "config/ignite-config.conf";
	public static final String WORKDIR_PATH = "work2";

	public static void main(String[] args) throws Exception {
		new IgniteServer2();
	}

	public IgniteServer2() throws InterruptedException, ExecutionException {
		CompletableFuture<Ignite> igniteFuture = IgnitionManager.start("node2", Paths.get(CONFIG_PATH),
				Paths.get(WORKDIR_PATH));

		InitParameters initParameters = InitParameters.builder().destinationNodeName("node2")
				.metaStorageNodeNames(List.of("node2")).clusterName("cluster").build();

		IgnitionManager.init(initParameters);
		Ignite ignite = igniteFuture.get();

		System.out.println(ignite.clusterNodes());
	}

}
