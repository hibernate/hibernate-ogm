/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.test.serialization;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.datastore.ehcache.EhcacheProperties;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for reading property values and association rows back from the Ehcache disk store. This is implicitly ensured by
 * using a cache which allows only one element on the heap and then flows over to disk.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "OGM-443")
public class ReadingFromDiskStoreTest extends OgmJpaTestCase {
	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void shouldRetainPropertyValuesWhenReadingFromDiskStore() throws Exception {
		List<Engineer> bixbyEngineers = Arrays.asList(
				new Engineer( "Bob the constructor" ),
				new Engineer( "Biff the destructor" )
		);

		Bridge bixbyCreek = new Bridge( 1L, "Bixby Creek Bridge", bixbyEngineers );

		em.getTransaction().begin();
		em.persist( bixbyCreek );
		em.getTransaction().commit();

		em.clear();
		em.getTransaction().begin();
		Bridge news = em.find( Bridge.class, 1L );
		assertThat( news ).isNotNull();
		assertThat( news.getName() ).isEqualTo( "Bixby Creek Bridge" );

		em.remove( news );
		assertThat( em.find( Bridge.class, 1L ) ).isNull();
		em.getTransaction().commit();
	}

	@Test
	public void shouldRetrieveAssociationRowsWhenReadingAssociationFromDisk() throws Exception {
		Engineer bob = new Engineer( "Bob the constructor" );
		Engineer biff = new Engineer( "Biff the destructor" );

		List<Engineer> bixbyEngineers = Arrays.asList( bob, biff );

		Bridge bixbyCreek = new Bridge( 2L, "Bixby Creek Bridge", bixbyEngineers );

		Engineer bruce = new Engineer( "Bruce the initializer" );
		List<Engineer> astoriaEngineers = Arrays.asList( bruce );

		Bridge astoriaMegler = new Bridge( 3L, "Astoria-Megler Bridge", astoriaEngineers );

		em.getTransaction().begin();

		em.persist( bixbyCreek );
		em.persist( astoriaMegler );
		em.getTransaction().commit();

		em.clear();
		em.getTransaction().begin();
		Bridge loadedBridge = em.find( Bridge.class, 3L );
		assertThat( loadedBridge ).isNotNull();
		assertThat( loadedBridge.getEngineers() ).onProperty( "name" ).containsOnly( "Bruce the initializer" );
		em.remove( loadedBridge );
		assertThat( em.find( Bridge.class, 3L ) ).isNull();

		loadedBridge = em.find( Bridge.class, 2L );
		assertThat( loadedBridge ).isNotNull();
		assertThat( loadedBridge.getEngineers() ).onProperty( "name" )
				.containsOnly( "Bob the constructor", "Biff the destructor" );
		em.remove( loadedBridge );
		assertThat( em.find( Bridge.class, 2L ) ).isNull();
		em.getTransaction().commit();
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		info.getProperties().put( EhcacheProperties.CONFIGURATION_RESOURCE_NAME, "enforced-disk-read-ehcache.xml" );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Bridge.class, Engineer.class };
	}
}
