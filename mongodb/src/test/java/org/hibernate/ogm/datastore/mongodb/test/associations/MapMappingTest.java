/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.collection.types.Address;
import org.hibernate.ogm.backendtck.associations.collection.types.User;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * @author Gunnar Morling
 */
public class MapMappingTest extends OgmTestCase {

	@Test
	public void testMapOfEntity() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		Address home = new Address();
		home.setCity( "Paris" );
		Address work = new Address();
		work.setCity( "San Francisco" );
		User user = new User();
		user.getAddresses().put( "home", home );
		user.getAddresses().put( "work", work );

		session.persist( home );
		session.persist( work );
		session.persist( user );

		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"User",
				// query
				"{ '_id' : '" + user.getId() + "' }",
				// expected
				"{ " +
					"'_id' : '" + user.getId() + "', " +
					"'addresses' : {" +
						"'home' : '" + home.getId() + "'," +
						"'work' : '" + work.getId() + "'" +
					"}" +
				"}"
		);

		// clean-up
		user = (User) session.get( User.class, user.getId() );
		session.delete( user );
		session.delete( session.load( Address.class, home.getId() ) );
		session.delete( session.load( Address.class, work.getId() ) );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Address.class };
	}
}
