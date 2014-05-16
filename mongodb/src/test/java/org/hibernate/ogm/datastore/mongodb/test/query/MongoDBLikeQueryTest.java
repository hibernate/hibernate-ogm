/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query;


import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestSessionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for {@code LIKE} queries with MongoDB.
 *
 * @author Gunnar Morling
 */
public class MongoDBLikeQueryTest extends OgmTestCase {

	@TestSessionFactory
	private static SessionFactory sessions;

	private Session session;
	private Transaction transaction;

	@BeforeClass
	public static void addTestEntities() {
		Session session = sessions.openSession();
		Transaction transaction = session.getTransaction();
		transaction.begin();

		Hypothesis hypothesis = new Hypothesis();
		hypothesis.setId( "1" );
		hypothesis.setPosition( 1 );
		hypothesis.setDescription( "Alea iacta est." );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "2" );
		hypothesis.setPosition( 2 );
		hypothesis.setDescription( "Ne vadis..." );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "3" );
		hypothesis.setPosition( 3 );
		hypothesis.setDescription( "Omne initium difficile est." );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "4" );
		hypothesis.setPosition( 4 );
		hypothesis.setDescription( "Nomen est omen." );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "5" );
		hypothesis.setPosition( 5 );
		hypothesis.setDescription( "Quo vadis?" );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "6" );
		hypothesis.setPosition( 6 );
		hypothesis.setDescription( "Ne vadis." );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "7" );
		hypothesis.setPosition( 7 );
		hypothesis.setDescription( "100% scientia" );
		session.persist( hypothesis );

		hypothesis = new Hypothesis();
		hypothesis.setId( "8" );
		hypothesis.setPosition( 8 );
		hypothesis.setDescription( "100\nscientiae" );
		session.persist( hypothesis );

		transaction.commit();
		session.clear();
		session.close();
	}

	@AfterClass
	public static void deleteTestEntities() throws Exception {
		Session session = sessions.openSession();
		Transaction transaction = session.getTransaction();
		transaction.begin();

		session.delete( new Hypothesis( "1" ) );
		session.delete( new Hypothesis( "2" ) );
		session.delete( new Hypothesis( "3" ) );
		session.delete( new Hypothesis( "4" ) );
		session.delete( new Hypothesis( "5" ) );
		session.delete( new Hypothesis( "6" ) );
		session.delete( new Hypothesis( "7" ) );
		session.delete( new Hypothesis( "8" ) );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Before
	public void startTransaction() {
		session = sessions.openSession();
		transaction = session.getTransaction();
		transaction.begin();
	}

	@After
	public void commitTransaction() {
		session.close();
		transaction.commit();
	}

	@Test
	public void shouldTreatDotNotAsRegexWildCard() throws Exception {
		List<?> results = session.createQuery( "from Hypothesis h where h.description like '%est.'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "1", "3" );
	}

	@Test
	public void shouldApplyWildCardCharacters() throws Exception {
		List<?> results = session.createQuery( "from Hypothesis h where h.description like '%vadis?'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "5" );

		results = session.createQuery( "from Hypothesis h where h.description like '100%'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "7", "8" );
	}

	@Test
	public void shouldApplyEscaping() throws Exception {
		List<?> results = session.createQuery( "from Hypothesis h where h.description like '100$%%' escape '$'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "7" );

		results = session.createQuery( "from Hypothesis h where h.description like '100%% scientia' escape '%'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "7" );
	}
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class };
	}
}
