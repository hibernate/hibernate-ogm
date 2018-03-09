/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.projection;

import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * @author Fabio Massimo Ercoli
 */
public abstract class SingleMovieBaseTest extends OgmTestCase {

	protected static final String PROJECTION_ADD_ENTITY_MESSAGE = "OGM000091: Projection and addEntity are not allowed in the same query: table <Movie>";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	protected final Movie originalMovie = new Movie( 1, "2001: A Space Odyssey", "Stanley Kubrick", 1968 );

	@Before
	public void init() {
		inTransaction( session -> session.persist( originalMovie ) );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			Object entity = session.get( Movie.class, originalMovie.getId() );
			if ( entity != null ) {
				session.delete( entity );
			}
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Movie.class };
	}
}
