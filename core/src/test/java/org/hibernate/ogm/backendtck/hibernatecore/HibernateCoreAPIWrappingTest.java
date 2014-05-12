/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryObjectFactory;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionImpl;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
		assertThat( em.unwrap( Session.class ).getClass() ).isEqualTo( OgmSessionImpl.class );
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
