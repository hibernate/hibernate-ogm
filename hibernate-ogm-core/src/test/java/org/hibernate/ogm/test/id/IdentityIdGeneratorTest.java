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
package org.hibernate.ogm.test.id;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.hibernate.ogm.test.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * @author Nabeel Ali Memon <nabeel@nabeelalimemon.com>
 */
public class IdentityIdGeneratorTest extends JpaTestCase {

	@Test
	public void testIdentityGenerator() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		Animal jungleKing = new Animal();
		Animal fish = new Animal();
		boolean ok = false;
		try {
			jungleKing.setName( "Lion" );
			jungleKing.setSpecies( "Mammal" );
			em.persist( jungleKing );

			fish.setName( "Shark" );
			fish.setSpecies( "Tiger Shark" );
			em.persist( fish );
			ok = true;
		}
		finally {
			commitOrRollback( ok );
		}
		em.clear();

		getTransactionManager().begin();
		ok = false;
		try {
			Animal animal = em.find( Animal.class, jungleKing.getId() );
			assertThat( animal ).isNotNull();
			assertThat( animal.getId() ).isEqualTo( 1 );
			assertThat( animal.getName() ).isEqualTo( "Lion" );
			em.remove( animal );

			animal = em.find( Animal.class, fish.getId() );
			assertThat( animal ).isNotNull();
			assertThat( animal.getId() ).isEqualTo( 2 );
			assertThat( animal.getName() ).isEqualTo( "Shark" );
			em.remove( animal );
			ok = true;
		}
		finally {
			commitOrRollback( ok );
		}
		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Animal.class
		};
	}
}
