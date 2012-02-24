package org.hibernate.ogm.test.simpleentity;

import junit.framework.TestCase;

import org.hibernate.cfg.Environment;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * 
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public abstract class OgmTestBase extends TestCase {

	private static final Log log = LoggerFactory.make();
	private ServerAware server = null;

	private void startServer() {
		if (this.server == null) {
			throw new RuntimeException(
					"OGMTestBase.server is not set correctly. Please set it before running test cases");
		}
		this.server.start();
	}

	protected void stopServer() {
		this.server.stop();
		this.server = null;
	}

	protected void setUpServer() {
		String provider = Environment.getProperties().getProperty(
				"hibernate.ogm.datastore.provider");

		log.info("provider: " + provider);
		if (provider
				.equals("org.hibernate.ogm.datastore.voldemort.impl.VoldemortDatastoreProvider")) {
			this.server = new EmbeddedVoldemort();
		} else {
			this.server = new NoopServer();
		}

		this.startServer();
	}

	protected ServerAware getServer() {
		return this.server;
	}
}
