/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class JPATablePerClassFindTest {
	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/ogm.xml", CommunityMember.class, Employee.class );

	private EntityManagerFactory emf;
	private EntityManager em;

	@Before
	public void setUp() {
		emf = Persistence.createEntityManagerFactory( "ogm",
				TestHelper.getEnvironmentProperties() );
		em = emf.createEntityManager();
	}

	@After
	public void tearDown() {
		dropSchemaAndDatabase( emf );
		em.close();
		emf.close();
	}

	@Test
	public void testJPAPolymorphicFind() throws Exception {
		em.getTransaction().begin();
		CommunityMember member = new CommunityMember( "Davide" );
		em.persist( member );

		Employee employee = new Employee( "Alex", "EMPLOYER" );
		em.persist( employee );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		CommunityMember lh = em.find( CommunityMember.class, member.name );
		assertThat( lh ).isNotNull();
		assertThat( lh ).isInstanceOf( CommunityMember.class );

		CommunityMember lsh = em.find( Employee.class, employee.name );
		assertThat( lsh ).isNotNull();
		assertThat( lsh ).isInstanceOf( Employee.class );
		assertThat( employee.employer ).isEqualTo( employee.employer );

		em.remove( lh );
		em.remove( lsh );
		em.getTransaction().commit();
	}

}
