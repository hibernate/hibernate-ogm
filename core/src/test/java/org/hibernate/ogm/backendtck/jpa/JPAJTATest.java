/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.junit.Rule;
import org.junit.Test;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JPAJTATest extends JpaTestCase {
	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/transaction-type-jta.xml", Poem.class );

	@Test
	@SkipByGridDialect(value = GridDialectType.MONGODB, comment = "MongoDB tests runs w/o transaction manager")
	public void testBootstrapAndCRUD() throws Exception {

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory(
				"transaction-type-jta", TestHelper.getEnvironmentProperties()
		);

		TransactionManager transactionManager = getTransactionManager( emf );

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

	private TransactionManager getTransactionManager(EntityManagerFactory factory) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) ( (HibernateEntityManagerFactory) factory )
				.getSessionFactory();
		return sessionFactory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Poem.class };
	}
}
