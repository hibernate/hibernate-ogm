/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.cachemapping;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.utils.OgmTestCase;
import org.infinispan.Cache;
import org.junit.Test;

/**
 * Base for tests around the cache mapping strategy.
 *
 * @author Gunnar Morling
 */
public abstract class CacheMappingTestBase extends OgmTestCase {

	@Test
	public void canStoreAndLoadEntitiesWithIdGeneratorAndAssociation() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Plant ficus = new Plant( 181 );
		session.persist( ficus );

		Family family = new Family( "family-1", "Moraceae", ficus );
		session.persist( family );

		session.getTransaction().commit();

		// when
		session.getTransaction().begin();
		Family loadedFamily = (Family) session.get( Family.class, "family-1" );

		// then
		assertThat( loadedFamily ).isNotNull();
		assertThat( loadedFamily.getMembers() ).onProperty( "height" ).containsExactly( 181 );

		session.getTransaction().commit();

		session.close();
	}

	protected Cache<?, Map<String, Object>> getEntityCache(String tableName, String... columnNames) {
		return getProvider().getCacheManager()
				.getEntityCache( new DefaultEntityKeyMetadata( tableName, columnNames ) );
	}

	protected Cache<?,Map<RowKey,Map<String,Object>>> getAssociationCache(String tableName, String... columnNames) {
		DefaultAssociationKeyMetadata associationKeyMetadata = new DefaultAssociationKeyMetadata.Builder().table( tableName )
				.columnNames( columnNames )
				.build();

		return getProvider().getCacheManager().getAssociationCache( associationKeyMetadata );
	}

	protected Cache<?,Object> getIdSourceCache(String tableName) {
		return getProvider().getCacheManager()
				.getIdSourceCache( DefaultIdSourceKeyMetadata.forTable( tableName, "sequence_name", "next_val" ) );
	}

	private InfinispanDatastoreProvider getProvider() {
		return (InfinispanDatastoreProvider) sessions
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Family.class, Plant.class };
	}
}
