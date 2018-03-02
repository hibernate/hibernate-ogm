/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.pagination;

import static java.util.Arrays.copyOfRange;
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test pagination.
 *
 * @author Davide D'Alto
 * @author Fabio Massimo Ercoli
 */
public abstract class PaginationUseCases extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final Poem portia = new Poem( 1L, "Portia", "Oscar Wilde", 1881 );
	private final Poem athanasia = new Poem( 2L, "Athanasia", "Oscar Wilde", 1879 );
	private final Poem imperatrix = new Poem( 3L, "Ave Imperatrix", "Oscar Wilde", 1882 );
	private final Poem intellectualis = new Poem( 4L, "Amor Intellectualis", "Oscar Wilde", 1881 );
	private final Poem apologia = new Poem( 5L, "Apologias", "Oscar Wilde", 1881 );
	private final Poem easter = new Poem( 6L, "Easter Day", "Oscar Wilde", 1881 );
	private final Poem rome = new Poem( 7L, "Rome Unvisited", "Oscar Wilde", 1881 );
	private final Poem miniato = new Poem( 8L, "San Miniato", "Oscar Wilde", 1881 );
	private final Poem liberty = new Poem( 9L, "Sonnet to Liberty", "Oscar Wilde", 1881 );
	private final Poem vita = new Poem( 10L, "Vita Nuova", "Oscar Wilde", 1881 );

	private final Poem[] poems = { intellectualis, apologia, athanasia, imperatrix, easter, portia, rome, miniato, liberty, vita };

	protected abstract List<Poem> findPoemsSortedAlphabetically(Session session, int startPosition, int maxResult);

	@Before
	public void init() {
		inTransaction( session -> {
			for ( Poem poem : poems ) {
				session.persist( poem );
			}
		} );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			for ( Poem poem : poems ) {
				delete( session, poem );
			}
		} );
	}

	private void delete(Session session, Poem poem) {
		Object entity = session.get( Poem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	public void testFirstPageWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 0, 7 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 7 ) );
		} );
	}

	@Test
	public void testLastPageWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 5, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 5, 10 ) );
		} );
	}

	@Test
	public void testFirstTwoPagesWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 0, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 5 ) );

			result = findPoemsSortedAlphabetically( session, 5, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 5, 10 ) );
		} );
	}

	@Test
	public void testMaxResultsBiggerThanNumberOfResultsWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 0, 15 );
			assertThat( result ).containsExactly( poems );
		} );
	}

	@Test
	public void testPageInTheMiddleWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );
		} );
	}

	@Test
	public void testScrollingForwardWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );

			result = findPoemsSortedAlphabetically( session, 6, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 6, 9 ) );
		} );
	}

	@Test
	public void testScrollingBackwardWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );

			result = findPoemsSortedAlphabetically( session, 0, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 3 ) );
		} );
	}

	@Test
	public void testStartPositionIsOutOfRangeWithFind() {
		inTransaction( session -> {
			List<?> result = findPoemsSortedAlphabetically( session, 10, 5 );
			assertThat( result ).isEmpty();
		} );
	}

	@Test
	public void testStartPositionIsNegativeWithFind() {
		thrown.expect( IllegalArgumentException.class );

		inTransaction( session -> {
			findPoemsSortedAlphabetically( session, -4, 5 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Poem.class };
	}
}
