/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.utils.jpa.JpaTestCase.extractJBossTransactionManager;

import java.util.Date;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.utils.PackagingRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class TypeOverridingInDialectTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone-customdialect.xml",
			Poem.class,
			OverridingTypeDialect.class,
			ExplodingType.class,
			TypeOverridingInDialectTest.class
		);

	@Test
	public void testOverriddenTypeInDialect() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone" );

		TransactionManager transactionManager = extractJBossTransactionManager( emf );
		transactionManager.begin();
		final EntityManager em = emf.createEntityManager();
		try {
			Poem poem = new Poem();
			poem.setName( "L'albatros" );
			poem.setPoemSocietyId( UUID.randomUUID() );
			poem.setCreation( new Date() );
			em.persist( poem );
			transactionManager.commit();
			assertThat( true ).as( "Custom type not used" ).isFalse();
		}
		catch ( RollbackException e ) {
			//make this chaing more robust
			assertThat( e.getCause().getCause().getMessage() ).isEqualTo( "Exploding type" );
		}
		finally {
			try {
				transactionManager.rollback();
			}
			catch ( Exception e ) {
				//we try
			}
			em.close();
			dropSchemaAndDatabase( emf );
			emf.close();
		}
	}

}
