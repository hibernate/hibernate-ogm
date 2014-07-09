/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.utils.jpa.JpaTestCase.extractJBossTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JPAStandaloneTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Poem.class );

	@Test
	public void testJTAStandalone() throws Exception {

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );

		TransactionManager transactionManager = extractJBossTransactionManager( emf );

		transactionManager.begin();
		final EntityManager em = emf.createEntityManager();
		Poem poem = new Poem();
		poem.setName( "L'albatros" );
		em.persist( poem );
		transactionManager.commit();

		em.clear();

		transactionManager.begin();
		poem = em.find( Poem.class, poem.getId() );
		assertThat( poem ).isNotNull();
		assertThat( poem.getName() ).isEqualTo( "L'albatros" );
		em.remove( poem );
		transactionManager.commit();

		em.close();

		dropSchemaAndDatabase( emf );
		emf.close();
	}


}
