/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.utils.TestEntities;
import org.hibernate.ogm.utils.TestEntityManagerFactory;
import org.hibernate.ogm.utils.TestEntityManagerFactoryConfiguration;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.runner.RunWith;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 * @author Guillaume Smet
 */
@RunWith(OgmJpaTestRunner.class)
public abstract class OgmJpaTestCase {

	@TestEntityManagerFactory
	private EntityManagerFactory factory;

	public EntityManagerFactory getFactory() {
		return factory;
	}

	@TestEntities
	private Class<?>[] getTestEntities() {
		return getAnnotatedClasses();
	}

	/**
	 * Must be implemented by subclasses to return the entity types used by this test.
	 *
	 * @return an array with this tests entity types
	 */
	protected abstract Class<?>[] getAnnotatedClasses();

	@TestEntityManagerFactoryConfiguration
	private void modifyConfiguration(GetterPersistenceUnitInfo info) {
		configure( info );
	}

	/**
	 * Can be overridden in subclasses to inspect or modify the {@link GetterPersistenceUnitInfo} of this test.
	 *
	 * @param info the configuration
	 */
	protected void configure(GetterPersistenceUnitInfo info) {
	}

	/**
	 * @return Return the transaction manager in the case where one is available. Can be {@code null}.
	 * A transaction manager will be available if JBoss Transaction is on the classpath. Where it is in use depends on
	 * the current persistence unit under test and its persistence type setting.
	 *
	 * @throws Exception
	 */
	protected TransactionManager getTransactionManager(EntityManagerFactory factory) {
		return getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	protected ServiceRegistryImplementor getServiceRegistry() {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) getFactory();
		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		return serviceRegistry;
	}

	protected void removeEntities() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		for ( Class<?> each : getAnnotatedClasses() ) {
			List<?> entities = em.createQuery( "FROM " + each.getSimpleName() ).getResultList();
			for ( Object object : entities ) {
				em.remove( object );
			}
		}
		em.getTransaction().commit();
		em.close();
	}

}
