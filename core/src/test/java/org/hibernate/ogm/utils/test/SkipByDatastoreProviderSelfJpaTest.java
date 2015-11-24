/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.CASSANDRA_EXPERIMENTAL;
import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.COUCHDB_EXPERIMENTAL;
import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.INFINISPAN;
import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.MAP;
import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.EHCACHE;
import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.REDIS_EXPERIMENTAL;
import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.NEO4J_EMBEDDED;
import static org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider.MONGODB;
import static org.junit.Assert.fail;

import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.jpa.JpaTestCase;

import org.junit.Test;

/**
 * Test {@link SkipByDatastoreProvider} is working with {@link JpaTestCase}
 *
 * @author Mark Paluch
 */
public class SkipByDatastoreProviderSelfJpaTest extends JpaTestCase {

	@Test
	@SkipByDatastoreProvider({
		MAP, INFINISPAN, MONGODB, EHCACHE, NEO4J_EMBEDDED, COUCHDB_EXPERIMENTAL, REDIS_EXPERIMENTAL, CASSANDRA_EXPERIMENTAL
	})
	public void testWhichAlwaysFails() {
		fail( "This should never be executed" );
	}

	@Test
	public void testCorrect() {
		// all fine
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Hypothesis.class };
	}

}
