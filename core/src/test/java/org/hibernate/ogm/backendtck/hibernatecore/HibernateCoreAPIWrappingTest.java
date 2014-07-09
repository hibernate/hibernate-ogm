/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.hibernatecore;

import static org.fest.assertions.Assertions.assertThat;

import javax.naming.Reference;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryObjectFactory;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionImpl;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class HibernateCoreAPIWrappingTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Contact.class );

	@Test
	public void testWrappedFromEntityManagerAPI() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );
		assertThat( HibernateEntityManagerFactory.class.isAssignableFrom( emf.getClass() ) ).isTrue();
		SessionFactory factory = ( (HibernateEntityManagerFactory) emf ).getSessionFactory();
		assertThat( factory.getClass() ).isEqualTo( OgmSessionFactoryImpl.class );

		Session s = factory.openSession();
		assertThat( s.getClass() ).isEqualTo( OgmSessionImpl.class );
		assertThat( s.getSessionFactory().getClass() ).isEqualTo( OgmSessionFactoryImpl.class );
		s.close();

		EntityManager em = emf.createEntityManager();
		assertThat( em.unwrap( Session.class ) instanceof OgmSession );
		assertThat( em.getDelegate().getClass() ).isEqualTo( OgmSessionImpl.class );

		em.close();

		emf.close();
	}

	@Test
	public void testJNDIReference() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );
		SessionFactory factory = ( (HibernateEntityManagerFactory) emf ).getSessionFactory();
		Reference reference = factory.getReference();
		assertThat( reference.getClassName() ).isEqualTo( OgmSessionFactoryImpl.class.getName() );
		assertThat( reference.getFactoryClassName() ).isEqualTo( OgmSessionFactoryObjectFactory.class.getName() );
		assertThat( reference.get( 0 ) ).isNotNull();
		assertThat( reference.getFactoryClassLocation() ).isNull();

		OgmSessionFactoryObjectFactory objFactory = new OgmSessionFactoryObjectFactory();
		SessionFactory factoryFromRegistry = (SessionFactory) objFactory.getObjectInstance( reference, null, null, null );
		assertThat( factoryFromRegistry.getClass() ).isEqualTo( OgmSessionFactoryImpl.class );
		assertThat( factoryFromRegistry.getReference() ).isEqualTo( factory.getReference() );

		emf.close();
	}

}
