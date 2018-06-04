/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.utils;

import org.hibernate.SessionFactory;
import org.hibernate.ogm.utils.OgmTestRunner;

import org.junit.BeforeClass;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

/**
 * Start the MongoDb server before running anything else.
 * <p>
 * It extends {@link OgmTestRunner} and makes sure that the {@link SessionFactory} is created
 * after the server is started. It means that you can access the factory in the {@link BeforeClass}
 * methods if you need to.
 *
 * @author The Viet Nguyen
 */
public class MongoDbServerRunner extends OgmTestRunner {

	private static EmbeddedMongoDbServerRule embeddedMongoDbServerRule = new EmbeddedMongoDbServerRule();

	public MongoDbServerRunner(Class<?> klass) throws InitializationError {
		super( klass );
	}

	@Override
	@SuppressWarnings("Duplicates")
	public void run(RunNotifier notifier) {
		try {
			embeddedMongoDbServerRule.before();
			super.run( notifier );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
		finally {
			embeddedMongoDbServerRule.after();
		}
	}
}
