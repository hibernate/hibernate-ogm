/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class CompositeIdTest extends OgmJpaTestCase {
	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void testCompositeEmbeddedId() throws Exception {
		final String titleOGM = "How to use Hibernate OGM ?";
		final String titleAboutJUG = "What is a JUG ?";
		final String titleCountJUG = "There are more than 20 JUGs in France";

		final String author = "Guillaume";

		final String contentOGM = "Simple, just like ORM but with a NoSQL database";
		final String contentAboutJUG = "JUG means Java User Group";
		final String contentCountJUG = "Great! Congratulations folks";

		Label questionLabel = new Label( "question" );
		Label jugLabel = new Label( "jug" );
		Label hibernateLabel = new Label( "hibernate" );
		Label ogmLabel = new Label( "OGM" );
		Label statJugLabel = new Label( "statJUG" );

		NewsID newsOgmID = new NewsID( titleOGM, author );
		NewsID newsAboutJugID = new NewsID( titleAboutJUG, author );
		NewsID newsCountJugID = new NewsID( titleCountJUG, author );

		final List<Label> newsOgmLabels = new ArrayList<Label>();
		newsOgmLabels.add( ogmLabel );
		newsOgmLabels.add( hibernateLabel );

		final List<Label> newsAboutJugLabels = new ArrayList<Label>();
		newsAboutJugLabels.add( jugLabel );
		newsAboutJugLabels.add( questionLabel );

		final List<Label> newsCountJugLabels = new ArrayList<Label>();
		newsCountJugLabels.add( statJugLabel );

		News newsAboutJUG = new News( newsAboutJugID, contentAboutJUG, newsAboutJugLabels );
		News newsOGM = new News( newsOgmID, contentOGM, newsOgmLabels );
		News newsCountJUG = new News( newsCountJugID, contentCountJUG, newsCountJugLabels );

		em.getTransaction().begin();
		em.persist( newsOGM );
		em.persist( newsAboutJUG );
		em.persist( newsCountJUG );
		em.getTransaction().commit();

		em.clear();
		em.getTransaction().begin();
		News news = em.find( News.class, newsOgmID );
		assertThat( news ).isNotNull();
		assertThat( news.getContent() ).isEqualTo( contentOGM );
		assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
		assertThat( news.getNewsId().getTitle() ).isEqualTo( titleOGM );
		assertThat( news.getLabels().size() ).isEqualTo( newsOgmLabels.size() );
		em.remove( news );
		assertThat( em.find( News.class, newsOgmID ) ).isNull();

		em.clear();
		news = em.find( News.class, newsAboutJugID );
		assertThat( news ).isNotNull();
		assertThat( news.getContent() ).isEqualTo( contentAboutJUG );
		assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
		assertThat( news.getNewsId().getTitle() ).isEqualTo( titleAboutJUG );
		assertThat( news.getLabels().size() ).isEqualTo( newsAboutJugLabels.size() );
		em.remove( news );
		assertThat( em.find( News.class, newsAboutJugID ) ).isNull();

		em.clear();
		news = em.find( News.class, newsCountJugID );
		assertThat( news ).isNotNull();
		assertThat( news.getContent() ).isEqualTo( contentCountJUG );
		assertThat( news.getNewsId().getAuthor() ).isEqualTo( author );
		assertThat( news.getNewsId().getTitle() ).isEqualTo( titleCountJUG );
		assertThat( news.getLabels().size() ).isEqualTo( newsCountJugLabels.size() );
		em.remove( news );
		assertThat( em.find( News.class, newsCountJugID ) ).isNull();
		em.getTransaction().commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				News.class,
				NewsID.class,
				Label.class
		};
	}
}
