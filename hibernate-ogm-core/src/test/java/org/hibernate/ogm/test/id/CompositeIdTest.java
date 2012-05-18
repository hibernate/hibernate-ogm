/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class CompositeIdTest extends JpaTestCase {

	@Test
	public void testCompositeEmbeddedId() throws Exception {
		final String titleOGM = "How to use Hibernate OGM ?";
		final String titleJUG = "What is a JUG ?";
		final String author = "Guillaume";
		final String contentOGM = "Simple, just like ORM but with a NoSQL database";
		final String contentJUG = "JUG means Java User Group";

		NewsID newsOgmID = new NewsID( titleOGM, author );
		NewsID newsJugID = new NewsID( titleJUG, author );
		News newsOGM = new News( newsOgmID, contentOGM );
		News newsJUG = new News( newsJugID, contentJUG );

		boolean operationSuccessful = false;
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		//em.getTransaction().begin();
		try {
			em.persist( newsOGM );
			em.persist( newsJUG );
			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.clear();
		getTransactionManager().begin();
		operationSuccessful = false;
		try {
			News news = em.find( News.class, newsOgmID );
			assertThat( news ).isNotNull();
			assertThat( news.getContent() ).isEqualTo( contentOGM );
			assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
			assertThat( news.getNewsId().getTitle() ).isEqualTo( titleOGM );

			em.clear();
			news = em.find( News.class, newsJugID );
			assertThat( news ).isNotNull();
			assertThat( news.getContent() ).isEqualTo( contentJUG );
			assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
			assertThat( news.getNewsId().getTitle() ).isEqualTo( titleJUG );

		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				News.class,
				NewsID.class
		};
	}
}
