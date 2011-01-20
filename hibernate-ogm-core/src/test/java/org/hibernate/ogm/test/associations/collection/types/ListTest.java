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
package org.hibernate.ogm.test.associations.collection.types;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import static org.fest.assertions.Assertions.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.getAssociationCache;
import static org.hibernate.ogm.test.utils.TestHelper.getEntityCache;

/**
 * @author Emmanuel Bernard  <emmanuel@hibernate.org>
 */
public class ListTest extends OgmTestCase {

	public void testOrderedList() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Child luke = new Child();
		luke.setName( "Luke" );
		Child leia = new Child();
		leia.setName( "Leia" );
		session.persist( luke );
		session.persist( leia );
		Father father = new Father();
		father.getOrderedChildren().add( luke );
		father.getOrderedChildren().add( null );
		father.getOrderedChildren().add( leia );
		session.persist( father );
		tx.commit();

		session.clear();

		tx = session.beginTransaction();
		father = (Father) session.get( Father.class, father.getId() );
		assertThat( father.getOrderedChildren() )
				.as("List should have 3 elements")
				.hasSize( 3 );
		assertThat( father.getOrderedChildren().get(0).getName() )
				.as( "Luke should be first" )
				.isEqualTo( luke.getName() );
		assertThat( father.getOrderedChildren().get( 1 ) )
				.as( "Second born should be null" )
				.isNull();
		assertThat( father.getOrderedChildren().get(2).getName() )
				.as( "Leia should be third" )
				.isEqualTo( leia.getName() );
		session.delete( father );
		session.delete( session.load(Child.class, luke.getId() ) );
		session.delete( session.load(Child.class, leia.getId() ) );
		tx.commit();

		assertThat(getEntityCache( session )).as("Entity cache should be empty").hasSize( 0 );
		assertThat(getAssociationCache( session )).as("Association cache should be empty").hasSize( 0 );

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Father.class,
				Child.class
		};
	}
}
