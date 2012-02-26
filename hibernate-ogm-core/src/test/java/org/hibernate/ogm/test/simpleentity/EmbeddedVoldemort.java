package org.hibernate.ogm.test.simpleentity;

import java.io.File;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

import voldemort.server.VoldemortConfig;
import voldemort.server.VoldemortServer;

public class EmbeddedVoldemort extends AbstractServer {

	private static final Log log = LoggerFactory.make();
	private VoldemortConfig config;
	private VoldemortServer server;

	/**
	 * Starts embedded server such as Voldemort.
	 */
	public void start() {
		log.info("Voldemort starting ...");
		startVoldemortServer();
	}

	private void startVoldemortServer() {
		config = VoldemortConfig
				.loadFromVoldemortHome(EmbeddedVoldemort.DEBUG_LOCATION);
		server = new VoldemortServer(config);
		server.start();
	}

	/**
	 * Stops embedded server such as Voldemort.
	 */
	public void stop() {
		log.info("Voldemort stopping ...");
		if (server != null) {
			server.stop();
		}

		log.info("removing all the entries from voldemort: "
				+ removeAllEntries());
	}

	@Override
	public boolean removeAllEntries() {
		return deleteDirectories(new File(new String(
				EmbeddedVoldemort.DEBUG_LOCATION) + File.separator + "data"));
	}

}
