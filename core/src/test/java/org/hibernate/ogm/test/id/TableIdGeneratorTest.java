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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TableIdGeneratorTest extends JpaTestCase {

	@Test
	public void testTableIdGeneratorInJTA() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		Music music = new Music();
		music.setName( "Variations Sur Marilou" );
		music.setComposer( "Gainsbourg" );
		em.persist( music );
		Video video = new Video();
		video.setDirector( "Wes Craven" );
		video.setName( "Scream" );
		em.persist( video );
		getTransactionManager().commit();

		em.clear();

		getTransactionManager().begin();
		music = em.find( Music.class, music.getId() );
		assertThat( music ).isNotNull();
		assertThat( music.getName() ).isEqualTo( "Variations Sur Marilou" );
		em.remove( music );
		video = em.find( Video.class, video.getId() );
		assertThat( video ).isNotNull();
		assertThat( video.getName() ).isEqualTo( "Scream" );
		em.remove( video );
		getTransactionManager().commit();

		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Music.class,
				Video.class
		};
	}
}
