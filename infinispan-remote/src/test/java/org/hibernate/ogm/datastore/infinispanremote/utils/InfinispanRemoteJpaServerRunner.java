/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.utils;

import org.hibernate.SessionFactory;
import org.hibernate.ogm.utils.jpa.OgmJpaTestRunner;
import org.junit.BeforeClass;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

/**
 * Start the Infinispan Remote server before running anything else when testing with {@link OgmJpaTestRunner}.
 * <p>
 * It extends {@link OgmJpaTestRunner} and makes sure that the {@link SessionFactory} is created after the server is
 * started. It means that you can access the factory in the {@link BeforeClass} methods if you need to.
 *
 * @see OgmJpaTestRunner
 * @author Davide D'Alto
 */
public class InfinispanRemoteJpaServerRunner extends OgmJpaTestRunner {

	public static RemoteHotRodServerRule hotrodServer = new RemoteHotRodServerRule();

	public InfinispanRemoteJpaServerRunner(Class<?> klass) throws InitializationError {
		super( klass );
	}

	@Override
	public void run(RunNotifier notifier) {
		try {
			hotrodServer.before();
			super.run( notifier );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
		finally {
			hotrodServer.after();
		}
	}
}
