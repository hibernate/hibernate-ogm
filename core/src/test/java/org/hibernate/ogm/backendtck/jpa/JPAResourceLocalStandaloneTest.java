/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.RequiresTransactionalCapabilitiesRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JPAResourceLocalStandaloneTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone-resourcelocal.xml", Poem.class );

	@Rule
	public RequiresTransactionalCapabilitiesRule transactions = new RequiresTransactionalCapabilitiesRule();

	@Test
	public void testJTAStandalone() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );
		try {

			final EntityManager em = emf.createEntityManager();
			try {

				em.getTransaction().begin();
				Poem poem = new Poem();
				poem.setName( "L'albatros" );
				em.persist( poem );
				em.getTransaction().commit();

				em.clear();

				em.getTransaction().begin();
				Poem poem2 = new Poem();
				poem2.setName( "Wazaaaaa" );
				em.persist( poem2 );
				em.flush();
				assertThat( getNumberOfEntities( em ) ).isEqualTo( 2 );
				em.getTransaction().rollback();

				assertThat( getNumberOfEntities( em ) ).isEqualTo( 1 );

				em.getTransaction().begin();
				poem = em.find( Poem.class, poem.getId() );
				assertThat( poem ).isNotNull();
				assertThat( poem.getName() ).isEqualTo( "L'albatros" );
				em.remove( poem );
				poem2 = em.find( Poem.class, poem2.getId() );
				assertThat( poem2 ).isNull();
				em.getTransaction().commit();

			}
			finally {
				EntityTransaction transaction = em.getTransaction();
				if ( transaction != null && transaction.isActive() ) {
					transaction.rollback();
				}
				em.close();
			}
		}
		finally {
			dropSchemaAndDatabase( emf );
			emf.close();
		}
	}


}
