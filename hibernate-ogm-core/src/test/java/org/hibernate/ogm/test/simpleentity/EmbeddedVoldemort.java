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
		this.startVoldemortServer();
	}

	private void startVoldemortServer() {
		this.config = VoldemortConfig
				.loadFromVoldemortHome(EmbeddedVoldemort.DEBUG_LOCATION);
		this.server = new VoldemortServer(this.config);
		this.server.start();
	}

	/**
	 * Stops embedded server such as Voldemort.
	 */
	public void stop() {
		log.info("Voldemort stopping ...");
		if (this.server != null) {
			this.server.stop();
		}

		log.info("removing all the entries from voldemort: "
				+ this.removeAllEntries());
	}

	@Override
	public boolean removeAllEntries() {
		return this.deleteDirectories(new File(new String(
				EmbeddedVoldemort.DEBUG_LOCATION) + File.separator + "data"));
	}

}
