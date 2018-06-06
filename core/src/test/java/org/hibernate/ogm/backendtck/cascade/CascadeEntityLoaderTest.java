/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.cascade;

import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Testing loading behaviour,
 * on merging detached entities
 * or refreshing attached entities,
 * with or without cascade.
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1468")
@SkipByGridDialect({ INFINISPAN })
public class CascadeEntityLoaderTest extends OgmTestCase {

	public static final String ARTIST_1 = "Arctic Monkeys";
	public static final String ARTIST_2 = "Liam Gallagher";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void detachUpdateMerge() {
		inTransaction( session -> {
			Party party = new Party( 1, ARTIST_1, new Date(), "Budapest" );
			session.persist( party );
		} );

		inTransaction( session -> {
			Party party = session.load( Party.class, 1 );
			assertEquals( ARTIST_1, party.getName() );

			// detach - update - merge
			session.detach( party );
			party.setName( ARTIST_2 );
			session.merge( party );
		} );

		inTransaction( session -> {
			Party party = session.load( Party.class, 1 );
			assertEquals( ARTIST_2, party.getName() );
		} );

		deleteAll( Party.class, 1 );
	}

	@Test
	public void updateAttachedRefresh() {
		inTransaction( session -> {
			Party party = new Party( 2, ARTIST_1, new Date(), "Budapest" );
			session.persist( party );
		} );

		inTransaction( session -> {
			Party party = session.load( Party.class, 2 );
			assertEquals( ARTIST_1, party.getName() );

			// update attached - refresh
			party.setName( ARTIST_2 );
			session.refresh( party );
		} );

		inTransaction( session -> {
			Party party = session.load( Party.class, 2 );
			assertEquals( ARTIST_1, party.getName() );
		} );

		deleteAll( Party.class, 2 );
	}

	@Test
	public void detachUpdateMergeOnCascade() {
		String festivalName = "Sziget 2018";
		inTransaction( session -> {
			Party party = new Party( 4, ARTIST_1, new Date(), "Budapest" );
			Festival festival = new Festival( festivalName );
			festival.add( party );
			session.persist( festival );
		} );

		inTransaction( session -> {
			Festival festival = session.load( Festival.class, festivalName );
			Party party = festival.getParties().get( 0 );
			assertEquals( ARTIST_1, party.getName() );

			// detach parent - update - merge parent
			session.detach( festival );
			party.setName( ARTIST_2 );
			session.merge( festival );
		} );

		inTransaction( session -> {
			Festival festival = session.load( Festival.class, festivalName );
			Party party = festival.getParties().get( 0 );
			assertEquals( ARTIST_2, party.getName() );
		} );

		deleteAll( Festival.class, festivalName );
	}

	@Test
	public void updateAttachedRefreshOnCascade() {
		String festivalName = "Sziget 2019";
		inTransaction( session -> {
			Party party = new Party( 5, ARTIST_1, new Date(), "Budapest" );
			Festival festival = new Festival( festivalName );
			festival.add( party );
			session.persist( festival );
		} );

		inTransaction( session -> {
			Festival festival = session.load( Festival.class, festivalName );
			Party party = festival.getParties().get( 0 );

			// update attached - refresh parent
			assertEquals( ARTIST_1, party.getName() );
			party.setName( ARTIST_2 );
			session.refresh( festival );
		} );

		inTransaction( session -> {
			Festival festival = session.load( Festival.class, festivalName );
			Party party = festival.getParties().get( 0 );
			assertEquals( ARTIST_1, party.getName() );
		} );

		deleteAll( Festival.class, festivalName );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Festival.class, Party.class };
	}
}
