/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.test.cachestorage;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.keyvalue.options.CacheStorageType;
import org.junit.Test;

/**
 * Test for the {@link CacheStorageType#CACHE_PER_TABLE} strategy.
 *
 * @author Gunnar Morling
 */
public class CachePerTableCacheStorageTest extends CacheStorageTestBase {

	@Test
	public void shouldUseCachePerTable() {
		Cache<?> plantCache = getEntityCache( "Plant", "id" );
		assertThat( plantCache.getName() ).isEqualTo( "Plant" );

		Cache<?> familyCache = getEntityCache( "Family", "id" );
		assertThat( familyCache.getName() ).isEqualTo( "Family" );

		Cache<?> membersCache = getAssociationCache( "Family_Plant", "Family_id" );
		assertThat( membersCache.getName() ).isEqualTo( "associations_Family_Plant" );

		Cache<?> plantSequenceCache = getIdSourceCache( "myIds" );
		assertThat( plantSequenceCache.getName() ).isEqualTo( "myIds" );
	}
}
