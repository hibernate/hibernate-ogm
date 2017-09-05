/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.mongodb.ogm;

import java.util.Random;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.mongodb.MongoException;

/**
 * Context object controlling the {@link EntityManagerFactory} lifecycle and making it available to MongoDB-based
 * benchmarks.
 *
 * @author Gunnar Morling
 */
@State(Scope.Benchmark)
public class EntityManagerFactoryHolder {

	EntityManagerFactory entityManagerFactory;
	TransactionManager transactionManager;
	Random rand;

	@Setup
	public void setupEntityManagerFactory() throws Exception {
		entityManagerFactory = Persistence.createEntityManagerFactory( "perfTestPu" );
		dropSchemaAndDatabase( entityManagerFactory );

		transactionManager = extractJBossTransactionManager( entityManagerFactory );
		rand = new Random();
	}

	@TearDown
	public void closeEntityManagerFactory() {
		entityManagerFactory.close();
	}

	private TransactionManager extractJBossTransactionManager(EntityManagerFactory factory) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) factory;
		return sessionFactory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	private MongoDBDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class );
		if ( !( MongoDBDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with MongoDB, cannot extract underlying cache" );
		}
		return MongoDBDatastoreProvider.class.cast( provider );
	}

	private void dropSchemaAndDatabase(EntityManagerFactory entityManagerFactory) {
		MongoDBDatastoreProvider provider = getProvider( entityManagerFactory.unwrap( SessionFactory.class ) );
		try {
			provider.getDatabase().drop();
		}
		catch ( MongoException ex ) {
			throw new RuntimeException( ex );
		}
	}
}
