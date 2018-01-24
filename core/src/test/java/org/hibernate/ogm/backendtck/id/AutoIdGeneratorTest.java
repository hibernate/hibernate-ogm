/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test case for JPA Auto identifier generator.
 *
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 */
public class AutoIdGeneratorTest extends OgmJpaTestCase {
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
	public void testAutoIdentifierGenerator() throws Exception {
		DistributedRevisionControl git = new DistributedRevisionControl();
		DistributedRevisionControl bzr = new DistributedRevisionControl();

		em.getTransaction().begin();
		git.setName( "Git" );
		em.persist( git );

		bzr.setName( "Bazaar" );
		em.persist( bzr );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		DistributedRevisionControl dvcs = em.find( DistributedRevisionControl.class, git.getId() );
		assertThat( dvcs ).isNotNull();
		assertThat( dvcs.getId() ).isEqualTo( 1 );
		em.remove( dvcs );

		dvcs = em.find( DistributedRevisionControl.class, bzr.getId() );
		assertThat( dvcs ).isNotNull();
		assertThat( dvcs.getId() ).isEqualTo( 2 );
		em.getTransaction().commit();
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		TestHelper.enableCountersForInfinispan( info.getProperties() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DistributedRevisionControl.class
		};
	}
}
