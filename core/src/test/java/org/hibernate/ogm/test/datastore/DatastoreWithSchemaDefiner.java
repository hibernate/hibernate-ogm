/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.datastore;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.ogm.utils.PackagingRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test that DatastoreProvider implementing StartStoppable are properly receiving events.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class DatastoreWithSchemaDefiner {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/datastoreobserver.xml", Noise.class );

	@Test
	public void testSchemaDefiner() throws Exception {
		assertThat( DatastoreProviderGeneratingSchema.TestSchemaDefiner.schemaInitialized ).isFalse();

		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "datastoreobserver" );
		try {
			assertThat( DatastoreProviderGeneratingSchema.TestSchemaDefiner.schemaInitialized ).isTrue();
		}
		finally {
			emf.close();
		}
	}
}
