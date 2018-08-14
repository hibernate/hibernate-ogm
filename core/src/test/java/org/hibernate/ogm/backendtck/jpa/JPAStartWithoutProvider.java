/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests a store and load scenario, using the JPA interface,
 * without defining a persistence provider to enable Hibernate OGM engine.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1517")
public class JPAStartWithoutProvider {

	private static final String POEM_NAME = "Orlando Furioso";

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/ogm-noprovider.xml", Poem.class );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testJPAStartWithoutProvider() {
		Map<String, Object> settings = new HashMap<>( TestHelper.getDefaultTestSettings() );

		EntityManagerFactory factory = null;
		try {
			factory = Persistence.createEntityManagerFactory( "ogm", settings );

			EntityManager manager = null;
			try {
				manager = factory.createEntityManager();
				createAndLoadAPoem( manager );
			}
			finally {
				if ( manager != null ) {
					manager.close();
				}
			}
		}
		finally {
			if ( factory != null ) {
				factory.close();
			}
		}
	}

	private void createAndLoadAPoem(EntityManager em) {
		EntityTransaction trx = em.getTransaction();

		trx.begin();
		Poem entity = new Poem();
		entity.setName( POEM_NAME );
		em.persist( entity );

		Poem poem = em.find( Poem.class, entity.getId() );
		assertThat( poem.getName() ).isEqualTo( POEM_NAME );
		trx.commit();
	}
}
