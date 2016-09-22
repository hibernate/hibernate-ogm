/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.CASSANDRA_EXPERIMENTAL;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.COUCHDB_EXPERIMENTAL;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.INFINISPAN;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.MAP;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.EHCACHE;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.REDIS_EXPERIMENTAL;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.datastore.impl.DatastoreProviderType.MONGODB;
import static org.junit.Assert.fail;

import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.Test;

/**
 * Test {@link SkipByDatastoreProvider} is working with {@link OgmJpaTestCase}
 *
 * @author Mark Paluch
 */
public class SkipByDatastoreProviderSelfJpaTest extends OgmJpaTestCase {

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
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class };
	}

}
