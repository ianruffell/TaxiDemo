package com.gridgain.demo;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.gridgain.grid.configuration.GridGainConfiguration;

public class Server implements AutoCloseable {
	private Ignite ignite;

	public static void main(String[] args) {
			new Server();
	}

	public Server() {
		AppConfiguration cfg = new AppConfiguration();
		GridGainConfiguration ggCfg = new GridGainConfiguration();
		ggCfg.setLicenseUrl("file:/Users/iruffell/gridgain/gridgain-license.xml");
		cfg.setPluginConfigurations(ggCfg);

		ignite = Ignition.start(cfg);
	}

	@Override
	public void close() throws Exception {
		ignite.close();
	}

}
