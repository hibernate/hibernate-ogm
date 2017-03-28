/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDocument;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.type.Bookmark;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the mappings of built-in types into MongoDB.
 *
 * @author Gunnar Morling
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

		assertDocument(
				session.getSessionFactory(),
				// collection
				"Bookmark",
				// query
				"{ '_id' : '" + bookmarkId + "' }",
				// fields
				"{ 'favourite' : 1 }",
				// expected
				"{ " +
					"'_id' : '" + bookmarkId + "', " +
					"'favourite' : true" +
				"}"
		);

		transaction.commit();
		session.close();
	}

	@Test
	public void trueFalseTypeMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		assertDocument(
				session.getSessionFactory(),
				// collection
				"Bookmark",
				// query
				"{ '_id' : '" + bookmarkId + "' }",
				// fields
				"{ 'isPrivate' : 1 }",
				// expected
				"{ " +
					"'_id' : '" + bookmarkId + "', " +
					"'isPrivate' : 'T'" +
				"}"
		);

		transaction.commit();
		session.close();
	}

	@Test
	public void yesNoTypeMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		assertDocument(
				session.getSessionFactory(),
				// collection
				"Bookmark",
				// query
				"{ '_id' : '" + bookmarkId + "' }",
				// fields
				"{ 'isRead' : 1 }",
				// expected
				"{ " +
					"'_id' : '" + bookmarkId + "', " +
					"'isRead' : 'Y'" +
				"}"
		);

		transaction.commit();
		session.close();
	}

	@Test
	public void numericBooleanMapping() {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		assertDocument(
				session.getSessionFactory(),
				// collection
				"Bookmark",
				// query
				"{ '_id' : '" + bookmarkId + "' }",
				// fields
				"{ 'isShared' : 1 }",
				// expected
				"{ " +
					"'_id' : '" + bookmarkId + "', " +
					"'isShared' : 1" +
				"}"
		);

		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Bookmark.class };
	}
}
