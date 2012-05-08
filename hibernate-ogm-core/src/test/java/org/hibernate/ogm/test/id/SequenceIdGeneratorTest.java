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

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.ogm.test.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Nabeel Ali Memon <nabeel@nabeelalimemon.com>
 */
public class SequenceIdGeneratorTest extends JpaTestCase {
	@Test
	public void testSequenceIdGenerationInJTA() throws Exception {
		Song firstSong = new Song();
		Song secondSong = new Song();
		Actor firstActor = new Actor();
		Actor secondActor = new Actor();

		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		boolean operationSuccessfull = false;
		try {
			firstSong.setSinger( "Charlotte Church" );
			firstSong.setTitle( "Ave Maria" );
			em.persist( firstSong );

			secondSong.setSinger( "Charlotte Church" );
			secondSong.setTitle( "Flower Duet" );
			em.persist( secondSong );

			firstActor.setName( "Russell Crowe" );
			firstActor.setBestMovieTitle( "Gladiator" );
			em.persist( firstActor );

			secondActor.setName( "Johnny Depp" );
			secondActor.setBestMovieTitle( "Pirates of the Caribbean" );
			em.persist( secondActor );
			operationSuccessfull = true;
		}
		finally {
			commitOrRollback( operationSuccessfull );
		}
		em.clear();

		getTransactionManager().begin();
		operationSuccessfull = false;
		try {
			firstSong = em.find( Song.class, firstSong.getId() );
			assertThat( firstSong ).isNotNull();
			assertThat( firstSong.getId() ).isEqualTo( Song.INITIAL_VALUE );
			assertThat( firstSong.getTitle() ).isEqualTo( "Ave Maria" );
			em.remove( firstSong );

			secondSong = em.find( Song.class, secondSong.getId() );
			assertThat( secondSong ).isNotNull();
			assertThat( secondSong.getId() ).isEqualTo( Song.INITIAL_VALUE + 1 );
			assertThat( secondSong.getTitle() ).isEqualTo( "Flower Duet" );
			em.remove( secondSong );

			firstActor = em.find( Actor.class, firstActor.getId() );
			assertThat( firstActor ).isNotNull();
			assertThat( firstActor.getId() ).isEqualTo( Actor.INITIAL_VALUE );
			assertThat( firstActor.getName() ).isEqualTo( "Russell Crowe" );
			em.remove( firstActor );

			secondActor = em.find( Actor.class, secondActor.getId() );
			assertThat( secondActor ).isNotNull();
			assertThat( secondActor.getId() ).isEqualTo( Actor.INITIAL_VALUE + 1 );
			assertThat( secondActor.getName() ).isEqualTo( "Johnny Depp" );
			em.remove( secondActor );
			operationSuccessfull = true;
		}
		finally {
			commitOrRollback( operationSuccessfull );
		}

		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Song.class,
				Actor.class
		};
	}
}
