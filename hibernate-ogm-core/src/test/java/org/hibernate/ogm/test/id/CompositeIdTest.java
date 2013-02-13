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

import java.util.ArrayList;
import java.util.List;
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
		final String author = "Guillaume";

		final String titleAboutJUG = "What is a JUG ?";
		final String contentAboutJUG = "JUG means Java User Group";
		final List<Label> newsAboutJugLabels = labels( "jug", "question" );
		final NewsID newsAboutJugID = new NewsID( titleAboutJUG, author );
		final News newsAboutJUG = new News( newsAboutJugID, contentAboutJUG, newsAboutJugLabels );

		final String titleCountJUG = "There are more than 20 JUGs in France";
		final String contentCountJUG = "Great! Congratulations folks";
		final List<Label> newsCountJugLabels = labels( "statJUG" );
		final NewsID newsCountJugID = new NewsID( titleCountJUG, author );
		final News newsCountJUG = new News( newsCountJugID, contentCountJUG, newsCountJugLabels );

		final String titleOGM = "How to use Hibernate OGM ?";
		final String contentOGM = "Simple, just like ORM but with a NoSQL database";
		final List<Label> newsOgmLabels = labels( "OGM", "hibernate" );
		final NewsID newsOgmID = new NewsID( titleOGM, author );
		final News newsOGM = new News( newsOgmID, contentOGM, newsOgmLabels );

		boolean operationSuccessful = false;
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		try {
			em.persist( newsOGM );
			em.persist( newsAboutJUG );
			em.persist( newsCountJUG );
			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.clear();
		getTransactionManager().begin();
		operationSuccessful = false;
		try {
			{
				News news = em.find( News.class, newsOgmID );
				assertThat( news ).isNotNull();
				assertThat( news.getContent() ).isEqualTo( contentOGM );
				assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
				assertThat( news.getNewsId().getTitle() ).isEqualTo( titleOGM );
				assertThat( news.getLabels().size() ).isEqualTo( newsOgmLabels.size() );
				em.remove( news );

				assertThat( em.find( News.class, newsOgmID ) ).isNull();
				em.clear();
			}
			{
				News news = em.find( News.class, newsAboutJugID );
				assertThat( news ).isNotNull();
				assertThat( news.getContent() ).isEqualTo( contentAboutJUG );
				assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
				assertThat( news.getNewsId().getTitle() ).isEqualTo( titleAboutJUG );
				assertThat( news.getLabels().size() ).isEqualTo( newsAboutJugLabels.size() );
				em.remove( news );

				assertThat( em.find( News.class, newsAboutJugID ) ).isNull();
				em.clear();
			}
			{
				News news = em.find( News.class, newsCountJugID );
				assertThat( news ).isNotNull();
				assertThat( news.getContent() ).isEqualTo( contentCountJUG );
				assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
				assertThat( news.getNewsId().getTitle() ).isEqualTo( titleCountJUG );
				assertThat( news.getLabels().size() ).isEqualTo( newsCountJugLabels.size() );
				em.remove( news );
				assertThat( em.find( News.class, newsCountJugID ) ).isNull();
			}
		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.close();
	}

	private List<Label> labels(String... keywords) {
		List<Label> newsOgmLabels = new ArrayList<Label>();
		for ( String keyword : keywords ) {
			newsOgmLabels.add( new Label( keyword ) );
		}
		return newsOgmLabels;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { News.class, NewsID.class, Label.class };
	}
}
