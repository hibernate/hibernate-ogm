/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestSessionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 * @author Davide D'Alto
 */
@SkipByGridDialect(
		value = { GridDialectType.CASSANDRA },
		comment = "WithEmbedded has lists - bag semantics unsupported (no primary key)"
)
public class QueriesWithEmbeddedCollectionTest extends OgmTestCase {

	@TestSessionFactory
	public static SessionFactory sessions;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Session session;

	private Transaction tx;

	@Before
	public void createSession() {
		closeSession();
		session = sessions.openSession();
		tx = session.beginTransaction();
	}

	@After
	public void closeSession() {
		if ( tx != null && tx.isActive() ) {
			tx.commit();
			tx = null;
		}
		if ( session != null ) {
			session.close();
			session = null;
		}
	}

	@Test
	public void testEqualOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.item = 'item[0]'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testInOperatorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "from WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.item IN ( 'item[0]' )" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}


	@Test
	public void testBetweenOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.item BETWEEN 'aaaaaa' AND 'zzzzzzzzz'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testLikeOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.item LIKE 'item[1%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualEmbeddedCollectionWithEmbeddableInCollectionWhereClause() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.item = 'item[1]'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testConjunctionOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.item = 'item[1]' AND c.anotherItem IN ('secondItem[0]')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testBetweenOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.embedded.embeddedInteger BETWEEN -100 AND 100" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testLikeOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.embedded.embeddedString LIKE 'string[1%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualOperatorWithEmbeddedInEmbeddedCollectionForString() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.embedded.embeddedString = 'string[1][0]'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualOperatorWithEmbeddedInEmbeddedCollectionForInteger() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.embedded.embeddedInteger = 10" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testConjunctionOperatorEqualOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM WithEmbedded e JOIN e.anEmbeddedCollection c WHERE c.item = 'item[1]' AND c.embedded.embeddedString IN ('string[1][0]')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryReturningEmbeddedObject() {
		@SuppressWarnings("unchecked")
		List<WithEmbedded> list = session.createQuery( "from WithEmbedded we" ).list();

		assertThat( list )
			.onProperty( "anEmbeddable" )
			.onProperty( "embeddedString" )
			.containsExactly( "embedded 1" );

		assertThat( list )
			.onProperty( "anEmbeddable" )
			.onProperty( "anotherEmbeddable" )
			.onProperty( "embeddedString" )
			.containsExactly( "string 1" );

		assertThat( list )
			.onProperty( "yetAnotherEmbeddable" )
			.onProperty( "embeddedString" )
			.containsExactly( "embedded 2" );

		assertThat( list.get( 0 ).getAnEmbeddedCollection() )
			.onProperty( "item" )
			.containsOnly( "item[0]", "item[1]" );

		assertThat( list.get( 0 ).getAnEmbeddedCollection() )
			.onProperty( "anotherItem" )
			.containsOnly( "secondItem[0]", null );

		assertThat( list.get( 0 ).getAnotherEmbeddedCollection() )
			.onProperty( "item" )
			.containsOnly( "another[0]", "another[1]" );

		assertThat( list.get( 0 ).getAnotherEmbeddedCollection() )
			.onProperty( "anotherItem" )
			.containsOnly( null, null );
	}

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		WithEmbedded with = new WithEmbedded( 1L, null );
		with.setAnEmbeddable( new AnEmbeddable( "embedded 1", new AnotherEmbeddable( "string 1", 1 ) ) );
		with.setYetAnotherEmbeddable( new AnEmbeddable( "embedded 2" ) );
		with.setAnEmbeddedCollection( Arrays.asList( new EmbeddedCollectionItem( "item[0]", "secondItem[0]", null ), new EmbeddedCollectionItem( "item[1]", null, new AnotherEmbeddable( "string[1][0]", 10 ) ) ) );
		with.setAnotherEmbeddedCollection( Arrays.asList( new EmbeddedCollectionItem( "another[0]", null, null ), new EmbeddedCollectionItem( "another[1]", null, null ) ) );

		persist( with );
	}


	@AfterClass
	public static void removeTestEntities() throws Exception {
		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		session.delete( session.load( WithEmbedded.class, 1L ) );

		transaction.commit();
		session.close();
	}

	private static void persist(Object... entities) {
		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		for ( Object entity : entities ) {
			session.persist( entity );
		}

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { WithEmbedded.class };
	}
}
