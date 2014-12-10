/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.test.cachemapping;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.utils.EhcacheTestHelper;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.utils.OgmTestCase;
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

	protected Cache<?> getEntityCache(String tableName, String... columnNames) {
		return EhcacheTestHelper.getEntityCache( sessions, new DefaultEntityKeyMetadata( tableName, columnNames ) );
	}

	protected Cache<?> getAssociationCache(String tableName, String... columnNames) {
		DefaultAssociationKeyMetadata associationKeyMetadata = new DefaultAssociationKeyMetadata.Builder().table( tableName )
				.columnNames( columnNames )
				.build();

		return EhcacheTestHelper.getAssociationCache( sessions, associationKeyMetadata );
	}

	protected Cache<?> getIdSourceCache(String tableName) {
		return EhcacheTestHelper.getIdSourceCache( sessions, tableName );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Family.class, Plant.class };
	}
}
