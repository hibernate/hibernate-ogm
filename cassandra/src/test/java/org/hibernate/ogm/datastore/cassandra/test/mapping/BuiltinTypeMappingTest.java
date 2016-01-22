/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.mapping;

import static org.hibernate.ogm.datastore.cassandra.utils.CassandraTestHelper.rowAssertion;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.type.Bookmark;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the mappings of built-in types into Cassandra.
 *
 * @author Nicola Ferraro
 */
public class BuiltinTypeMappingTest extends OgmTestCase {

	private String bookmarkId;

	@Before
	public void setUpTestData() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		Bookmark bookmark = new Bookmark();
		bookmark.setFavourite( Boolean.TRUE );
		bookmark.setPrivate( true );
		bookmark.setRead( true );
		bookmark.setShared( true );

		session.persist( bookmark );
		transaction.commit();
		session.close();

		this.bookmarkId = bookmark.getId();
	}

	@After
	public void removeTestData() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		session.delete( session.get( Bookmark.class, bookmarkId ) );

		transaction.commit();
		session.close();
	}

	@Test
	public void booleanMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		rowAssertion( session.getSessionFactory(), "Bookmark" )
				.keyColumn( "id", bookmarkId )
				.assertColumn( "favourite", true )
				.execute();

		transaction.commit();
		session.close();
	}

	@Test
	public void trueFalseTypeMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		rowAssertion( session.getSessionFactory(), "Bookmark" )
				.keyColumn( "id", bookmarkId )
				.assertColumn( "isPrivate", "T" ) // characters are strings in Cassandra
				.execute();

		transaction.commit();
		session.close();
	}

	@Test
	public void yesNoTypeMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		rowAssertion( session.getSessionFactory(), "Bookmark" )
				.keyColumn( "id", bookmarkId )
				.assertColumn( "isRead", "Y" ) // characters are strings in Cassandra
				.execute();

		transaction.commit();
		session.close();
	}

	@Test
	public void numericBooleanMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		rowAssertion( session.getSessionFactory(), "Bookmark" )
				.keyColumn( "id", bookmarkId )
				.assertColumn( "isShared", 1 )
				.execute();

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Bookmark.class };
	}
}
