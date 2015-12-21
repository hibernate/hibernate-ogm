/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.associations;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.backendtck.associations.collection.types.Address;
import org.hibernate.ogm.backendtck.associations.collection.types.Department;
import org.hibernate.ogm.backendtck.associations.collection.types.Enterprise;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber;
import org.hibernate.ogm.backendtck.associations.collection.types.PhoneNumber.PhoneNumberId;
import org.hibernate.ogm.backendtck.associations.collection.types.User;
import org.hibernate.ogm.datastore.document.options.MapStorageType;
import org.hibernate.ogm.datastore.redis.Redis;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.Test;

import static org.hibernate.ogm.datastore.redis.utils.RedisTestHelper.assertDbObject;

/**
 * @author Gunnar Morling
 * @author Mark Paluch
 */
@SkipByGridDialect(value = GridDialectType.REDIS_HASH, comment = "RedisHashDialect does not support embedded structures/associations")
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
				user.getId(),
				// expected
				"{ " +
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
	public void testMapOfEntityUsingListStrategy() throws Exception {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		PhoneNumber home = new PhoneNumber( new PhoneNumberId( "DE", 123 ), "Home Phone" );
		PhoneNumber work = new PhoneNumber( new PhoneNumberId( "EN", 456 ), "Work Phone" );
		User user = new User();
		user.getAlternativePhoneNumbers().put( "home", home );
		user.getAlternativePhoneNumbers().put( "work", work );

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
				user.getId(),
				// expected
				"{ " +
					"'alternativePhoneNumbers' : [" +
						"{ 'phoneType' : 'home', 'countryCode' : 'DE', 'number'  : 123 }," +
						"{ 'phoneType' : 'work', 'countryCode' : 'EN', 'number'  : 456 }" +
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
	public void testMapOfEntityUsingListStrategyConfiguredViaOptionApi() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, Redis.class )
			.entity( User.class )
				.property( "addresses", ElementType.METHOD )
					.mapStorage( MapStorageType.AS_LIST );

		OgmSessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( settings, getAnnotatedClasses() );
		OgmSession session = sessionFactory.openSession();
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

		assertDbObject(
				session.getSessionFactory(),
				// collection
				"User",
				// query
				user.getId(),
				// expected
				"{ " +
					"'addresses' : [" +
						"{ 'addressType' : 'home', 'addresses_id' : '" + home.getId() + "' }," +
						"{ 'addressType' : 'work', 'addresses_id' : '" + work.getId() + "' }" +
					"]" +
				"}"
		);

		// clean-up
		user = (User) session.get( User.class, user.getId() );
		session.delete( user );
		session.delete( session.load( Address.class, home.getId() ) );
		session.delete( session.load( Address.class, work.getId() ) );

		tx.commit();
		session.close();
		sessionFactory.close();

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
				user.getId(),
				// expected
				"{ " +
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
				user.getId(),
				// expected
				"{ " +
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
				"enterprise-1",
				// expected
				"{ " +
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

	@Test
	public void testMapWithSimpleValueType() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		Enterprise timberTradingInc = new Enterprise( "enterprise-1", null );
		timberTradingInc.getRevenueByDepartment().put( "sale", 1000 );
		timberTradingInc.getRevenueByDepartment().put( "sawing", 2000 );
		timberTradingInc.getRevenueByDepartment().put( "planting", 3000 );

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
				"enterprise-1",
				// expected
				"{ " +
					"'revenueByDepartment' : {" +
						"'sawing' : 2000," +
						"'sale' : 1000," +
						"'planting' : 3000," +
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
