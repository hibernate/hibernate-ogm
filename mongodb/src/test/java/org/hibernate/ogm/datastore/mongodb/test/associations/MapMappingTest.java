/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.associations;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.collection.types.Address;
import org.hibernate.ogm.backendtck.associations.collection.types.Department;
import org.hibernate.ogm.backendtck.associations.collection.types.Enterprise;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber.PhoneNumberId;
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

	@Test
	public void testMapOfEntityWithCompositeId() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		PhoneNumber home = new PhoneNumber( new PhoneNumberId( "DE", 123 ), "Home Phone" );
		PhoneNumber work = new PhoneNumber( new PhoneNumberId( "EN", 456 ), "Work Phone" );
		User user = new User();
		user.getPhoneNumbers().put( "home", home );
		user.getPhoneNumbers().put( "work", work );

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
					"'phoneNumbers' : {" +
						"'home' : { 'countryCode' : 'DE', 'number'  : 123 }," +
						"'work' : { 'countryCode' : 'EN', 'number'  : 456 }" +
					"}" +
				"}"
		);

		// clean-up
		user = (User) session.get( User.class, user.getId() );
		session.delete( user );
		session.delete( session.load( PhoneNumber.class, home.getId() ) );
		session.delete( session.load( PhoneNumber.class, work.getId() ) );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Test
	public void testMapWithNonStringKey() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		PhoneNumber home = new PhoneNumber( new PhoneNumberId( "DE", 123 ), "Home Phone" );
		PhoneNumber work = new PhoneNumber( new PhoneNumberId( "EN", 456 ), "Work Phone" );
		User user = new User();
		user.getPhoneNumbersByPriority().put( 1, home );
		user.getPhoneNumbersByPriority().put( 2, work );

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
					"'phoneNumbersByPriority' : [" +
						"{ 'priority' : 1, 'countryCode' : 'DE', 'number'  : 123 }," +
						"{ 'priority' : 2, 'countryCode' : 'EN', 'number'  : 456 }" +
					"]" +
				"}"
		);

		// clean-up
		user = (User) session.get( User.class, user.getId() );
		session.delete( user );
		session.delete( session.load( PhoneNumber.class, home.getId() ) );
		session.delete( session.load( PhoneNumber.class, work.getId() ) );

		tx.commit();
		session.close();

		checkCleanCache();
	}

	@Test
	public void testMapOfComponent() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		Map<String, Department> departments = new HashMap<>();
		departments.put( "sawing", new Department( "Sawing", 7 ) );
		departments.put( "sale", new Department( "Sale", 2 ) );
		Enterprise timberTradingInc = new Enterprise( "enterprise-1", departments );

		session.persist( timberTradingInc );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();

		// assert
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Enterprise",
				// query
				"{ '_id' : 'enterprise-1' }",
				// expected
				"{ " +
					"'_id' : 'enterprise-1', " +
					"'departments' : {" +
						"'sawing' : { 'name' : 'Sawing', 'headCount'  : 7 }," +
						"'sale' : { 'name' : 'Sale', 'headCount'  : 2 }," +
					"}" +
				"}"
		);

		// clean up
		session.delete( timberTradingInc );

		tx.commit();
		session.close();
		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Address.class, PhoneNumber.class, Enterprise.class };
	}
}
