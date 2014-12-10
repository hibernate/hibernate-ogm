/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.test.serialization;

import org.hibernate.ogm.datastore.ehcache.EhcacheProperties;
import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;

/**
 * Test for reading property values and association rows back from the Ehcache disk store. This is implicitly ensured by
 * using a cache which allows only one element on the heap and then flows over to disk.
 *
 * @author Gunnar Morling
 */
public class ReadingFromDiskStoreUsingCachePerKindStrategyTest extends ReadingFromDiskStoreTest {

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		super.refineInfo( info );
		info.getProperties().put( EhcacheProperties.CACHE_MAPPING, CacheMappingType.CACHE_PER_KIND );
	}
}
