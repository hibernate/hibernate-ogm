/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.initialize;

import org.hibernate.ogm.datastore.infinispan.impl.InfinispanEmbeddedDatastoreProvider;
import org.hibernate.ogm.datastore.infinispan.test.cachemapping.Family;
import org.hibernate.ogm.datastore.infinispan.test.cachemapping.Plant;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Verify that a cache which is configured as non-transactional is indeed not transactional at boot time. The reason for
 * this is that some of the configuration steps could make a cache transactional by side-effect of other operations.
 * See infinispan-local.xml for the cache configurations being loaded.
 */
@TestForIssue(jiraKey = "OGM-1272")
public class NonTransactionalCachesTest extends OgmTestCase {

	@Test
	public void testNonTransactionalCacheConfiguration() {
		assertEquals( TransactionMode.NON_TRANSACTIONAL, getTransactionModeForCache( "IDENTIFIERS" ) );
	}

	@Test
	public void testTransactionalCacheConfiguration() {
		assertEquals( TransactionMode.TRANSACTIONAL, getTransactionModeForCache( "ENTITIES" ) );
	}

	private TransactionMode getTransactionModeForCache(String cacheName) {
		EmbeddedCacheManager cacheManager = getProvider().getCacheManager().getCacheManager();
		return cacheManager.getCacheConfiguration( cacheName ).transaction().transactionMode();
	}

	private InfinispanEmbeddedDatastoreProvider getProvider() {
		return (InfinispanEmbeddedDatastoreProvider) getSessionFactory()
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Family.class, Plant.class };
	}
}
