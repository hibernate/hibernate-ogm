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
package org.hibernate.ogm.test.hibernatecore;

import javax.naming.Reference;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryObjectFactory;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JNDIReferenceTest extends OgmTestCase {

	@Test
	public void testGetReferenceImplementation() throws Exception {
		final Session session = openSession();
		SessionFactory factory = session.getSessionFactory();

		assertThat( factory.getClass() ).isEqualTo( OgmSessionFactory.class );

		Reference reference = factory.getReference();
		assertThat( reference.getClassName() ).isEqualTo( OgmSessionFactory.class.getName() );
		assertThat( reference.getFactoryClassName() ).isEqualTo( OgmSessionFactoryObjectFactory.class.getName() );
		assertThat( reference.get( 0 ) ).isNotNull();
		assertThat( reference.getFactoryClassLocation() ).isNull();

		OgmSessionFactoryObjectFactory objFactory = new OgmSessionFactoryObjectFactory();
		SessionFactory factoryFromRegistry = (SessionFactory) objFactory.getObjectInstance( reference, null, null, null );
		assertThat( factoryFromRegistry.getClass() ).isEqualTo( OgmSessionFactory.class );
		assertThat( factoryFromRegistry.getReference() ).isEqualTo( factory.getReference() );

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Contact.class
		};
	}
}
