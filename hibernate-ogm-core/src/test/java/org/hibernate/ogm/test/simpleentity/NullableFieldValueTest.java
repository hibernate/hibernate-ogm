/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.simpleentity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Test that a column value can be reset to null.
 * <p>
 * For example, Neo4j does not allow to set the property of a node to null. In that case you could decide to remove the
 * property, throw an exception or skip the operation.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class NullableFieldValueTest extends OgmTestCase {

	public void testValueShouldBeNullWhenSetToNull() throws Exception {
		Session session = sessions.openSession();
		String uuid = null;
		{
			Helicopter entity = helicopter( "Honey Bee CP3" );
			Transaction tx = session.beginTransaction();
			session.persist( entity );
			uuid = entity.getUUID();
			tx.commit();
			session.clear();
		}
		{
			Transaction tx = session.beginTransaction();
			Helicopter entity = (Helicopter) session.get( Helicopter.class, uuid );
			entity.setName( null );
			tx.commit();
			session.clear();
		}
		{
			Transaction tx = session.beginTransaction();
			Helicopter entity = (Helicopter) session.get( Helicopter.class, uuid );
			session.delete( entity );
			tx.commit();
			assertThat( entity.getName(), is( nullValue() ) );
		}
		session.close();
	}

	private Helicopter helicopter(String name) {
		Helicopter entity = new Helicopter();
		entity.setName( name );
		return entity;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Helicopter.class };
	}
}
