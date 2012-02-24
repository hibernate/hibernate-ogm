package org.hibernate.ogm.test.simpleentity;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

public class NoopServer extends AbstractServer {

	private static final Log log = LoggerFactory.make();

	@Override
	public void start() {
		log.info("starting EmbeddedNoop ...");

	}

	@Override
	public void stop() {
		log.info("stopping EmbeddedNoop ...");

	}

	@Override
	public boolean removeAllEntries() {
		log.info("removing all entries");
		return false;
	}

}
